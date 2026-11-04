package com.km1quiz.app.ui.pixel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.km1quiz.app.ui.theme.Px
import com.km1quiz.app.ui.theme.Skin
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * 像素设计系统 v3 —— 多皮肤组件库
 * 每个组件根据 px.skin 切换视觉语言：
 * PIXEL 直角硬边框硬阴影 / SOFT 大圆角柔和投影 / GLASS 半透明细光边 /
 * PAPER 纸卡细边框 / NEU 同色系双影凸起。
 */

// ---------- 皮肤基础形状/颜色 ----------

private fun cardShape(px: Px): Shape = when (px.skin) {
    Skin.PIXEL -> RoundedCornerShape(0.dp)
    Skin.GLASS -> RoundedCornerShape(px.corner)
    Skin.PAPER -> RoundedCornerShape(px.corner)
    Skin.NEU -> RoundedCornerShape(px.corner)
    Skin.SOFT -> RoundedCornerShape(px.corner)
}

private fun btnShape(px: Px): Shape = when (px.skin) {
    Skin.PIXEL -> RoundedCornerShape(0.dp)
    Skin.GLASS -> RoundedCornerShape(16.dp)
    Skin.PAPER -> RoundedCornerShape(10.dp)
    Skin.NEU -> RoundedCornerShape(14.dp)
    Skin.SOFT -> RoundedCornerShape(14.dp)
}

/** 新拟态双影（左上受光、右下投影）：偏移形状 + 模糊，避免硬边方角溢出 */
private fun Modifier.neuShadow(light: Color, dark: Color, corner: Dp, elevation: Dp = 4.dp): Modifier =
    this
        .blur(6.dp)
        .drawBehind {
            val r = CornerRadius(corner.toPx())
            val e = elevation.toPx()
            drawRoundRect(light, topLeft = Offset(-e, -e), size = size, cornerRadius = r)
            drawRoundRect(dark, topLeft = Offset(e, e), size = size, cornerRadius = r)
        }

/** 像素卡片：按皮肤渲染容器 */
@Composable
fun PixelCard(
    px: Px,
    modifier: Modifier = Modifier,
    bg: Color = px.surface,
    elevated: Boolean = false,
    borderWidth: Dp = 2.dp,
    contentPadding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = cardShape(px)
    Box(modifier.fillMaxWidth()) {
        when (px.skin) {
            Skin.PIXEL -> if (elevated) {
                Box(Modifier.matchParentSize().offset(x = 3.dp, y = 3.dp).background(px.shadow))
            }
            Skin.NEU -> {
                val darkBg = com.km1quiz.app.ui.theme.PxStyles.isDarkPx(px)
                val light = Color.White.copy(alpha = if (darkBg) 0.07f else 0.85f)
                val dark = if (darkBg) Color.Black.copy(alpha = 0.55f) else Color(0xFFB9B5AC)
                Box(
                    Modifier
                        .matchParentSize()
                        .offset(x = (-3).dp, y = (-3).dp)
                        .neuShadow(light, dark, px.corner, elevation = 5.dp)
                )
            }
            else -> {}
        }
        Column(
            Modifier
                .fillMaxWidth()
                .then(
                    when (px.skin) {
                        Skin.PIXEL -> Modifier
                        // 毛玻璃不用系统阴影：半透明表面下阴影会形成实心衬底
                        Skin.GLASS -> Modifier
                        Skin.PAPER -> Modifier.shadow(if (elevated) 5.dp else 2.dp, shape)
                        Skin.NEU -> Modifier
                        Skin.SOFT -> Modifier.shadow(if (elevated) 8.dp else 4.dp, shape)
                    }
                )
                .background(if (px.skin == Skin.NEU) px.bg else bg, shape)
                .then(
                    when (px.skin) {
                        Skin.PIXEL -> Modifier.border(borderWidth, px.border, shape)
                        Skin.PAPER -> Modifier.border(1.dp, px.border, shape)
                        Skin.GLASS -> Modifier.border(
                            if (elevated) 2.dp else 1.dp,
                            px.border.copy(alpha = 0.5f),
                            shape
                        )
                        else -> Modifier
                    }
                )
                .padding(contentPadding),
            content = content
        )
    }
}

