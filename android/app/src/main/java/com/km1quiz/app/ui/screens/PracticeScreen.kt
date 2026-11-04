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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.km1quiz.app.data.Question
import com.km1quiz.app.logic.PracticeMode
import com.km1quiz.app.ui.PracticeUi
import com.km1quiz.app.ui.QuizViewModel
import com.km1quiz.app.ui.QuestionImage
import com.km1quiz.app.ui.pixel.Glyphs
import com.km1quiz.app.ui.pixel.PixelBlockBar
import com.km1quiz.app.ui.pixel.PixelButton
import com.km1quiz.app.ui.pixel.PixelCard
import com.km1quiz.app.ui.pixel.PixelIcon
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

/** 练习屏：即时判分 + 解析展开 + 收藏 + 错题移出 */
@Composable
fun PracticeScreen(vm: QuizViewModel, ui: PracticeUi) {
    val px = LocalPx.current
    val haptics = rememberHaptics()

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp)
    ) {
        // 顶栏
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(MODE_NAMES[ui.mode] ?: "练习", style = MaterialTheme.typography.titleMedium)
                if (ui.mode == PracticeMode.WRONG) {
                    Text("答对自动移出", style = MaterialTheme.typography.labelMedium, color = px.textDim)
                }
            }
            StarButton(starred = ui.starred) {
                haptics(true)
                vm.toggleStar()
            }
            Spacer(Modifier.width(8.dp))
            PixelButton(px = px, onClick = { vm.exitToHome() }, shadow = 3.dp) {
                PixelIcon(Glyphs.close, px.textDim, Modifier.padding(6.dp).size(22.dp, 22.dp))
            }
        }

        // 进度
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (ui.done) "本组完成" else "第 ${ui.index + 1} / ${ui.size} 题",
                style = MaterialTheme.typography.bodyMedium,
                color = px.textDim
            )
            TypeBadge(ui.q)
        }
        Spacer(Modifier.height(6.dp))
        PixelBlockBar(
            progress = if (ui.size > 0) (ui.index + if (ui.done) 0 else 1).toFloat() / ui.size else 0f,
            px = px,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))

        if (ui.done || ui.q == null) {
            DonePanel(vm, ui)
        } else {
            val q = ui.q
            // 题目卡片（切题方向性滑动）
            AnimatedContent(
                targetState = ui.index,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(240)) { dir * it / 3 } + fadeIn(tween(240))) togetherWith
                        (slideOutHorizontally(tween(200)) { -dir * it / 3 } + fadeOut(tween(140)))
                },
                label = "qSlide"
            ) { idx ->
                val cardMod = if (ui.picked != null && ui.correct == false) {
                    Modifier.shakeOn("${ui.index}:${ui.picked}")
                } else {
                    Modifier
                }
                PixelCard(px = px, modifier = cardMod) {
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
                    OptionRow(
                        label = OPTION_LABELS.getOrElse(idx) { "?" },
                        text = opt,
                        state = optionState(ui, idx, q),
                        shakeKey = "${ui.index}:${ui.picked}",
                        onClick = {
                            if (ui.picked == null) {
                                haptics(idx == q.answer)
                                vm.practicePick(idx)
                            }
                        }
                    )
                }
            }

            // 解析
            AnimatedVisibility(
                visible = ui.picked != null,
                enter = expandVertically(tween(260)) + fadeIn(tween(260))
            ) {
                Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    ExplainPanel(q = q, correct = ui.correct == true, picked = ui.picked)
                }
            }

            // 上下题
            Row(
                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PixelTextButton(
                    text = "上一题",
                    px = px,
                    onClick = { vm.practiceStep(-1) },
                    enabled = ui.index > 0,
                    bg = px.surfaceAlt,
                    fg = px.text,
                    modifier = Modifier.weight(1f)
                )
                PixelTextButton(
                    text = if (ui.index >= ui.size - 1) "完成" else "下一题",
                    px = px,
                    onClick = { vm.practiceStep(1) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private enum class OptState { NORMAL, CORRECT, WRONG, DIM }

private fun optionState(ui: PracticeUi, idx: Int, q: Question): OptState = when {
    ui.picked == null -> OptState.NORMAL
    idx == q.answer -> OptState.CORRECT
    idx == ui.picked -> OptState.WRONG
    else -> OptState.DIM
}

@Composable
private fun OptionRow(label: String, text: String, state: OptState, shakeKey: String, onClick: () -> Unit) {
    val px = LocalPx.current
    val borderColor = when (state) {
        OptState.CORRECT -> px.ok
        OptState.WRONG -> px.bad
        else -> px.border
    }
    val bg = when (state) {
        OptState.CORRECT -> px.ok.copy(alpha = 0.14f)
        OptState.WRONG -> px.bad.copy(alpha = 0.14f)
        else -> px.surface
    }
    val rowMod = if (state == OptState.WRONG) {
        Modifier
            .fillMaxWidth()
            .shakeOn(shakeKey)
    } else {
        Modifier.fillMaxWidth()
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = rowMod
            .alpha(if (state == OptState.DIM) 0.55f else 1f)
            .background(bg)
            .border(2.dp, borderColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = state == OptState.NORMAL
            ) { onClick() }
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        // 字母块
        Box(
            Modifier
                .size(24.dp)
                .background(if (state == OptState.CORRECT) px.ok else if (state == OptState.WRONG) px.bad else px.surfaceAlt)
                .border(2.dp, borderColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (state == OptState.CORRECT || state == OptState.WRONG) Color.White else px.text
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        // 对错图标弹出
        AnimatedVisibility(
            visible = state == OptState.CORRECT || state == OptState.WRONG,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 500f))
        ) {
            PixelIcon(
                if (state == OptState.CORRECT) Glyphs.check else Glyphs.close,
                if (state == OptState.CORRECT) px.ok else px.bad,
                Modifier.padding(start = 6.dp).size(24.dp, 24.dp),
                scale = 2.dp
            )
        }
    }
}

@Composable
private fun ExplainPanel(q: Question, correct: Boolean, picked: Int?) {
    val px = LocalPx.current
    PixelCard(px = px, bg = px.surfaceAlt, shadow = 3.dp) {
        Text(
            if (correct) "✓ 回答正确" else "✗ 回答错误，正确答案 ${OPTION_LABELS.getOrElse(q.answer) { "?" }}",
            style = MaterialTheme.typography.titleMedium,
            color = if (correct) px.ok else px.bad
        )
        if (q.explain.isNotBlank()) {
            Text(q.explain, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
        if (q.law.isNotBlank()) {
            Text(
                q.law,
                style = MaterialTheme.typography.labelMedium,
                color = px.textDim,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun StarButton(starred: Boolean, onClick: () -> Unit) {
    val px = LocalPx.current
    val scale by animateFloatAsState(
        targetValue = if (starred) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "starScale"
    )
    PixelButton(px = px, onClick = onClick, shadow = 3.dp) {
        PixelIcon(
            Glyphs.star,
            if (starred) px.warn else px.textDim,
            Modifier
                .padding(6.dp)
                .size(24.dp, 24.dp)
                .scale(scale)
        )
    }
}

@Composable
private fun TypeBadge(q: Question?) {
    val px = LocalPx.current
    Box(Modifier.background(px.surfaceAlt).border(2.dp, px.border).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(
            if (q?.type == com.km1quiz.app.data.QType.JUDGE) "判断题" else "单选题",
            style = MaterialTheme.typography.labelMedium,
            color = px.textDim
        )
    }
}

/** 完成态：错题重练给"再来一轮"，其余给"返回首页" */
@Composable
private fun DonePanel(vm: QuizViewModel, ui: PracticeUi) {
    val px = LocalPx.current
    val wrongLeft = vm.homeUi.wrongCount
    Column(
        Modifier.fillMaxWidth().padding(vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PixelIcon(Glyphs.check, px.ok, Modifier.size(48.dp, 48.dp), scale = 4.dp)
        Spacer(Modifier.height(10.dp))
        Text(
            when {
                ui.mode == PracticeMode.WRONG && wrongLeft == 0 -> "错题本已清空"
                ui.mode == PracticeMode.WRONG -> "本轮重练完成，还剩 $wrongLeft 道错题"
                else -> "本组题目已完成"
            },
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(14.dp))
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
