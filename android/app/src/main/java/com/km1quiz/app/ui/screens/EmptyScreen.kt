package com.km1quiz.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.km1quiz.app.logic.EmptyReason
import com.km1quiz.app.ui.QuizViewModel
import com.km1quiz.app.ui.pixel.Glyphs
import com.km1quiz.app.ui.pixel.PixelIcon
import com.km1quiz.app.ui.pixel.PixelTextButton
import com.km1quiz.app.ui.theme.LocalPx
import kotlin.math.roundToInt

/** 空态页：像素图标上下漂浮（错题本空 / 无收藏 / 题量不足） */
@Composable
fun EmptyScreen(vm: QuizViewModel, reason: EmptyReason?) {
    val px = LocalPx.current
    val t = rememberInfiniteTransition(label = "float")
    val float by t.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "bob"
    )

    val (icon, title, desc) = when (reason) {
        EmptyReason.STARRED_EMPTY -> Triple(Glyphs.star, "还没有收藏题目", "练习时点击顶部星标即可收藏")
        EmptyReason.EXAM_SHORT -> Triple(Glyphs.paperCheck, "题库不足", "题库题目数量不足以组成模拟卷")
        else -> Triple(Glyphs.xCircle, "错题本是空的", "练习或模拟考试中答错的题会自动收进来")
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 46.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PixelIcon(
            glyph = icon,
            color = px.textDim,
            modifier = Modifier
                .size(56.dp, 56.dp)
                .offset { IntOffset(0, (float * 2).roundToInt()) },
            scale = 4.dp
        )
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text(desc, style = MaterialTheme.typography.bodyMedium, color = px.textDim)
        Spacer(Modifier.height(18.dp))
        PixelTextButton(
            "返回首页", px, { vm.exitToHome() },
            bg = px.surfaceAlt, fg = px.text
        )
    }
}

/** 加载页：题库解码中（方向盘图标闪烁） */
@Composable
fun LoadingScreen() {
    val px = LocalPx.current
    val t = rememberInfiniteTransition(label = "load")
    val blink by t.animateFloat(
        initialValue = 1f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "blink"
    )
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PixelIcon(
            glyph = Glyphs.paperCheck,
            color = px.accent,
            modifier = Modifier
                .size(48.dp, 48.dp)
                .alpha(blink),
            scale = 4.dp
        )
        Spacer(Modifier.height(12.dp))
        Text("LOADING…", style = MaterialTheme.typography.titleMedium, color = px.textDim)
    }
}
