package com.km1quiz.app

import com.km1quiz.app.data.QType
import com.km1quiz.app.logic.PersistentState
import com.km1quiz.app.logic.PracticeMode
import com.km1quiz.app.logic.QuizEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 业务逻辑单测：断言口径对齐 VSCode 扩展版 scripts/test-webview.js，
 * 保证两个平台行为一致（答对/答错入错题本、答对移出、收藏、顺序进度、模拟考全流程、状态恢复）。
 */
class QuizLogicTest {

    private fun bank(): List<com.km1quiz.app.data.Question> = (0 until 120).map { i ->
        com.km1quiz.app.data.Question(
            id = "q$i",
            type = if (i < 50) QType.JUDGE else QType.SINGLE, // 50 判断 + 70 单选
            question = "第 $i 题",
            image = null,
            options = if (i < 50) listOf("对", "错") else listOf("A项", "B项", "C项", "D项"),
            answer = i % (if (i < 50) 2 else 4),
            explain = "解析$i",
            law = ""
        )
    }

    private val byId get() = bank().associateBy { it.id }

    @Test
    fun `顺序练习 答对记录且不进错题本`() {
        val e = QuizEngine(bank())
        e.startPractice(PracticeMode.SEQUENTIAL)
        val s = e.practice!!
        assertEquals(0, s.index)
        val q = byId["q0"]!!
        e.practicePick(q.answer)
        val rec = e.state.answers[q.id]!!
        assertTrue(rec.correct)
        assertFalse(e.state.wrong.contains(q.id))
    }

    @Test
    fun `顺序练习 答错进错题本且进度持久化`() {
        val e = QuizEngine(bank())
        e.startPractice(PracticeMode.SEQUENTIAL)
        val q = byId["q0"]!!
        e.practicePick(q.answer) // 第 1 题答对
        e.practiceStep(1)        // 到第 2 题
        val q2 = byId["q1"]!!
        val wrongPick = (q2.answer + 1) % q2.options.size
        e.practicePick(wrongPick)
        assertTrue(e.state.wrong.contains(q2.id))
        assertEquals(1, e.state.sequentialIndex)
    }

    @Test
    fun `收藏开关切换`() {
        val e = QuizEngine(bank())
        e.startPractice(PracticeMode.SEQUENTIAL)
        val q = byId["q0"]!!
        e.toggleStar()
        assertTrue(e.state.starred.contains(q.id))
        e.toggleStar()
        assertFalse(e.state.starred.contains(q.id))
    }

    @Test
    fun `错题重练 答对自动移出并清空队列`() {
        val e = QuizEngine(bank())
        e.startPractice(PracticeMode.SEQUENTIAL)
        e.practicePick((byId["q0"]!!.answer + 1) % 2) // 答错 q0
        assertTrue(e.state.wrong.contains("q0"))
        e.exitToHome()
        e.startPractice(PracticeMode.WRONG)
        val s = e.practice!!
        assertEquals(1, s.size)
        e.practicePick(byId["q0"]!!.answer) // 答对 → 移出 + splice
        assertFalse(e.state.wrong.contains("q0"))
        assertTrue("splice 后 index 越界代表本轮完成", s.index >= s.size)
    }

    @Test
    fun `空错题本 空态提示`() {
        val e = QuizEngine(bank())
        e.startPractice(PracticeMode.WRONG)
        assertNull(e.practice)
        assertEquals(com.km1quiz.app.logic.EmptyReason.WRONG_EMPTY, e.emptyReason)
    }

    @Test
    fun `模拟考试 100题构成与答题流转`() {
        val e = QuizEngine(bank())
        val t0 = 1_000_000L
        e.startExam(t0)
        val exam = e.exam!!
        assertEquals(QuizEngine.EXAM_TOTAL, exam.paper.size)
        assertEquals(QuizEngine.EXAM_JUDGE, exam.paper.count { it.type == QType.JUDGE })
        assertEquals(QuizEngine.EXAM_TOTAL - QuizEngine.EXAM_JUDGE, exam.paper.count { it.type == QType.SINGLE })

        e.examPick(0)
        assertEquals(0, exam.picked[0])
        e.examGoto(5)
        assertEquals(5, exam.index)
        e.examPick(1)
        assertEquals(2, exam.answeredCount())

        // 有未答题：首次交卷 → 确认态，未出分
        assertFalse(e.submitExam(false, t0 + 60_000))
        assertTrue(exam.confirming)
        // 强制交卷 → 出分并入历史
        assertTrue(e.submitExam(true, t0 + 120_000))
        val r = exam.result!!
        assertEquals(100, r.total)
        assertTrue(r.score in 0..100)
        assertEquals(1, e.state.examHistory.size)
        assertEquals(100, e.state.examHistory[0].total)
        assertEquals(120, e.state.examHistory[0].durationSec)
        // 卷内错题进错题本
        if (r.score < 100) {
            assertTrue(e.state.wrong.isNotEmpty())
        }
    }

    @Test
    fun `考试倒计时到点自动交卷`() {
        val e = QuizEngine(bank())
        val t0 = 5_000_000L
        e.startExam(t0)
        // 超时后 force 交卷
        assertTrue(e.submitExam(true, t0 + QuizEngine.EXAM_MINUTES * 60_000L + 1))
        assertNotNull(e.exam!!.result)
    }

    @Test
    fun `模拟考历史保留上限20条`() {
        val e = QuizEngine(bank())
        val t0 = 9_000_000L
        repeat(25) { i ->
            e.startExam(t0 + i)
            e.submitExam(true, t0 + i + 1000)
        }
        assertEquals(QuizEngine.HISTORY_LIMIT, e.state.examHistory.size)
        assertEquals(QuizEngine.EXAM_TOTAL, e.state.examHistory.last().total)
    }

    @Test
    fun `状态恢复 顺序练习续刷且失效记录被清理`() {
        val first = QuizEngine(bank())
        first.startPractice(PracticeMode.SEQUENTIAL)
        first.practicePick(first.questions[0].answer)
        first.practiceStep(1)
        first.practicePick((first.questions[1].answer + 1) % first.questions[1].options.size)

        // 用持久化状态重建（模拟进程重启）；再混入一条题库中不存在的脏数据
        val dirty = first.state.copy(wrong = first.state.wrong + "ghost-id")
        val restored = QuizEngine(bank(), dirty)
        assertEquals(2, restored.state.answeredCount)
        assertEquals(1, restored.state.sequentialIndex)
        assertFalse(restored.state.wrong.contains("ghost-id"))
        restored.startPractice(PracticeMode.SEQUENTIAL)
        assertEquals(1, restored.practice!!.index)
    }

    @Test
    fun `StateJson 序列化往返`() {
        val s = PersistentState(
            answers = mapOf("q1" to com.km1quiz.app.logic.AnswerRecord(2, true)),
            wrong = listOf("q2"),
            starred = listOf("q3", "q4"),
            sequentialIndex = 7,
            examHistory = listOf(com.km1quiz.app.logic.ExamRecord("2026-01-01T00:00:00Z", 95, 100, 600))
        )
        val back = com.km1quiz.app.data.StateJson.fromJson(com.km1quiz.app.data.StateJson.toJson(s))
        assertEquals(s, back)
        assertEquals(PersistentState(), com.km1quiz.app.data.StateJson.fromJson("不是json{{{"))
    }
}
