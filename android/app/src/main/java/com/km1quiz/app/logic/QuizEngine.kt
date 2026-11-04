package com.km1quiz.app.logic

import com.km1quiz.app.data.Question

/**
 * 核心业务逻辑（纯 Kotlin，无 Android 依赖，可 JVM 单测）。
 * 语义逐条对齐 VSCode 扩展版 media/main.js：
 * - 顺序练习进度、随机练习、错题重练（答对自动移出并 splice 队列）、收藏夹
 * - 模拟考试：判断 40 + 单选 60 共 100 题、45 分钟、90 分及格、错题并入错题本、历史保留最近 20 次
 */
class QuizEngine(
    val questions: List<Question>,
    persisted: PersistentState = PersistentState()
) {
    companion object {
        const val EXAM_TOTAL = 100
        const val EXAM_JUDGE = 40
        const val EXAM_MINUTES = 45
        const val PASS_SCORE = 90
        const val HISTORY_LIMIT = 20

        /** 合并默认值并过滤题库更换后失效的旧记录（对齐 main.js normalizeState） */
        fun normalize(s: PersistentState?, validIds: Set<String>): PersistentState {
            if (s == null) { return PersistentState() }
            val answers = s.answers.filterKeys { it in validIds }
            return PersistentState(
                answers = answers,
                wrong = s.wrong.filter { it in validIds },
                starred = s.starred.filter { it in validIds },
                sequentialIndex = if (s.sequentialIndex >= 0) s.sequentialIndex else 0,
                examHistory = s.examHistory.takeLast(HISTORY_LIMIT)
            )
        }
    }

    private val byId: Map<String, Question> = questions.associateBy { it.id }

    /** 持久化状态（进度/错题/收藏/成绩） */
    var state: PersistentState = normalize(persisted, byId.keys)
        private set

    // ---------- 会话 ----------
    var practice: PracticeSession? = null
        private set
    var exam: ExamSession? = null
        private set

    /** 非空 = 队列为空时的空态提示（wrong/starred/exam） */
    var emptyReason: EmptyReason? = null
        private set

    /** 数据/会话发生任何变化后回调（ViewModel 用来刷新 Compose 状态） */
    var onChange: (() -> Unit)? = null

    private fun changed() { onChange?.invoke() }

    // ---------- 首页入口 ----------
    fun startPractice(mode: PracticeMode) {
        val queue: List<String>
        var index = 0
        when (mode) {
            PracticeMode.SEQUENTIAL -> {
                queue = questions.map { it.id }
                index = minOf(state.sequentialIndex, (queue.size - 1).coerceAtLeast(0))
            }
            PracticeMode.RANDOM -> queue = questions.map { it.id }.shuffled()
            PracticeMode.WRONG -> queue = state.wrong.toList()
            PracticeMode.STARRED -> queue = state.starred.toList()
        }
        if (queue.isEmpty()) {
            practice = null
            exam = null
            emptyReason = when (mode) {
                PracticeMode.WRONG -> EmptyReason.WRONG_EMPTY
                PracticeMode.STARRED -> EmptyReason.STARRED_EMPTY
                else -> EmptyReason.EXAM_SHORT
            }
            changed()
            return
        }
        practice = PracticeSession(mode, queue.toMutableList(), index)
        exam = null
        emptyReason = null
        changed()
    }

    fun startExam(nowMs: Long = System.currentTimeMillis()) {
        val judges = questions.filter { it.type == com.km1quiz.app.data.QType.JUDGE }.shuffled().take(EXAM_JUDGE)
        val singles = questions.filter { it.type == com.km1quiz.app.data.QType.SINGLE }
            .shuffled().take(EXAM_TOTAL - EXAM_JUDGE)
        val paper = (judges + singles).shuffled()
        if (paper.size < EXAM_TOTAL) {
            practice = null
            exam = null
            emptyReason = EmptyReason.EXAM_SHORT
            changed()
            return
        }
        exam = ExamSession(
            paper = paper,
            picked = IntArray(paper.size) { -1 },
            index = 0,
            deadlineMs = nowMs + EXAM_MINUTES * 60_000L
        )
        practice = null
        emptyReason = null
        changed()
    }

    fun exitToHome() {
        practice = null
        exam = null
        emptyReason = null
        changed()
    }

    // ---------- 练习 ----------
    fun practicePick(pickedIdx: Int) {
        val s = practice ?: return
        val id = s.queue.getOrNull(s.index) ?: return
        val q = byId[id] ?: return
        val correct = pickedIdx == q.answer
        state = state.copy(
            answers = state.answers + (id to AnswerRecord(pickedIdx, correct)),
            wrong = if (!correct && id !in state.wrong) {
                state.wrong + id
            } else if (correct) {
                state.wrong - id
            } else {
                state.wrong
            }
        )
        if (correct && s.mode == PracticeMode.WRONG) {
            val qi = s.queue.indexOf(id)
            if (qi >= 0) { s.queue.removeAt(qi) }
            // splice 后 index 天然指向下一题，无需自增（对齐 main.js）
        }
        changed()
    }

    fun practiceStep(delta: Int) {
        val s = practice ?: return
        val next = (s.index + delta).coerceAtLeast(0)
        s.index = next
        if (s.mode == PracticeMode.SEQUENTIAL) {
            state = state.copy(sequentialIndex = next)
        }
        if (next >= s.queue.size) {
            exitToHome() // 最后一题点"完成"
            return
        }
        changed()
    }

    fun toggleStar() {
        val s = practice ?: return
        val id = s.queue.getOrNull(s.index) ?: return
        state = if (id in state.starred) {
            state.copy(starred = state.starred - id)
        } else {
            state.copy(starred = state.starred + id)
        }
        changed()
    }

    fun isStarred(id: String): Boolean = id in state.starred

    // ---------- 考试 ----------
    fun examPick(pickedIdx: Int) {
        val e = exam ?: return
        e.picked[e.index] = pickedIdx
        e.confirming = false
        changed()
    }

    fun examGoto(i: Int) {
        val e = exam ?: return
        if (i in e.paper.indices) {
            e.index = i
            e.confirming = false
            changed()
        }
    }

    /** 关闭"确认交卷"提示，继续答题 */
    fun cancelConfirm() {
        val e = exam ?: return
        e.confirming = false
        changed()
    }

    /** 交卷评分；force=true 表示倒计时到点自动交卷。返回是否真正交卷 */
    fun submitExam(force: Boolean, nowMs: Long = System.currentTimeMillis()): Boolean {
        val e = exam ?: return false
        val unanswered = e.picked.count { it < 0 }
        if (unanswered > 0 && !force && !e.confirming) {
            e.confirming = true
            changed()
            return false
        }
        val usedSec = ((nowMs - (e.deadlineMs - EXAM_MINUTES * 60_000L)) / 1000L)
            .coerceIn(0, EXAM_MINUTES * 60L).toInt()
        var correctCount = 0
        val wrongList = mutableListOf<WrongItem>()
        var wrongIds = state.wrong.toMutableList()
        e.paper.forEachIndexed { i, q ->
            val picked = e.picked[i]
            if (picked == q.answer) {
                correctCount++
            } else {
                wrongList.add(WrongItem(q, picked))
                if (q.id !in wrongIds) { wrongIds.add(q.id) }
            }
        }
        val record = ExamRecord(
            dateIso = isoNow(nowMs),
            score = correctCount,
            total = e.paper.size,
            durationSec = usedSec
        )
        state = state.copy(
            wrong = wrongIds,
            examHistory = (state.examHistory + record).takeLast(HISTORY_LIMIT)
        )
        e.result = ExamResult(correctCount, e.paper.size, usedSec, wrongList)
        changed()
        return true
    }

    private fun isoNow(ms: Long): String =
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date(ms))
}

