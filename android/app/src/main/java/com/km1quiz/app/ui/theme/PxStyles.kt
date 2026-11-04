package com.km1quiz.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * 界面风格库：每套风格一个 Px 调色板。
 * 经典款跟随系统深浅；其余为固定明暗的个性风格。
 * 新增风格只需往 STYLES 里加一项，选择弹窗自动出现。
 */
data class StyleMeta(
    val id: String,
    val name: String,
    val followSystem: Boolean = false,
    val light: Px? = null,      // followSystem=true 时的浅色板
    val dark: Px? = null,       // followSystem=true 时的深色板
    val fixed: Px? = null       // 固定风格的单板
)

object PxStyles {

    /** 经典像素（默认）：奶油纸面 + 深蓝霓虹，跟随系统深浅 */
    val CLASSIC = StyleMeta(
        id = "classic", name = "经典像素",
        followSystem = true, light = PxLight, dark = PxDark
    )

    /** 掌机绿屏：初代掌机 LCD 四阶绿，bad 用砖红保证可辨识 */
    private val gb = Px(
        bg = Color(0xFF9BBC0F), surface = Color(0xFF93B30D), surfaceAlt = Color(0xFF86A90C),
        border = Color(0xFF0F380F), shadow = Color(0xFF0F380F),
        text = Color(0xFF0F380F), textDim = Color(0xFF306230),
        accent = Color(0xFF306230), accentFg = Color(0xFFC8E58A),
        ok = Color(0xFF1F6B2F), bad = Color(0xFF8B3A2E), warn = Color(0xFF5C5016), cyan = Color(0xFF2F5D3A)
    )

    /** 霓虹夜城：合成器浪潮，深紫夜幕 + 粉青霓虹 */
    private val synth = Px(
        bg = Color(0xFF150A2E), surface = Color(0xFF1F1040), surfaceAlt = Color(0xFF2B1A58),
        border = Color(0xFF5F3DA8), shadow = Color(0xFF06021A),
        text = Color(0xFFF3EDFF), textDim = Color(0xFF9E90C7),
        accent = Color(0xFFFF2D95), accentFg = Color(0xFF2A0714),
        ok = Color(0xFF2DE2A3), bad = Color(0xFFFF5C7A), warn = Color(0xFFFFB84D), cyan = Color(0xFF3AE1FF)
    )

    /** 粉彩糖果：奶油草莓色系，软萌但保持硬边框的利落 */
    private val candy = Px(
        bg = Color(0xFFFDF1F6), surface = Color(0xFFFFFAFC), surfaceAlt = Color(0xFFF7E1EC),
        border = Color(0xFF6E4A5B), shadow = Color(0xFF6E4A5B),
        text = Color(0xFF4A343F), textDim = Color(0xFFA3838F),
        accent = Color(0xFFFF8FC0), accentFg = Color(0xFF5C2B40),
        ok = Color(0xFF33B988), bad = Color(0xFFF0607F), warn = Color(0xFFF09A3E), cyan = Color(0xFF3FB6CE)
    )

    /** 极客终端：磷光绿字符屏，错误用终端红 */
    private val terminal = Px(
        bg = Color(0xFF04120A), surface = Color(0xFF07190E), surfaceAlt = Color(0xFF0C2516),
        border = Color(0xFF1F8A3B), shadow = Color(0xFF000000),
        text = Color(0xFF3CFF7E), textDim = Color(0xFF1FA04F),
        accent = Color(0xFF3CFF7E), accentFg = Color(0xFF04120A),
        ok = Color(0xFF8CFFB0), bad = Color(0xFFFF4D4D), warn = Color(0xFFFFD21E), cyan = Color(0xFF57D8FF)
    )

    /** 墨白报印：铅字报纸，黑白灰 + 印章红点睛 */
    private val ink = Px(
        bg = Color(0xFFF4F1E8), surface = Color(0xFFFFFFFF), surfaceAlt = Color(0xFFE9E5DA),
        border = Color(0xFF141414), shadow = Color(0xFF141414),
        text = Color(0xFF141414), textDim = Color(0xFF6A6A66),
        accent = Color(0xFFD7263D), accentFg = Color(0xFFFFFFFF),
        ok = Color(0xFF0E7B38), bad = Color(0xFFB3202E), warn = Color(0xFF8A6D00), cyan = Color(0xFF14555E)
    )

    val STYLES = listOf(
        CLASSIC,
        StyleMeta(id = "gameboy", name = "掌机绿屏", fixed = gb),
        StyleMeta(id = "synthwave", name = "霓虹夜城", fixed = synth),
        StyleMeta(id = "candy", name = "粉彩糖果", fixed = candy),
        StyleMeta(id = "terminal", name = "极客终端", fixed = terminal),
        StyleMeta(id = "ink", name = "墨白报印", fixed = ink)
    )

    const val DEFAULT_ID = "classic"

    fun byId(id: String?): StyleMeta = STYLES.firstOrNull { it.id == id } ?: CLASSIC

    /** 解析当前应使用的调色板 */
    fun resolve(id: String?, systemDark: Boolean): Px {
        val meta = byId(id)
        return when {
            meta.followSystem -> if (systemDark) meta.dark!! else meta.light!!
            else -> meta.fixed!!
        }
    }

    /** 风格是否偏深色底（用于状态栏图标明暗） */
    fun isDarkPx(px: Px): Boolean = px.bg.luminance() < 0.5f
}
