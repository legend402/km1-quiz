package com.km1quiz.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.width
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
import com.km1quiz.app.data.Question
import com.km1quiz.app.logic.QuizEngine
import com.km1quiz.app.ui.QuizViewModel
import com.km1quiz.app.ui.ResultUi
import com.km1quiz.app.ui.fmtTime
import com.km1quiz.app.ui.pixel.CountUpText
import com.km1quiz.app.ui.pixel.Glyphs
import com.km1quiz.app.ui.pixel.PixelCard
import com.km1quiz.app.ui.pixel.PixelChip
import com.km1quiz.app.ui.pixel.PixelConfetti
import com.km1quiz.app.ui.pixel.PixelDivider
import com.km1quiz.app.ui.pixel.PixelIcon
import com.km1quiz.app.ui.pixel.PixelTextButton
import com.km1quiz.app.ui.pixel.SectionTitle
import com.km1quiz.app.ui.pixel.StaggerIn
import com.km1quiz.app.ui.theme.LocalPx

/**
 * 成绩页 v2
 * Hero 卡片（分数+印章并置不遮挡）→ 数据 chips → 主操作上移首屏 → 错题回顾（扁平彩边卡）
 */
@Composable
fun ResultScreen(vm: QuizViewModel, ui: ResultUi) {
    val px = LocalPx.current
    val passed = ui.score >= QuizEngine.PASS_SCORE

    Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(14.dp))

            // ---------- Hero 成绩卡 ----------
            PixelCard(px = px, elevated = true, contentPadding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            CountUpText(
                                target = ui.score,
                                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (passed) px.ok else px.bad,
                                durationMs = 1100
                            )
                            Text(
                                " 分",
                                style = MaterialTheme.typography.titleMedium,
                                color = px.textDim,
                                modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
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
                Spacer(Modifier.height(14.dp))
                PixelDivider(px)
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PixelChip("对 ${ui.score}", px, px.ok)
                    PixelChip("错 ${ui.total - ui.score}", px, px.bad)
                    PixelChip("用时 ${fmtTime(ui.durationSec)}", px, px.cyan)
                    PixelChip("及格 ${QuizEngine.PASS_SCORE}", px, px.textDim)
                }
            }

            // ---------- 主操作（首屏可达） ----------
            Spacer(Modifier.height(14.dp))
            PixelTextButton(
                "返回首页", px, { vm.exitToHome() },
                bg = px.surfaceAlt, fg = px.text,
                modifier = Modifier.fillMaxWidth()
            )

            // ---------- 错题回顾 ----------
            Spacer(Modifier.height(16.dp))
            if (ui.wrongList.isEmpty()) {
                PixelCard(px = px) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelIcon(Glyphs.check, px.ok, Modifier.size(26.dp, 26.dp), scale = 2.2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("全对，太强了！", style = MaterialTheme.typography.titleMedium, color = px.ok)
                    }
                }
            } else {
                SectionTitle("错题回顾 · 已自动加入错题本", px, px.bad)
                Spacer(Modifier.height(10.dp))
                ui.wrongList.forEachIndexed { i, w ->
                    StaggerIn(i.coerceAtMost(8)) {
                        ReviewItem(yourPick = w.picked, q = w.q)
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // 及格：像素五彩纸屑
        if (passed) {
            PixelConfetti(
                colors = listOf(px.accent, px.ok, px.cyan, px.bad, Color(0xFFB98CF5)),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** PASS / RETRY 印章：弹簧砸下，立于分数右侧（不遮挡数字） */
@Composable
private fun PassStamp(passed: Boolean) {
    val px = LocalPx.current
    val appear = remember { Animatable(2.4f) }
    LaunchedEffect(passed) {
        appear.snapTo(2.4f)
        appear.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 260f))
    }
    Box(
        Modifier
            .rotate(-10f)
            .scale(appear.value)
            .border(4.dp, if (passed) px.ok else px.bad)
            .padding(horizontal = 10.dp, vertical = 2.dp)
    ) {
        Text(
            if (passed) "PASS" else "RETRY",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = if (passed) px.ok else px.bad
        )
    }
}

/** 错题卡 v2：扁平卡，答案行只给标签上色（避免"红字写正确"的语义打架） */
@Composable
private fun ReviewItem(yourPick: Int, q: Question) {
    val px = LocalPx.current
    val labels = listOf("A", "B", "C", "D")
    val yourText = if (yourPick >= 0) {
        "${labels.getOrElse(yourPick) { "?" }} ${q.options.getOrNull(yourPick) ?: ""}"
    } else {
        "未作答"
    }
    val rightText = "${labels.getOrElse(q.answer) { "?" }} ${q.options.getOrNull(q.answer) ?: ""}"

    PixelCard(px = px, contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.Top) {
            PixelIcon(Glyphs.close, px.bad, Modifier.size(20.dp, 20.dp), scale = 1.6.dp)
            Spacer(Modifier.width(8.dp))
            Text(q.question, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("你的答案", style = MaterialTheme.typography.labelMedium, color = px.bad)
            Spacer(Modifier.width(8.dp))
            Text(yourText, style = MaterialTheme.typography.bodyMedium, color = px.text)
        }
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("正确答案", style = MaterialTheme.typography.labelMedium, color = px.ok)
            Spacer(Modifier.width(8.dp))
            Text(rightText, style = MaterialTheme.typography.bodyMedium, color = px.text)
        }
        if (q.explain.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                q.explain,
                style = MaterialTheme.typography.labelMedium,
                color = px.textDim,
                modifier = Modifier.background(px.surfaceAlt).padding(8.dp)
            )
        }
    }
}
