package com.km1quiz.app

import com.km1quiz.app.data.Bank
import com.km1quiz.app.data.QType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** 真实题库全量校验（对齐 scripts/verify-bank.js 的口径） */
class BankTest {

    private val bank: List<com.km1quiz.app.data.Question> by lazy {
        // gradle 单测工作目录 = android/app
        Bank.parse(File("../data/bank.json").readText())
    }

    @Test
    fun `题库共1757题`() {
        assertEquals(1757, bank.size)
    }

    @Test
    fun `id唯一且类型合法`() {
        assertEquals(bank.size, bank.map { it.id }.toSet().size)
        assertTrue(bank.all { it.type == QType.JUDGE || it.type == QType.SINGLE })
    }

    @Test
    fun `答案索引在选项范围内`() {
        bank.forEach { q ->
            assertTrue("非法答案 ${q.id}", q.answer in q.options.indices)
        }
    }

    @Test
    fun `判断题两选项 单选2到4选项`() {
        assertTrue(bank.filter { it.type == QType.JUDGE }.all { it.options.size == 2 })
        assertTrue(bank.filter { it.type == QType.SINGLE }.all { it.options.size in 2..4 })
    }

    @Test
    fun `解析覆盖率100 percent`() {
        assertTrue(bank.all { it.explain.isNotBlank() })
    }

    @Test
    fun `题目与选项非空`() {
        assertTrue(bank.all { it.question.isNotBlank() })
        assertTrue(bank.all { q -> q.options.all { it.isNotBlank() } })
    }

    @Test
    fun `文本已剥离HTML标签`() {
        // 源数据 906 个字段含 <p>/<br/>/<span> 等，解析层必须清洗
        bank.forEach { q ->
            assertFalse("题干残留HTML ${q.id}", q.question.contains(Regex("<[a-zA-Z/]")))
            assertFalse("解析残留HTML ${q.id}", q.explain.contains(Regex("<[a-zA-Z/]")))
            assertFalse("法条残留HTML ${q.id}", q.law.contains(Regex("<[a-zA-Z/]")))
        }
    }

    @Test
    fun `cleanText行为`() {
        assertEquals("第一行\n第二行", Bank.cleanText("第一行<br/>第二行"))
        assertEquals("去标签留文字", Bank.cleanText("<p>去标签<span style=\"x\">留文字</span></p>"))
        assertEquals("a & b", Bank.cleanText("a &amp; b"))
    }

    @Test
    fun `模拟考题量充足`() {
        val judges = bank.count { it.type == QType.JUDGE }
        val singles = bank.count { it.type == QType.SINGLE }
        assertTrue(judges >= 40 && singles >= 60)
    }
}