// ---------- 模型 ----------
enum class PracticeMode { SEQUENTIAL, RANDOM, WRONG, STARRED }

enum class EmptyReason { WRONG_EMPTY, STARRED_EMPTY, EXAM_SHORT }

data class AnswerRecord(val picked: Int, val correct: Boolean)

data class ExamRecord(val dateIso: String, val score: Int, val total: Int, val durationSec: Int)

data class PersistentState(
    val answers: Map<String, AnswerRecord> = emptyMap(),
    val wrong: List<String> = emptyList(),
    val starred: List<String> = emptyList(),
    val sequentialIndex: Int = 0,
    val examHistory: List<ExamRecord> = emptyList()
) {
    val answeredCount: Int get() = answers.size
    val correctCount: Int get() = answers.values.count { it.correct }
}

class PracticeSession(
    val mode: PracticeMode,
    val queue: MutableList<String>,
    var index: Int
) {
    val size: Int get() = queue.size
    fun current(bank: Map<String, Question>): Question? = bank[queue.getOrNull(index)]
}

class ExamSession(
    val paper: List<Question>,
    val picked: IntArray,
    var index: Int,
    val deadlineMs: Long
) {
    var confirming: Boolean = false
    var result: ExamResult? = null
    fun answeredCount(): Int = picked.count { it >= 0 }
    fun remainSeconds(nowMs: Long = System.currentTimeMillis()): Int =
        kotlin.math.ceil((deadlineMs - nowMs) / 1000.0).toInt()
}

data class WrongItem(val q: Question, val picked: Int)

data class ExamResult(
    val score: Int,
    val total: Int,
    val durationSec: Int,
    val wrongList: List<WrongItem>
)