/** 像素按钮：按皮肤渲染，按压反馈为下沉(像素)或缩放(其余) */
@Composable
fun PixelButton(
    px: Px,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    bg: Color = px.accent,
    fg: Color = px.accentFg,
    elevated: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    content: @Composable RowScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = btnShape(px)
    val pressDepth by animateDpAsState(
        targetValue = when {
            pressed && px.skin == Skin.PIXEL && elevated -> 3.dp
            pressed && px.skin == Skin.PIXEL -> 1.dp
            else -> 0.dp
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 900f),
        label = "depth"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && px.skin != Skin.PIXEL) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 900f),
        label = "pressScale"
    )
    val effectiveBg = when {
        px.skin == Skin.NEU && bg == px.surfaceAlt -> px.bg
        px.skin == Skin.NEU && bg == px.surface -> px.bg
        else -> bg
    }
    Box(modifier) {
        if (px.skin == Skin.PIXEL && elevated) {
            Box(
                Modifier
                    .matchParentSize()
                    .offset(x = 3.dp, y = 3.dp)
                    .background(if (enabled) px.shadow else px.border.copy(alpha = 0.3f))
            )
        }
        if (px.skin == Skin.NEU && !pressed) {
            val darkBg = com.km1quiz.app.ui.theme.PxStyles.isDarkPx(px)
            val light = Color.White.copy(alpha = if (darkBg) 0.07f else 0.8f)
            val dark = if (darkBg) Color.Black.copy(alpha = 0.5f) else Color(0xFFB9B5AC)
            Box(
                Modifier
                    .matchParentSize()
                    .neuShadow(light, dark, corner = 14.dp, elevation = 4.dp)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .offset(x = pressDepth, y = pressDepth)
                .scale(pressScale)
                .then(
                    when (px.skin) {
                        Skin.PIXEL -> Modifier
                        Skin.GLASS -> Modifier
                        Skin.PAPER -> Modifier.shadow(2.dp, shape)
                        Skin.NEU -> Modifier
                        Skin.SOFT -> Modifier.shadow(if (elevated) 5.dp else 2.dp, shape)
                    }
                )
                .background(if (enabled) effectiveBg else px.surfaceAlt, shape)
                .then(
                    when (px.skin) {
                        Skin.PIXEL -> Modifier.border(2.dp, px.border, shape)
                        Skin.PAPER -> Modifier.border(1.dp, px.border, shape)
                        Skin.GLASS -> Modifier.border(1.dp, px.border, shape)
                        Skin.NEU -> if (pressed) Modifier.border(1.dp, Color.Black.copy(alpha = 0.18f), shape) else Modifier
                        Skin.SOFT -> Modifier
                    }
                )
                .alpha(if (enabled) 1f else 0.55f)
                .clickable(interactionSource = interaction, indication = null, enabled = enabled) { onClick() },
            content = content
        )
    }
}

/** 纯文字像素按钮 */
@Composable
fun PixelTextButton(
    text: String,
    px: Px,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    bg: Color = px.accent,
    fg: Color = px.accentFg,
    elevated: Boolean = false,
    small: Boolean = false
) {
    PixelButton(px = px, onClick = onClick, modifier = modifier, enabled = enabled, bg = bg, elevated = elevated) {
        Text(
            text,
            style = if (small) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
            color = fg,
            modifier = Modifier.padding(
                horizontal = if (small) 10.dp else 16.dp,
                vertical = if (small) 6.dp else 12.dp
            )
        )
    }
}

/** 方形图标按钮 */
@Composable
fun PixelIconButton(
    glyph: List<String>,
    px: Px,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    bg: Color = px.surfaceAlt
) {
    PixelButton(px = px, onClick = onClick, modifier = modifier, bg = bg) {
        PixelIcon(
            glyph = glyph,
            color = tint,
            modifier = Modifier
                .padding((size.value / 48f * 11f).dp)
                .size(24.dp, 24.dp)
        )
    }
}

