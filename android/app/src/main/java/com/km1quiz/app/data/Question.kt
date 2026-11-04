package com.km1quiz.app.data

/** 题目类型：judge 判断题 / single 单选题 */
enum class QType { JUDGE, SINGLE }

data class Question(
    val id: String,
    val type: QType,
    val question: String,
    val image: String?,        // 相对 assets 的路径（questions/xxx.jpg）或 null
    val options: List<String>,
    val answer: Int,           // 正确选项索引
    val explain: String,
    val law: String
)

/** 题库加载与解析（org.json：设备上用框架实现，JVM 单测用 org.json:json） */
object Bank {

    /** 清洗源数据残留的 HTML（906 个字段含 <p>/<br/>/<span> 等）：<br> 转换行，其余标签剥除，常见实体解码 */
    fun cleanText(s: String?): String {
        if (s.isNullOrEmpty()) { return "" }
        return s
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    /** 解析 bank.json 文本；bank 字段：id/type/question/image/options/answer/explain/law */
    fun parse(json: String): List<Question> {
        val arr = org.json.JSONArray(json)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Question(
                id = o.getString("id"),
                type = if (o.optString("type") == "judge") QType.JUDGE else QType.SINGLE,
                question = cleanText(o.optString("question")),
                image = o.optString("image").ifEmpty { null },
                options = o.optJSONArray("options")?.let { opts ->
                    List(opts.length()) { cleanText(opts.optString(it)) }
                } ?: emptyList(),
                answer = o.optInt("answer", -1),
                explain = cleanText(o.optString("explain")),
                law = cleanText(o.optString("law"))
            )
        }
    }

    /** 从 assets 加载（构建期由 gradle 任务从仓库 data/ 复制） */
    fun load(context: android.content.Context): List<Question> =
        parse(context.assets.open("bank.json").bufferedReader().use { it.readText() })
}
