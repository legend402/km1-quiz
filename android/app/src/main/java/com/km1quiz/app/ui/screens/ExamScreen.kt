package com.km1quiz.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.km1quiz.app.data.Question
import com.km1quiz.app.ui.ExamUi
import com.km1quiz.app.ui.QuizViewModel
import com.km1quiz.app.ui.QuestionImage
import com.km1quiz.app.ui.fmtTime
import com.km1quiz.app.ui.pixel.Glyphs
import com.km1quiz.app.ui.pixel.PixelBlockBar
import com.km1quiz.app.ui.pixel.PixelButton
import com.km1quiz.app.ui.pixel.PixelCard
import com.km1quiz.app.ui.pixel.PixelDialog
import com.km1quiz.app.ui.pixel.PixelIcon
import com.km1quiz.app.ui.pixel.PixelTextButton
import com.km1quiz.app.ui.rememberHaptics
import com.km1quiz.app.ui.theme.LocalPx

private val OPTION_LABELS = listOf("A", "B", "C", "D")

/** 模拟考试屏：倒计时、答题卡跳题、交卷确认、返回键退出确认 */
@Composable
fun ExamScreen(vm: QuizViewModel, ui: ExamUi) {
    val px = LocalPx.current
    val haptics = rememberHaptics()
    var showExitDialog by remember { mutableStateOf(false) }

    // 返回键：先关确认层，再弹退出确认
    BackHandler(enabled = true) {
        when {
            ui.confirming -> vm.cancelConfirm()
            showExitDialog -> { showExitDialog = false }
            else -> showExitDialog = true
        }
    }

    Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
        ) {
            // 顶栏：倒计时 + 交卷
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PixelIcon(Glyphs.paperCheck, px.ok, Modifier.size(20.dp, 20.dp))
                Spacer(Modifier.width(6.dp))
                Text("模拟考试", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TimerText(remainSec = ui.remainSec)
                Spacer(Modifier.width(8.dp))
                PixelTextButton("交卷", px, { vm.submitExam(false) }, small = true, bg = px.bad, fg = Color.White)
            }

            // 进度
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "第 ${ui.index + 1} / ${ui.size} 题 · 已答 ${ui.answered}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = px.textDim
                )
                Box(Modifier.background(px.surfaceAlt).border(2.dp, px.border).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text(
                        if (ui.q?.type == com.km1quiz.app.data.QType.JUDGE) "判断题" else "单选题",
                        style = MaterialTheme.typography.labelMedium,
                        color = px.textDim
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            PixelBlockBar(
                progress = if (ui.size > 0) (ui.index + 1).toFloat() / ui.size else 0f,
                px = px,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            val q = ui.q ?: return@Column

            // 题目卡片（方向性滑动）
            AnimatedContent(
                targetState = ui.index,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(240)) { dir * it / 3 } + fadeIn(tween(240))) togetherWith
                        (slideOutHorizontally(tween(200)) { -dir * it / 3 } + fadeOut(tween(140)))
                },
                label = "examSlide"
            ) { _ ->
                PixelCard(px = px) {
                    QuestionImage(q.image, px)
                    Text(
                        q.question,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // 选项
            Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                q.options.forEachIndexed { idx, opt ->
                    Spacer(Modifier.height(6.dp))
                    ExamOptionRow(
                        label = OPTION_LABELS.getOrElse(idx) { "?" },
                        text = opt,
                        pickedNow = ui.picked == idx,
                        onClick = {
                            haptics(true)
                            vm.examPick(idx)
                        }
                    )
                }
            }

            // 上下题
            Row(
                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PixelTextButton(
                    "上一题", px, { vm.examGoto(ui.index - 1) },
                    enabled = ui.index > 0, bg = px.surfaceAlt, fg = px.text,
                    modifier = Modifier.weight(1f)
                )
                PixelTextButton(
                    "下一题", px, { vm.examGoto(ui.index + 1) },
                    enabled = ui.index < ui.size - 1, bg = px.surfaceAlt, fg = px.text,
                    modifier = Modifier.weight(1f)
                )
            }

            // 答题卡
            Text("答题卡（点击跳题）", style = MaterialTheme.typography.labelMedium, color = px.textDim)
            Spacer(Modifier.height(6.dp))
            AnswerSheet(
                picked = ui.sheetPicked,
                current = ui.index,
                onGoto = { vm.examGoto(it) }
            )
            Spacer(Modifier.height(24.dp))
        }

        // 交卷确认弹窗
        if (ui.confirming) {
            PixelDialog(
                px = px,
                onDismiss = { vm.cancelConfirm() },
                title = "还有 ${ui.size - ui.answered} 题未作答"
            ) {
                Text("确认交卷？交卷后立即评分。", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PixelTextButton(
                        "继续答题", px, { vm.cancelConfirm() },
                        bg = px.surfaceAlt, fg = px.text, modifier = Modifier.weight(1f)
                    )
                    PixelTextButton(
                        "确认交卷", px, { vm.submitExam(true) },
                        bg = px.bad, fg = Color.White, modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 退出考试确认弹窗
        if (showExitDialog) {
            PixelDialog(
                px = px,
                onDismiss = { showExitDialog = false },
                title = "退出考试？"
            ) {
                Text("本次作答将不作数。", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PixelTextButton(
                        "继续考试", px, { showExitDialog = false },
                        bg = px.surfaceAlt, fg = px.text, modifier = Modifier.weight(1f)
                    )
                    PixelTextButton(
                        "退出", px, {
                            showExitDialog = false
                            vm.quitExam()
                        },
                        bg = px.bad, fg = Color.White, modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** 倒计时：≤5 分钟脉冲变红 */
@Composable
private fun TimerText(remainSec: Int) {
    val px = LocalPx.current
    val urgent = remainSec in 1..300
    val t = rememberInfiniteTransition(label = "timer")
    val pulse by t.animateFloat(
        initialValue = 1f, targetValue = if (urgent) 1.15f else 1f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "timerPulse"
    )
    Box(
        Modifier
            .scale(pulse)
            .background(if (urgent) px.bad.copy(alpha = 0.16f) else px.surfaceAlt)
            .border(2.dp, if (urgent) px.bad else px.border)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            fmtTime(remainSec),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (urgent) px.bad else px.text
        )
    }
}

@Composable
private fun ExamOptionRow(label: String, text: String, pickedNow: Boolean, onClick: () -> Unit) {
    val px = LocalPx.current
    val borderColor = if (pickedNow) px.accent else px.border
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (pickedNow) px.accent.copy(alpha = 0.14f) else px.surface)
            .border(2.dp, borderColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = true
            ) { onClick() }
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Box(
            Modifier
                .size(24.dp)
                .background(if (pickedNow) px.accent else px.surfaceAlt)
                .border(2.dp, borderColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (pickedNow) px.accentFg else px.text
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

/** 答题卡：答过的格子弹一下，当前题描边高亮 */
@Composable
private fun AnswerSheet(picked: List<Int>, current: Int, onGoto: (Int) -> Unit) {
    val px = LocalPx.current
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        picked.chunked(6).forEachIndexed { rowIdx, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                row.forEachIndexed { colIdx, p ->
                    val i = rowIdx * 6 + colIdx
                    val done = p >= 0
                    val isCurrent = i == current
                    val scale by animateFloatAsState(
                        targetValue = if (done) 1f else 0.92f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "cell$i"
                    )
                    Box(
                        Modifier
                            .scale(scale)
                            .size(36.dp)
                            .background(if (done) px.accent else px.surfaceAlt)
                            .border(
                                width = if (isCurrent) 3.dp else 2.dp,
                                color = if (isCurrent) px.accent else px.border
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onGoto(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${i + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (done) px.accentFg else px.textDim
                        )
                    }
                }
            }
        }
    }
}