/** 小徽章 chip */
@Composable
fun PixelChip(text: String, px: Px, color: Color, modifier: Modifier = Modifier) {
    val shape = when (px.skin) {
        Skin.PIXEL -> RoundedCornerShape(0.dp)
        else -> RoundedCornerShape(999.dp)
    }
    Box(
        modifier
            .background(color.copy(alpha = if (px.skin == Skin.PIXEL) 0.16f else 0.22f), shape)
            .then(
                if (px.skin == Skin.PIXEL) {
                    Modifier.border(2.dp, color, shape)
                } else if (px.skin == Skin.GLASS) {
                    Modifier.border(1.dp, color.copy(alpha = 0.5f), shape)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 10.dp, vertical = if (px.skin == Skin.PIXEL) 2.dp else 3.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** 区块小标题：像素四点 + 文字 */
@Composable
fun SectionTitle(text: String, px: Px, color: Color = px.accent, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        PixelQuads(color, Modifier.size(if (px.skin == Skin.PIXEL) 10.dp else 12.dp, if (px.skin == Skin.PIXEL) 10.dp else 12.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** 2x2 四点装饰（圆润皮肤画圆点） */
@Composable
fun PixelQuads(color: Color, modifier: Modifier = Modifier) {
    val soft = com.km1quiz.app.ui.theme.LocalPx.current.iconSoft
    val block = with(androidx.compose.ui.platform.LocalDensity.current) { 2.5.dp.toPx() }
    androidx.compose.foundation.Canvas(modifier) {
        val positions = listOf(
            Offset(0f, 0f), Offset(block * 2, 0f),
            Offset(0f, block * 2), Offset(block * 2, block * 2)
        )
        positions.forEach { p ->
            if (soft) {
                drawCircle(color, radius = block * 0.75f, center = p + Offset(block / 2, block / 2))
            } else {
                drawRect(color, p, androidx.compose.ui.geometry.Size(block, block))
            }
        }
    }
}

/** 分隔线：像素=方块虚线，其余=细线 */
@Composable
fun PixelDivider(px: Px, modifier: Modifier = Modifier, color: Color = px.border) {
    if (px.skin == Skin.PIXEL) {
        androidx.compose.foundation.Canvas(
            modifier
                .fillMaxWidth()
                .height(4.dp)
        ) {
            val block = 4.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawRect(color, Offset(x, 0f), androidx.compose.ui.geometry.Size(block, block))
                x += block * 2
            }
        }
    } else {
        Box(
            modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(color.copy(alpha = 0.35f), RoundedCornerShape(1.dp))
        )
    }
}

/** 进度条：像素=分格块，其余=连续圆角条 */
@Composable
fun PixelBlockBar(
    progress: Float,
    px: Px,
    modifier: Modifier = Modifier,
    blocks: Int = 14,
    blockW: Dp = 10.dp,
    blockH: Dp = 14.dp,
    color: Color = px.accent
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "barFill"
    )
    if (px.barDiscrete) {
        val blockFill = animated * blocks
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = modifier) {
            repeat(blocks) { i ->
                val active = i < blockFill
                val frac = (blockFill - i).coerceIn(0f, 1f)
                val scaleB by animateFloatAsState(
                    targetValue = if (active) 0.7f + 0.3f * frac else 0.55f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "blk"
                )
                Box(
                    Modifier
                        .size(blockW, blockH)
                        .scale(scaleB)
                        .background(if (active) color else px.surfaceAlt)
                        .border(1.dp, px.border)
                )
            }
        }
    } else {
        BoxWithConstraints(modifier.fillMaxWidth().height(10.dp)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(px.surfaceAlt, RoundedCornerShape(5.dp))
            )
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(maxWidth * animated)
                    .background(color, RoundedCornerShape(5.dp))
            )
        }
    }
}

/** 数字跳动累加文本 */
@Composable
fun CountUpText(
    target: Int,
    style: TextStyle,
    suffix: String = "",
    color: Color = Color.Unspecified,
    durationMs: Int = 700
) {
    val shown by animateIntAsState(
        targetValue = target,
        animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
        label = "countUp"
    )
    Text("$shown$suffix", style = style, color = color)
}

/** 角标：>0 时脉冲 */
@Composable
fun PixelBadge(count: Int, px: Px, modifier: Modifier = Modifier) {
    if (count <= 0) { return }
    val t = rememberInfiniteTransition(label = "badge")
    val pulse by t.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(480), RepeatMode.Reverse),
        label = "pulse"
    )
    val shape = if (px.skin == Skin.PIXEL) RoundedCornerShape(0.dp) else RoundedCornerShape(999.dp)
    Box(
        modifier
            .scale(pulse)
            .background(px.bad, shape)
            .then(if (px.skin == Skin.PIXEL) Modifier.border(2.dp, px.border, shape) else Modifier)
            .padding(horizontal = 7.dp, vertical = if (px.skin == Skin.PIXEL) 1.dp else 2.dp)
    ) {
        Text("$count", style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

/** 错误抖动 */
@Composable
fun Modifier.shakeOn(shakeKey: Any): Modifier {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(shakeKey) {
        if (shakeKey == 0 || shakeKey == "0:null") { return@LaunchedEffect }
        anim.snapTo(0f)
        listOf(0f, -10f, 9f, -7f, 5f, -3f, 2f, 0f).forEach { v ->
            anim.snapTo(v)
            delay(26)
        }
    }
    return this.offset { IntOffset(anim.value.roundToInt(), 0) }
}

/** 列表项依次弹入 */
@Composable
fun StaggerIn(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 70L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + slideInVertically(tween(320), initialOffsetY = { it / 3 }),
        exit = fadeOut(tween(80))
    ) { content() }
}

/** 弹窗：遮罩 + 弹簧缩放卡片（卡片皮肤随 PixelCard） */
@Composable
fun PixelDialog(
    px: Px,
    onDismiss: (() -> Unit)? = null,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val appear = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        // 高刚度弹性：保留砸下的弹跳感，但 ~300ms 内收敛，避免动画期间缩放未稳导致首拍落空
        appear.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 1200f))
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                enabled = onDismiss != null,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss?.invoke() },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 28.dp).scale(appear.value)) {
            PixelCard(px = px, bg = px.surface, elevated = true, contentPadding = 16.dp) {
                Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 10.dp))
                content()
            }
        }
    }
}

/** 字母键帽：像素=方键帽，圆润=圆形字母底 */
@Composable
fun KeycapLetter(
    label: String,
    bgColor: Color,
    textColor: Color,
    edgeColor: Color,
    px: Px,
    size: Dp = 30.dp
) {
    if (px.skin == Skin.PIXEL) {
        Box(
            Modifier
                .size(size)
                .background(bgColor)
                .border(2.dp, edgeColor),
            contentAlignment = Alignment.Center
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = textColor)
        }
    } else {
        Box(
            Modifier
                .size(size)
                .background(bgColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = textColor)
        }
    }
}
