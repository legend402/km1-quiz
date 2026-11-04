package com.km1quiz.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.km1quiz.app.data.Question
import com.km1quiz.app.ui.pixel.Glyphs
import com.km1quiz.app.ui.pixel.KeycapLetter
import com.km1quiz.app.ui.pixel.PixelIcon
import com.km1quiz.app.ui.pixel.shakeOn
import com.km1quiz.app.ui.theme.LocalPx

/** 选项视觉状态（练习判分 + 考试选中统一建模） */
enum class OptVisual { NORMAL, CORRECT, WRONG, DIM, PICKED }

private val OPTION_LABELS = listOf("A", "B", "C", "D")

/**
 * 选项区：判断题（2 项）并排半宽，其余纵向全宽。
 * stateOf 由调用方决定每个选项的视觉状态；enabled=false 时不可再点。
 */
@Composable
fun OptionsArea(
    q: Question,
    stateOf: (Int) -> OptVisual,
    shakeKey: String,
    enabled: Boolean = true,
    onPick: (Int) -> Unit
) {
    if (q.options.size == 2) {
        // 判断题：A/B 并排，居中，键帽样式
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            q.options.forEachIndexed { idx, opt ->
                if (idx > 0) { Spacer(Modifier.width(10.dp)) }
                OptionRow(
                    label = OPTION_LABELS.getOrElse(idx) { "?" },
                    text = opt,
                    visual = stateOf(idx),
                    shakeKey = shakeKey,
                    enabled = enabled && stateOf(idx) == OptVisual.NORMAL,
                    onPick = { onPick(idx) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            q.options.forEachIndexed { idx, opt ->
                OptionRow(
                    label = OPTION_LABELS.getOrElse(idx) { "?" },
                    text = opt,
                    visual = stateOf(idx),
                    shakeKey = shakeKey,
                    enabled = enabled && stateOf(idx) == OptVisual.NORMAL,
                    onPick = { onPick(idx) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** 选项行：按皮肤渲染容器与字母块 */
@Composable
private fun OptionRow(
    label: String,
    text: String,
    visual: OptVisual,
    shakeKey: String,
    enabled: Boolean,
    onPick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val px = LocalPx.current
    val accent = when (visual) {
        OptVisual.CORRECT -> px.ok
        OptVisual.WRONG -> px.bad
        OptVisual.PICKED -> px.accent
        else -> px.border
    }
    val bg = when (visual) {
        OptVisual.CORRECT -> px.ok.copy(alpha = 0.14f)
        OptVisual.WRONG -> px.bad.copy(alpha = 0.14f)
        OptVisual.PICKED -> px.accent.copy(alpha = 0.14f)
        else -> px.surface
    }
    val shape = when (px.skin) {
        com.km1quiz.app.ui.theme.Skin.PIXEL -> androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
        com.km1quiz.app.ui.theme.Skin.PAPER -> androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
        else -> androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    }
    val shake = if (visual == OptVisual.WRONG) {
        Modifier.shakeOn(shakeKey)
    } else {
        Modifier
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .then(shake)
            .fillMaxWidth()
            .alpha(if (visual == OptVisual.DIM) 0.5f else 1f)
            .background(bg, shape)
            .then(
                when (px.skin) {
                    com.km1quiz.app.ui.theme.Skin.PIXEL -> Modifier.border(2.dp, accent, shape)
                    com.km1quiz.app.ui.theme.Skin.PAPER -> Modifier.border(1.dp, accent, shape)
                    com.km1quiz.app.ui.theme.Skin.GLASS -> Modifier.border(1.dp, accent.copy(alpha = 0.5f), shape)
                    else -> Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled
            ) { onPick() }
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        // 字母块：像素=键帽，其余=圆形
        KeycapLetter(
            label = label,
            bgColor = when (visual) {
                OptVisual.CORRECT -> px.ok
                OptVisual.WRONG -> px.bad
                OptVisual.PICKED -> px.accent
                else -> px.surfaceAlt
            },
            textColor = when (visual) {
                OptVisual.CORRECT, OptVisual.WRONG -> Color.White
                OptVisual.PICKED -> px.accentFg
                else -> px.text
            },
            edgeColor = accent,
            px = px
        )
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        AnimatedVisibility(
            visible = visual == OptVisual.CORRECT || visual == OptVisual.WRONG,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 500f))
        ) {
            PixelIcon(
                if (visual == OptVisual.CORRECT) Glyphs.check else Glyphs.close,
                if (visual == OptVisual.CORRECT) px.ok else px.bad,
                Modifier.size(22.dp, 22.dp),
                scale = 1.8.dp
            )
        }
    }
}
