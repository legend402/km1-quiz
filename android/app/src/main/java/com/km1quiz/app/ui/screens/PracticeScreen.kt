package com.km1quiz.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.km1quiz.app.data.Question
import com.km1quiz.app.data.QType
import com.km1quiz.app.logic.PracticeMode
import com.km1quiz.app.ui.PracticeUi
import com.km1quiz.app.ui.QuizViewModel
import com.km1quiz.app.ui.QuestionImage
import com.km1quiz.app.ui.pixel.Glyphs
import com.km1quiz.app.ui.pixel.PixelBlockBar
import com.km1quiz.app.ui.pixel.PixelButton
import com.km1quiz.app.ui.pixel.PixelCard
import com.km1quiz.app.ui.pixel.PixelChip
import com.km1quiz.app.ui.pixel.PixelIcon
import com.km1quiz.app.ui.pixel.PixelIconButton
import com.km1quiz.app.ui.pixel.PixelTextButton
import com.km1quiz.app.ui.pixel.shakeOn
import com.km1quiz.app.ui.rememberHaptics
import com.km1quiz.app.ui.theme.LocalPx

private val MODE_NAMES = mapOf(
    PracticeMode.SEQUENTIAL to "顺序练习",
    PracticeMode.RANDOM to "随机练习",
    PracticeMode.WRONG to "错题重练",
    PracticeMode.STARRED to "收藏夹"
)
private val OPTION_LABELS = listOf("A", "B", "C", "D")

