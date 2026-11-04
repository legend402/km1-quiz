package com.km1quiz.app.data

import android.content.Context
import com.km1quiz.app.logic.AnswerRecord
import com.km1quiz.app.logic.ExamRecord
import com.km1quiz.app.logic.PersistentState
import org.json.JSONArray
import org.json.JSONObject

/**
 * 持久化：SharedPreferences 存一份 JSON（字段与 VSCode 扩展版 globalState 的 state 同构：
 * answers / wrong / starred / sequentialIndex / examHistory），加音效开关。
 */
class Prefs(context: Context) {

    private val sp = context.getSharedPreferences("km1_quiz", Context.MODE_PRIVATE)

    fun loadState(): PersistentState =
        sp.getString(KEY_STATE, null)?.let(StateJson::fromJson) ?: PersistentState()

    fun saveState(s: PersistentState) {
        sp.edit().putString(KEY_STATE, StateJson.toJson(s)).apply()
    }

    var soundEnabled: Boolean
        get() = sp.getBoolean(KEY_SOUND, true)
        set(v) { sp.edit().putBoolean(KEY_SOUND, v).apply() }

    var styleId: String
        get() = sp.getString(KEY_STYLE, null) ?: "classic"
        set(v) { sp.edit().putString(KEY_STYLE, v).apply() }

    var skinId: String
        get() = sp.getString(KEY_SKIN, null) ?: "pixel"
        set(v) { sp.edit().putString(KEY_SKIN, v).apply() }

    companion object {
        private const val KEY_STATE = "state"
        private const val KEY_SOUND = "soundEnabled"
        private const val KEY_STYLE = "styleId"
        private const val KEY_SKIN = "skinId"
    }
}

/** PersistentState ↔ JSON（org.json 同样在 JVM 单测可用） */
object StateJson {

    fun toJson(s: PersistentState): String {
        val answers = JSONObject()
        s.answers.forEach { (id, r) ->
            answers.put(id, JSONObject().put("picked", r.picked).put("correct", r.correct))
        }
        val history = JSONArray()
        s.examHistory.forEach { h ->
            history.put(JSONObject()
                .put("date", h.dateIso)
                .put("score", h.score)
                .put("total", h.total)
                .put("durationSec", h.durationSec))
        }
        return JSONObject()
            .put("answers", answers)
            .put("wrong", JSONArray(s.wrong))
            .put("starred", JSONArray(s.starred))
            .put("sequentialIndex", s.sequentialIndex)
            .put("examHistory", history)
            .toString()
    }

    /** 解析失败/字段缺失时防御性回退默认值，绝不让坏数据崩溃 */
    fun fromJson(text: String?): PersistentState {
        if (text.isNullOrBlank()) { return PersistentState() }
        return try {
            val o = JSONObject(text)
            val answers = buildMap {
                val a = o.optJSONObject("answers") ?: JSONObject()
                a.keys().forEach { id ->
                    val r = a.optJSONObject(id) ?: return@forEach
                    put(id, AnswerRecord(r.optInt("picked", -1), r.optBoolean("correct")))
                }
            }
            PersistentState(
                answers = answers,
                wrong = o.optJSONArray("wrong")?.let { arr -> List(arr.length()) { arr.optString(it) } } ?: emptyList(),
                starred = o.optJSONArray("starred")?.let { arr -> List(arr.length()) { arr.optString(it) } } ?: emptyList(),
                sequentialIndex = o.optInt("sequentialIndex", 0),
                examHistory = o.optJSONArray("examHistory")?.let { arr ->
                    List(arr.length()) { i ->
                        val h = arr.optJSONObject(i) ?: JSONObject()
                        ExamRecord(
                            dateIso = h.optString("date"),
                            score = h.optInt("score"),
                            total = h.optInt("total"),
                            durationSec = h.optInt("durationSec")
                        )
                    }
                } ?: emptyList()
            )
        } catch (_: Exception) {
            PersistentState()
        }
    }
}
