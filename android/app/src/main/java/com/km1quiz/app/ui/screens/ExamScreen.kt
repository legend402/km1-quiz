package com.km1quiz.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.km1quiz.app.data.QType
import com.km1quiz.app.ui.ExamUi
import com.km1quiz.app.ui.QuizViewModel
import com.km1quiz.app.ui.QuestionImage
import com.km1quiz.app.ui.fmtTime
import com.km1quiz.app.ui.pixel.Glyphs
import com.km1quiz.app.ui.pixel.PixelBlockBar
import com.km1quiz.app.ui.pixel.PixelButton
import com.km1quiz.app.ui.pixel.PixelCard
import com.km1quiz.app.ui.pixel.PixelChip
import com.km1quiz.app.ui.pixel.PixelDialog
import com.km1quiz.app.ui.pixel.PixelIcon
import com.km1quiz.app.ui.pixel.PixelIconButton
import com.km1quiz.app.ui.pixel.PixelTextButton
import com.km1quiz.app.ui.rememberHaptics
import com.km1quiz.app.ui.theme.LocalPx

/**
 * 模拟考试屏 v2
 * 布局：标题+计时+交卷 → 进度与答题卡入口 → 题目 → 选项（判断题并排）→ 上下题
 * 答题卡不再铺满页面，收进带图例的弹窗。
 */
@Composable
fun ExamScreen(vm: QuizViewModel, ui: ExamUi) {
    val px = LocalPx.current
    val haptics = rememberHaptics()
    var showSheet by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // 返回键：确认层 → 答题卡 → 退出弹窗
    BackHandler(enabled = true) {
        when {
            ui.confirming -> vm.cancelConfirm()
            showSheet -> { showSheet = false }
            showExitDialog -> { showExitDialog = false }
            else -> showExitDialog = true
        }
    }

    Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ---------- 顶栏：标题 + 计时 + 交卷 ----------
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("模拟考试", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                TimerText(remainSec = ui.remainSec)
                Spacer(Modifier.width(8.dp))
                PixelTextButton("交卷", px, { vm.submitExam(false) }, bg = px.bad, fg = Color.White)
            }

            // ---------- 进度行 + 答题卡入口 ----------
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "第 ${ui.index + 1} / ${ui.size} 题 · 已答 ${ui.answered}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = px.textDim
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelChip(
                        if (ui.q?.type == QType.JUDGE) "判断题" else "单选题",
                        px,
                        if (ui.q?.type == QType.JUDGE) px.cyan else px.accent
                    )
                    Spacer(Modifier.width(8.dp))
                    PixelButton(px = px, onClick = { showSheet = true }, bg = px.surfaceAlt) {
                        PixelIcon(Glyphs.list, px.cyan, Modifier.padding(8.dp).size(20.dp, 20.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "答题卡",
                            style = MaterialTheme.typography.labelMedium,
                            color = px.text,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            PixelBlockBar(
                progress = if (ui.size > 0) (ui.index + 1).toFloat() / ui.size else 0f,
                px = px,
                modifier = Modifier.fillMaxWidth(),
                blockH = 10.dp
            )
            Spacer(Modifier.height(12.dp))

            val q = ui.q ?: return@Column

            // ---------- 题目卡片 ----------
            AnimatedContent(
                targetState = ui.index,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(240)) { dir * it / 3 } + fadeIn(tween(240))) togetherWith
                        (slideOutHorizontally(tween(200)) { -dir * it / 3 } + fadeOut(tween(140)))
                },
                label = "examSlide"
            ) { _ ->
                PixelCard(px = px, contentPadding = 16.dp) {
                    QuestionImage(q.image, px)
                    Text(
                        q.question,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = if (q.image != null) 6.dp else 0.dp)
                    )
                }
            }

            // ---------- 选项（判断题并排） ----------
            OptionsArea(
                q = q,
                stateOf = { idx -> if (ui.picked == idx) OptVisual.PICKED else OptVisual.NORMAL },
                shakeKey = "exam",
                onPick = { idx ->
                    haptics(true)
                    vm.examPick(idx)
                }
            )

            // ---------- 上下题 ----------
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PixelIconButton(
                    glyph = Glyphs.arrowLeft,
                    px = px,
                    tint = if (ui.index > 0) px.text else px.textDim,
                    onClick = { vm.examGoto(ui.index - 1) },
                    size = 52.dp
                )
                Spacer(Modifier.width(10.dp))
                PixelTextButton(
                    text = "下一题",
                    px = px,
                    onClick = { vm.examGoto(ui.index + 1) },
                    enabled = ui.index < ui.size - 1,
                    bg = px.cyan,
                    fg = px.accentFg,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ---------- 答题卡弹窗 ----------
        if (showSheet) {
            PixelDialog(
                px = px,
                onDismiss = { showSheet = false },
                title = "答题卡 · 已答 ${ui.answered}/${ui.size}"
            ) {
                // 图例
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SheetLegend(px.accent, "已答", px)
                    SheetLegend(px.surfaceAlt, "未答", px)
                    SheetLegend(px.cyan, "当前", px)
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 430.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 8.dp)
                ) {
                    SheetGrid(
                        picked = ui.sheetPicked,
                        current = ui.index,
                        onGoto = { i ->
                            vm.examGoto(i)
                            showSheet = false
                        }
                    )
                }
            }
        }

        // ---------- 交卷确认弹窗 ----------
        if (ui.confirming) {
            PixelDialog(
                px = px,
                onDismiss = { vm.cancelConfirm() },
                title = "还有 ${ui.size - ui.answered} 题未作答"
            ) {
                Text("确认交卷？交卷后立即评分。", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(14.dp))
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

        // ---------- 退出考试弹窗 ----------
        if (showExitDialog) {
            PixelDialog(
                px = px,
                onDismiss = { showExitDialog = false },
                title = "退出考试？"
            ) {
                Text("本次作答将不作数。", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(14.dp))
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
        initialValue = 1f, targetValue = if (urgent) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "timerPulse"
    )
    Box(
        Modifier
            .scale(pulse)
            .background(if (urgent) px.bad.copy(alpha = 0.16f) else px.surfaceAlt)
            .border(2.dp, if (urgent) px.bad else px.border)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            fmtTime(remainSec),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (urgent) px.bad else px.text
        )
    }
}

/** 答题卡图例：色块 + 文字 */
@Composable
private fun SheetLegend(color: Color, label: String, px: com.km1quiz.app.ui.theme.Px) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(14.dp).background(color).border(2.dp, px.border))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = px.textDim)
    }
}

/** 答题卡网格：6 列，已答填充、当前描边加粗、点击跳题 */
@Composable
private fun SheetGrid(picked: List<Int>, current: Int, onGoto: (Int) -> Unit) {
    val px = LocalPx.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        picked.chunked(6).forEachIndexed { rowIdx, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEachIndexed { colIdx, p ->
                    val i = rowIdx * 6 + colIdx
                    val done = p >= 0
                    val isCurrent = i == current
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(if (done) px.accent else px.surfaceAlt)
                            .border(
                                width = if (isCurrent) 4.dp else 2.dp,
                                color = if (isCurrent) px.cyan else px.border
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