/** 练习屏 v2：扁平选项 + 键帽字母块 + 彩边解析 + 非对称导航 */
@Composable
fun PracticeScreen(vm: QuizViewModel, ui: PracticeUi) {
    val px = LocalPx.current
    val haptics = rememberHaptics()

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // ---------- 顶栏 ----------
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(MODE_NAMES[ui.mode] ?: "练习", style = MaterialTheme.typography.headlineMedium)
                if (ui.mode == PracticeMode.WRONG) {
                    Text("答对自动移出错题本", style = MaterialTheme.typography.labelMedium, color = px.textDim)
                }
            }
            StarButton(starred = ui.starred) {
                haptics(true)
                vm.toggleStar()
            }
            Spacer(Modifier.width(8.dp))
            PixelIconButton(Glyphs.close, px, px.textDim, { vm.exitToHome() })
        }

        // ---------- 进度 ----------
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (ui.done) "本组完成" else "第 ${ui.index + 1} / ${ui.size} 题",
                style = MaterialTheme.typography.bodyMedium,
                color = px.textDim
            )
            PixelChip(
                if (ui.q?.type == QType.JUDGE) "判断题" else "单选题",
                px,
                if (ui.q?.type == QType.JUDGE) px.cyan else px.accent
            )
        }
        Spacer(Modifier.height(6.dp))
        PixelBlockBar(
            progress = if (ui.size > 0) (ui.index + if (ui.done) 0 else 1).toFloat() / ui.size else 0f,
            px = px,
            modifier = Modifier.fillMaxWidth(),
            blockH = 10.dp
        )
        Spacer(Modifier.height(12.dp))

        if (ui.done || ui.q == null) {
            DonePanel(vm, ui)
            return@Column
        }

        val q = ui.q

        // ---------- 题目卡片（方向性滑动 + 答错抖动） ----------
        AnimatedContent(
            targetState = ui.index,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(240)) { dir * it / 3 } + fadeIn(tween(240))) togetherWith
                    (slideOutHorizontally(tween(200)) { -dir * it / 3 } + fadeOut(tween(140)))
            },
            label = "qSlide"
        ) { _ ->
            val cardMod = if (ui.picked != null && ui.correct == false) {
                Modifier.shakeOn("${ui.index}:${ui.picked}")
            } else {
                Modifier
            }
            PixelCard(px = px, modifier = cardMod, contentPadding = 16.dp) {
                QuestionImage(q.image, px)
                Text(
                    q.question,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = if (q.image != null) 6.dp else 0.dp)
                )
            }
        }

        // ---------- 选项（判断题并排半宽，其他纵向排布） ----------
        OptionsArea(
            q = q,
            stateOf = { idx ->
                when {
                    ui.picked == null -> OptVisual.NORMAL
                    idx == q.answer -> OptVisual.CORRECT
                    idx == ui.picked -> OptVisual.WRONG
                    else -> OptVisual.DIM
                }
            },
            shakeKey = "${ui.index}:${ui.picked}",
            enabled = ui.picked == null,
            onPick = { idx ->
                haptics(idx == q.answer)
                vm.practicePick(idx)
            }
        )

        // ---------- 解析（彩边 + 展开） ----------
        AnimatedVisibility(
            visible = ui.picked != null,
            enter = expandVertically(tween(260)) + fadeIn(tween(260))
        ) {
            Column(Modifier.fillMaxWidth()) { ExplainPanel(q = q, correct = ui.correct == true) }
        }

        // ---------- 非对称导航：方块上一题 + 宽主按钮下一题 ----------
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelIconButton(
                glyph = Glyphs.arrowLeft,
                px = px,
                tint = px.text,
                onClick = { vm.practiceStep(-1) },
                size = 52.dp
            )
            Spacer(Modifier.width(10.dp))
            PixelTextButton(
                text = if (ui.index >= ui.size - 1) "完成" else "下一题",
                px = px,
                onClick = { vm.practiceStep(1) },
                enabled = ui.picked != null || ui.index < ui.size - 1,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** 解析面板 v2：左彩边 + 标题行 + 正文 + 法条（IntrinsicSize 让彩边跟随内容高度） */
@Composable
private fun ExplainPanel(q: Question, correct: Boolean) {
    val px = LocalPx.current
    val edge = if (correct) px.ok else px.bad
    Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
        val edgeShape = if (px.skin == com.km1quiz.app.ui.theme.Skin.PIXEL) {
            androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
        } else {
            androidx.compose.foundation.shape.RoundedCornerShape(3.dp)
        }
        Box(Modifier.width(5.dp).fillMaxHeight().background(edge, edgeShape))
        Column(Modifier.weight(1f).padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelIcon(
                    if (correct) Glyphs.check else Glyphs.close,
                    edge,
                    Modifier.size(20.dp, 20.dp),
                    scale = 1.6.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (correct) "回答正确" else "正确答案 ${OPTION_LABELS.getOrElse(q.answer) { "?" }}",
                    style = MaterialTheme.typography.titleMedium,
                    color = edge
                )
            }
            if (q.explain.isNotBlank()) {
                Text(q.explain, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            }
            if (q.law.isNotBlank()) {
                Text(
                    q.law,
                    style = MaterialTheme.typography.labelMedium,
                    color = px.textDim,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun StarButton(starred: Boolean, onClick: () -> Unit) {
    val px = LocalPx.current
    val scale by animateFloatAsState(
        targetValue = if (starred) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "starScale"
    )
    PixelButton(px = px, onClick = onClick, bg = px.surfaceAlt) {
        PixelIcon(
            Glyphs.star,
            if (starred) px.warn else px.textDim,
            Modifier.padding(9.dp).size(24.dp, 24.dp).scale(scale)
        )
    }
}

/** 完成态 */
@Composable
private fun DonePanel(vm: QuizViewModel, ui: PracticeUi) {
    val px = LocalPx.current
    val wrongLeft = vm.homeUi.wrongCount
    Column(
        Modifier.fillMaxWidth().padding(vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PixelIcon(Glyphs.check, px.ok, Modifier.size(56.dp, 56.dp), scale = 4.6.dp)
        Spacer(Modifier.height(12.dp))
        Text(
            when {
                ui.mode == PracticeMode.WRONG && wrongLeft == 0 -> "错题本已清空"
                ui.mode == PracticeMode.WRONG -> "本轮重练完成，还剩 $wrongLeft 道错题"
                else -> "本组题目已完成"
            },
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (ui.mode == PracticeMode.WRONG && wrongLeft > 0) {
                PixelTextButton("再来一轮", px, { vm.startPractice(PracticeMode.WRONG) })
            }
            PixelTextButton(
                "返回首页", px, { vm.exitToHome() },
                bg = px.surfaceAlt, fg = px.text
            )
        }
    }
}
