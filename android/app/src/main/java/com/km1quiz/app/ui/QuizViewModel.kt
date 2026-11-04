package com.km1quiz.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.km1quiz.app.data.Bank
import com.km1quiz.app.data.Prefs
import com.km1quiz.app.data.Question
import com.km1quiz.app.logic.EmptyReason
import com.km1quiz.app.logic.ExamRecord
import com.km1quiz.app.logic.PracticeMode
import com.km1quiz.app.logic.QuizEngine
import com.km1quiz.app.logic.WrongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 屏幕路由（顺序即 AppRoot 转场的方向基准） */
enum class Screen { HOME, PRACTICE, EXAM, RESULT, EMPTY }

data class HomeUi(
    val total: Int,
    val answered: Int,
    val accuracy: Int,
    val wrongCount: Int,
    val starredCount: Int,
    val lastExam: ExamRecord?
)

data class PracticeUi(
    val mode: PracticeMode,
    val index: Int,
    val size: Int,
    val done: Boolean,
    val q: Question?,
    val picked: Int?,
    val correct: Boolean?,
    val starred: Boolean
)

data class ExamUi(
    val index: Int,
    val size: Int,
    val q: Question?,
    val picked: Int?,
    val answered: Int,
    val confirming: Boolean,
    val remainSec: Int,
    val sheetPicked: List<Int>
)

data class ResultUi(
    val score: Int,
    val total: Int,
    val durationSec: Int,
    val wrongList: List<WrongItem>
)

/**
 * 视图模型：持有 QuizEngine，把可变引擎状态快照成不可变 Ui 数据供 Compose 订阅；
 * 持久化在每次变更后写入 SharedPreferences。
 */
class QuizViewModel(app: Application) : AndroidViewModel(app) {

    var loaded by mutableStateOf(false)
        private set
    var screen by mutableStateOf(Screen.HOME)
        private set
    var bankSize by mutableIntStateOf(0)
        private set
    var homeUi by mutableStateOf(
        HomeUi(0, 0, 0, 0, 0, null)
    )
        private set
    var practiceUi by mutableStateOf<PracticeUi?>(null)
        private set
    var examUi by mutableStateOf<ExamUi?>(null)
        private set
    var resultUi by mutableStateOf<ResultUi?>(null)
        private set
    var emptyReason by mutableStateOf<EmptyReason?>(null)
        private set
    var soundEnabled by mutableStateOf(true)
        private set

    /** 界面风格（PxStyles.STYLES 之一），切换即时生效并持久化 */
    var styleId by mutableStateOf("classic")
        private set

    /** 皮肤（PxStyles.SKINS 之一）：像素/圆润/毛玻璃/纸卡/新拟态 */
    var skinId by mutableStateOf(com.km1quiz.app.ui.theme.PxStyles.SKIN_PIXEL)
        private set

    fun setStyle(id: String) {
        styleId = id
        prefs.styleId = id
    }

    fun setSkin(id: String) {
        skinId = id
        prefs.skinId = id
    }

    /** 考试倒计时驱动（每秒更新，UI 据此刷新剩余时间） */
    var examNowMs by mutableStateOf(0L)
        private set

    private val prefs = Prefs(app)
    private var engine: QuizEngine? = null
    private var timerJob: Job? = null

    init {
        soundEnabled = prefs.soundEnabled
        styleId = prefs.styleId
        skinId = prefs.skinId
        viewModelScope.launch {
            val bank = withContext(Dispatchers.IO) { runCatching { Bank.load(app) }.getOrDefault(emptyList()) }
            val eng = QuizEngine(bank, prefs.loadState())
            eng.onChange = { refresh() }
            engine = eng
            bankSize = bank.size
            refresh()
            loaded = true
        }
    }

