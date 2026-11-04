package com.km1quiz.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.km1quiz.app.R

/** Fusion Pixel（缝合像素字体，OFL 开源）— 全局像素感的基础 */
val PixelFont = FontFamily(Font(R.font.fusion_pixel))

/**
 * 像素风调色板：深色 = 深蓝黑底 + 霓虹；浅色 = 奶油纸面 + 浓墨描边（复古游戏说明书风）。
 * 硬边框 + 硬阴影由组件层实现，这里只管颜色。
 */
@Immutable
data class Px(
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val border: Color,
    val shadow: Color,
    val text: Color,
    val textDim: Color,
    val accent: Color,
    val accentFg: Color,
    val ok: Color,
    val bad: Color,
    val warn: Color,
    val cyan: Color
)

val PxDark = Px(
    bg = Color(0xFF101322),
    surface = Color(0xFF1A2036),
    surfaceAlt = Color(0xFF232B47),
    border = Color(0xFF3D4A73),
    shadow = Color(0xFF05070F),
    text = Color(0xFFE9E6D6),
    textDim = Color(0xFF8D96B8),
    accent = Color(0xFFFFD23F),
    accentFg = Color(0xFF1B2040),
    ok = Color(0xFF3ECF6E),
    bad = Color(0xFFFF5D5D),
    warn = Color(0xFFFFA94D),
    cyan = Color(0xFF43D9E6)
)

val PxLight = Px(
    bg = Color(0xFFEFE9D8),
    surface = Color(0xFFF8F4E6),
    surfaceAlt = Color(0xFFE6DFC8),
    border = Color(0xFF2E2C24),
    shadow = Color(0xFF2E2C24),
    text = Color(0xFF2E2C24),
    textDim = Color(0xFF6E6B5C),
    accent = Color(0xFFFFC93C),
    accentFg = Color(0xFF2E2C24),
    ok = Color(0xFF1E9E4F),
    bad = Color(0xFFD64545),
    warn = Color(0xFFB25E09),
    cyan = Color(0xFF0F7E8C)
)

val TypographyPx = Typography(
    displayLarge = TextStyle(fontFamily = PixelFont, fontSize = 56.sp, lineHeight = 60.sp),
    displayMedium = TextStyle(fontFamily = PixelFont, fontSize = 40.sp, lineHeight = 44.sp),
    headlineMedium = TextStyle(fontFamily = PixelFont, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = PixelFont, fontSize = 17.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = PixelFont, fontSize = 15.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = PixelFont, fontSize = 13.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = PixelFont, fontSize = 11.sp, lineHeight = 15.sp)
)

/** 全局取像素调色板：val px = LocalPx.current */
val LocalPx = staticCompositionLocalOf { PxDark }

@Composable
fun PixelTheme(
    styleId: String = PxStyles.DEFAULT_ID,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val px = PxStyles.resolve(styleId, darkTheme)
    val scheme = androidx.compose.material3.darkColorScheme(
        primary = px.accent,
        onPrimary = px.accentFg,
        secondary = px.cyan,
        background = px.bg,
        onBackground = px.text,
        surface = px.surface,
        onSurface = px.text,
        error = px.bad
    )
    // 关键：未显式指定颜色的 Text 走 LocalContentColor（默认黑字，深色主题下不可读），
    // 这里统一注入主题前景色，一处修复所有屏幕。
    androidx.compose.runtime.CompositionLocalProvider(
        LocalPx provides px,
        androidx.compose.material3.LocalContentColor provides px.text
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = TypographyPx,
            content = content
        )
    }
}
