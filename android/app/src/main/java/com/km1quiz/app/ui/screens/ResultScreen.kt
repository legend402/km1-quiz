package com.km1quiz.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.km1quiz.app.logic.QuizEngine
import com.km1quiz.app.ui.QuizViewModel
import com.km1quiz.app.ui.ResultUi
import com.km1quiz.app.ui.fmtTime
import com.km1quiz.app.ui.pixel.CountUpText
import com.km1quiz.app.ui.pixel.PixelButton
import com.km1quiz.app.ui.pixel.PixelCard
import com.km1quiz.app.ui.pixel.PixelConfetti
import com.km1quiz.app.ui.pixel.PixelTextButton
import com.km1quiz.app.ui.pixel.StaggerIn
import com.km1quiz.app.ui.theme.LocalPx

/** 成绩页：分数累加 + 及格撒纸屑/盖章 + 错题回顾 */
@Composable
fun ResultScreen(vm: QuizViewModel, ui: ResultUi) {
    val px = LocalPx.current
    val passed = ui.score >= QuizEngine.PASS_SCORE

    Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
        ) {
            Spacer(Modifier.height(18.dp))

            // 分数 + 印章
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        CountUpText(
                            target = ui.score,
                            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (passed) px.ok else px.bad,
                            durationMs = 1100
                        )
                        Text(
                            " 分",
                            style = MaterialTheme.typography.headlineMedium,
                            color = px.textDim,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Text(
                        if (passed) "恭喜及格" else "未及格，继续加油",
                        style = MaterialTheme.typography.bodyLarge,
                        color = px.textDim
                    )
                }
                PassStamp(passed = passed)
            }

            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("及格线 ${QuizEngine.PASS_SCORE} 分", style = MaterialTheme.typography.labelMedium, color = px.textDim)
                Spacer(Modifier.size(14.dp, 6.dp))
                Text(
                    "对 ${ui.score} 错 ${ui.total - ui.score}",
                    style = MaterialTheme.typography.labelMedium,
                    color = px.textDim
                )
                Spacer(Modifier.size(14.dp, 6.dp))
                Text("用时 ${fmtTime(ui.durationSec)}", style = MaterialTheme.typography.labelMedium, color = px.textDim)
            }

            Spacer(Modifier.height(16.dp))
            if (ui.wrongList.isEmpty()) {
                PixelCard(px = px) {
                    Text("✓ 全对，太强了！", style = MaterialTheme.typography.titleMedium, color = px.ok)
                }
            } else {
                Text(
                    "错题回顾（已自动加入错题本）",
                    style = MaterialTheme.typography.labelMedium,
                    color = px.textDim
                )
                Spacer(Modifier.height(8.dp))
                ui.wrongList.forEachIndexed { i, w ->
                    StaggerIn(i.coerceAtMost(8)) {
                        ReviewItem(index = i, questionText = w.q.question, yourPick = w.picked, q = w.q)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(14.dp))
            PixelTextButton("返回首页", px, { vm.exitToHome() }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
        }

        // 及格：像素五彩纸屑（覆盖层，不挡点击）
        if (passed) {
            PixelConfetti(
                colors = listOf(px.accent, px.ok, px.cyan, px.bad, Color(0xFFB98CF5)),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** PASS / RETRY 印章：弹簧缩放砸下 + 轻微旋转 */
@Composable
private fun PassStamp(passed: Boolean) {
    val px = LocalPx.current
    val appear = remember { Animatable(2.6f) }
    LaunchedEffect(passed) {
        appear.snapTo(2.6f)
        appear.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 260f))
    }
    Box(
        Modifier
            .rotate(-11f)
            .scale(appear.value)
            .background(Color.Transparent)
            .border(5.dp, if (passed) px.ok else px.bad)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            if (passed) "PASS" else "RETRY",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            color = if (passed) px.ok else px.bad
        )
    }
}

@Composable
private fun ReviewItem(index: Int, questionText: String, yourPick: Int, q: com.km1quiz.app.data.Question) {
    val px = LocalPx.current
    val labels = listOf("A", "B", "C", "D")
    PixelCard(px = px, shadow = 3.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Text("✗", style = MaterialTheme.typography.titleMedium, color = px.bad)
            Spacer(Modifier.size(8.dp, 0.dp))
            Text(questionText, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "你的答案：${if (yourPick >= 0) "${labels.getOrElse(yourPick) { "?" }} ${q.options.getOrNull(yourPick) ?: ""}" else "未作答"}",
            style = MaterialTheme.typography.bodyMedium,
            color = px.bad
        )
        Text(
            "正确答案：${labels.getOrElse(q.answer) { "?" }} ${q.options.getOrNull(q.answer) ?: ""}",
            style = MaterialTheme.typography.bodyMedium,
            color = px.ok
        )
        if (q.explain.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                q.explain,
                style = MaterialTheme.typography.labelMedium,
                color = px.textDim,
                modifier = Modifier
                    .background(px.surfaceAlt)
                    .padding(6.dp)
            )
        }
    }
}