    // ---------- 状态快照 ----------
    private fun refresh() {
        val eng = engine ?: return
        prefs.saveState(eng.state) // 每次变更即持久化（apply 异步落盘）
        homeUi = HomeUi(
            total = eng.questions.size,
            answered = eng.state.answeredCount,
            accuracy = if (eng.state.answeredCount > 0) {
                (eng.state.correctCount * 100 / eng.state.answeredCount)
            } else 0,
            wrongCount = eng.state.wrong.size,
            starredCount = eng.state.starred.size,
            lastExam = eng.state.examHistory.lastOrNull()
        )
        eng.practice?.let { s ->
            val byId = eng.questions.associateBy { it.id }
            val q = byId[s.queue.getOrNull(s.index)]
            val record = q?.let { eng.state.answers[it.id] }
            practiceUi = PracticeUi(
                mode = s.mode,
                index = s.index,
                size = s.size,
                done = s.index >= s.size,
                q = q,
                picked = record?.picked,
                correct = record?.correct,
                starred = q != null && eng.isStarred(q.id)
            )
        } ?: run { practiceUi = null }
        eng.exam?.let { e ->
            examUi = ExamUi(
                index = e.index,
                size = e.paper.size,
                q = e.paper.getOrNull(e.index),
                picked = e.picked[e.index].takeIf { it >= 0 },
                answered = e.answeredCount(),
                confirming = e.confirming,
                remainSec = e.remainSeconds(examNowMs.takeIf { it > 0 } ?: System.currentTimeMillis()),
                sheetPicked = e.picked.toList()
            )
            e.result?.let { r ->
                resultUi = ResultUi(r.score, r.total, r.durationSec, r.wrongList)
            } ?: run { resultUi = null }
        } ?: run { examUi = null; resultUi = null }
        emptyReason = eng.emptyReason
    }

    // ---------- 首页入口 ----------
    fun startPractice(mode: PracticeMode) {
        val eng = engine ?: return
        eng.startPractice(mode)
        screen = if (eng.practice != null) Screen.PRACTICE else Screen.EMPTY
        Sfx.tap()
    }

    fun startExam() {
        val eng = engine ?: return
        eng.startExam()
        screen = if (eng.exam != null) {
            startExamTimer()
            Screen.EXAM
        } else {
            Screen.EMPTY
        }
        Sfx.tap()
    }

    // ---------- 练习 ----------
    fun practicePick(picked: Int) {
        val eng = engine ?: return
        val wasCorrect = picked == (practiceUi?.q?.answer ?: -1)
        eng.practicePick(picked)
        refresh()
        if (wasCorrect) { Sfx.correct() } else { Sfx.wrong() }
    }

    fun practiceStep(delta: Int) {
        val eng = engine ?: return
        eng.practiceStep(delta)
        if (eng.practice == null) { screen = Screen.HOME } else { refresh() }
        Sfx.tap()
    }

    fun toggleStar() {
        val eng = engine ?: return
        eng.toggleStar()
        refresh()
        Sfx.tap()
    }

    // ---------- 考试 ----------
    fun examPick(picked: Int) {
        val eng = engine ?: return
        eng.examPick(picked)
        refresh()
        Sfx.tap()
    }

    fun examGoto(i: Int) {
        val eng = engine ?: return
        eng.examGoto(i)
        refresh()
    }

    fun cancelConfirm() {
        engine?.cancelConfirm()
        refresh()
    }

    /** force=false 时若有未答题仅进入确认态（返回 false）；真正交卷返回 true */
    fun submitExam(force: Boolean): Boolean {
        val eng = engine ?: return false
        val submitted = eng.submitExam(force)
        refresh()
        if (submitted) {
            stopExamTimer()
            screen = Screen.RESULT
            val passed = (resultUi?.score ?: 0) >= QuizEngine.PASS_SCORE
            if (passed) { Sfx.fanfare() } else { Sfx.wrong() }
        }
        return submitted
    }

    /** 考试中主动退出（成绩不作数） */
    fun quitExam() {
        engine?.exitToHome()
        stopExamTimer()
        refresh()
        screen = Screen.HOME
    }

    fun exitToHome() {
        engine?.exitToHome()
        stopExamTimer()
        refresh()
        screen = Screen.HOME
        Sfx.tap()
    }

    fun toggleSound() {
        soundEnabled = !soundEnabled
        prefs.soundEnabled = soundEnabled
        Sfx.enabled = soundEnabled
        if (soundEnabled) { Sfx.tap() }
    }

    // ---------- 考试计时 ----------
    private fun startExamTimer() {
        timerJob?.cancel()
        examNowMs = System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val e = engine?.exam ?: break
                examNowMs = System.currentTimeMillis()
                val remain = e.remainSeconds(examNowMs)
                if (remain <= 0) {
                    submitExam(true) // 到点自动交卷
                    break
                }
                if (remain <= 300 && remain % 60 == 0) { Sfx.tap() } // 最后 5 分钟每分钟提示音
                refresh()
            }
        }
    }

    private fun stopExamTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        engine?.let { prefs.saveState(it.state) }
        stopExamTimer()
        super.onCleared()
    }
}
