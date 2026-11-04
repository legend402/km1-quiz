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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

/** 像素卡片：2dp 硬边框 + 右下 4dp 硬阴影（无圆角、无模糊） */
@Composable
fun PixelCard(
    px: Px,
    modifier: Modifier = Modifier,
    bg: Color = px.surface,
    shadow: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier.fillMaxWidth()) {
        // 硬阴影块：跟随卡片尺寸
        Box(
            Modifier
                .matchParentSize()
                .offset(x = shadow, y = shadow)
                .background(px.shadow)
        )
        Column(
            Modifier
                .fillMaxWidth()
                .background(bg)
                .border(2.dp, px.border)
                .padding(12.dp),
            content = content
        )
    }
}

/** 像素按钮主体（含下沉动画），可自定义行内容 */
@Composable
fun PixelButton(
    px: Px,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    bg: Color = px.accent,
    fg: Color = px.accentFg,
    shadow: Dp = 4.dp,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    content: @Composable RowScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val depth by animateDpAsState(
        targetValue = if (pressed) shadow else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 900f),
        label = "depth"
    )
    Box(modifier) {
        // 固定阴影
        Box(
            Modifier
                .matchParentSize()
                .offset(x = shadow, y = shadow)
                .background(if (enabled) px.shadow else px.border.copy(alpha = 0.35f))
        )
        // 按钮本体：决定尺寸，按下时下沉盖住阴影
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

/** 纯文字像素按钮（默认不带外层 padding 的快捷形态） */
@Composable
fun PixelTextButton(
    text: String,
    px: Px,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    bg: Color = px.accent,
    fg: Color = px.accentFg,
    small: Boolean = false
) {
    PixelButton(px = px, onClick = onClick, modifier = modifier, enabled = enabled, bg = bg) {
        Text(
            text,
            style = if (small) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
            color = fg,
            modifier = Modifier.padding(horizontal = if (small) 8.dp else 14.dp, vertical = if (small) 5.dp else 10.dp)
        )
    }
}

/** 分格块状进度条：填充块以弹性动画逐格弹出 */
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

/** 数字跳动累加文本（首页统计 / 成绩分） */
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

/** 角标：数值 > 0 时持续脉冲 */
@Composable
fun PixelBadge(count: Int, px: Px, modifier: Modifier = Modifier) {
    if (count <= 0) { return }
    val t = rememberInfiniteTransition(label = "badge")
    val pulse by t.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
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

/** 错误抖动：key 变化时横向 shake 一下（0 不触发） */
@Composable
fun Modifier.shakeOn(shakeKey: Any): Modifier {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(shakeKey) {
        if (shakeKey == 0) { return@LaunchedEffect }
        anim.snapTo(0f)
        listOf(0f, -10f, 9f, -7f, 5f, -3f, 2f, 0f).forEach { v ->
            anim.snapTo(v)
            delay(26)
        }
    }
    return this.offset { IntOffset(anim.value.roundToInt(), 0) }
}

/** 列表项依次弹入（stagger）：index 越大越晚出现 */
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

/**
 * 像素弹窗：占满父容器的遮罩 + 居中缩放弹入卡片。
 * 需要全屏时由调用方放入 Box(fillMaxSize)。
 */
@Composable
fun PixelDialog(
    px: Px,
    onDismiss: (() -> Unit)? = null,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val appear = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 380f))
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
        Box(Modifier.fillMaxWidth().padding(horizontal = 30.dp).scale(appear.value)) {
            PixelCard(px = px, bg = px.surface) {
                Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 8.dp))
                content()
            }
        }
    }
}

/** 供屏幕复用：屏幕级容器内的小图标按钮（星标/关闭等） */
@Composable
fun PixelIconButton(
    glyph: List<String>,
    px: Px,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = ""
) {
    PixelButton(px = px, onClick = onClick, modifier = modifier, shadow = 3.dp) {
        PixelIcon(
            glyph = glyph,
            color = tint,
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .size(24.dp, 24.dp)
        )
    }
}

/** 遮罩内容对齐用的占位扩展（避免每个屏幕重复写 Box 组合） */
@Composable
fun BoxScope.CenterOverlay(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center, content = content)
}
