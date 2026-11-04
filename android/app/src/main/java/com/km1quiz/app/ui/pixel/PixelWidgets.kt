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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.km1quiz.app.ui.theme.Px
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * 像素设计系统 v2
 * 原则：默认扁平（边框表达结构），硬阴影仅用于主 CTA/弹窗（elevated），
 * 按压统一"下沉 1dp"的键帽手感，杜绝大面积阴影块。
 */

/** 像素卡片：默认扁平；elevated=true 时带 3dp 硬阴影（用于主角元素） */
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
    Box(modifier.fillMaxWidth()) {
        if (elevated) {
            Box(
                Modifier
                    .matchParentSize()
                    .offset(x = 3.dp, y = 3.dp)
                    .background(px.shadow)
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(bg)
                .border(borderWidth, px.border)
                .padding(contentPadding),
            content = content
        )
    }
}

/** 像素按钮：flat=日常（边框+按压下沉 1dp），elevated=主 CTA（3dp 硬影，按下沉入） */
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
    val depth by animateDpAsState(
        targetValue = when {
            pressed && elevated -> 3.dp
            pressed -> 1.dp
            else -> 0.dp
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 900f),
        label = "depth"
    )
    Box(modifier) {
        if (elevated) {
            Box(
                Modifier
                    .matchParentSize()
                    .offset(x = 3.dp, y = 3.dp)
                    .background(if (enabled) px.shadow else px.border.copy(alpha = 0.3f))
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .offset(x = depth, y = depth)
                .background(if (enabled) bg else px.surfaceAlt)
                .border(2.dp, px.border)
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

/** 方形图标按钮（上一题/下一题/星标/关闭等） */
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

/** 像素小徽章：题型（判断/单选）、图例等 */
@Composable
fun PixelChip(text: String, px: Px, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(color.copy(alpha = 0.16f))
            .border(2.dp, color)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** 区块小标题：像素四点 + 文字（增加像素活跃感） */
@Composable
fun SectionTitle(text: String, px: Px, color: Color = px.accent, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        PixelQuads(color, Modifier.size(10.dp, 10.dp))
        SpacerW(6.dp)
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** 2x2 像素四点装饰 */
@Composable
fun PixelQuads(color: Color, modifier: Modifier = Modifier) {
    val block = with(androidx.compose.ui.platform.LocalDensity.current) { 2.5.dp.toPx() }
    androidx.compose.foundation.Canvas(modifier) {
        drawRect(color, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Size(block, block))
        drawRect(color, androidx.compose.ui.geometry.Offset(block * 2, 0f), androidx.compose.ui.geometry.Size(block, block))
        drawRect(color, androidx.compose.ui.geometry.Offset(0f, block * 2), androidx.compose.ui.geometry.Size(block, block))
        drawRect(color, androidx.compose.ui.geometry.Offset(block * 2, block * 2), androidx.compose.ui.geometry.Size(block, block))
    }
}

/** 像素虚线分隔线（4dp 方块间隔排布） */
@Composable
fun PixelDivider(px: Px, modifier: Modifier = Modifier, color: Color = px.border) {
    androidx.compose.foundation.Canvas(
        modifier
            .fillMaxWidth()
            .height(4.dp)
    ) {
        val block = 4.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawRect(color, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Size(block, block))
            x += block * 2
        }
    }
}

/** 便捷横向空隙 */
@Composable
fun SpacerW(w: Dp) { androidx.compose.foundation.layout.Spacer(Modifier.width(w)) }

/** 分格块状进度条：填充块弹性逐格弹出 */
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
        targetValue = progress.coerceIn(0f, 1f) * blocks,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "barFill"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = modifier) {
        repeat(blocks) { i ->
            val active = i < animated
            val frac = (animated - i).coerceIn(0f, 1f)
            val scale by animateFloatAsState(
                targetValue = if (active) 0.7f + 0.3f * frac else 0.55f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "blk"
            )
            Box(
                Modifier
                    .size(blockW, blockH)
                    .scale(scale)
                    .background(if (active) color else px.surfaceAlt)
                    .border(1.dp, px.border)
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

/** 角标：>0 时持续脉冲 */
@Composable
fun PixelBadge(count: Int, px: Px, modifier: Modifier = Modifier) {
    if (count <= 0) { return }
    val t = rememberInfiniteTransition(label = "badge")
    val pulse by t.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(480), RepeatMode.Reverse),
        label = "pulse"
    )
    Box(
        modifier
            .scale(pulse)
            .background(px.bad)
            .border(2.dp, px.border)
            .padding(horizontal = 7.dp, vertical = 1.dp)
    ) {
        Text("$count", style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

/** 错误抖动：key 变化时横向 shake */
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

/** 像素弹窗：遮罩 + 弹簧缩放卡片 */
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
