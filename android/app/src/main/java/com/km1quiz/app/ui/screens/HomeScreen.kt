package com.km1quiz.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.km1quiz.app.logic.PracticeMode
import com.km1quiz.app.logic.QuizEngine
import com.km1quiz.app.ui.QuizViewModel
import com.km1quiz.app.ui.pixel.Glyphs
import com.km1quiz.app.ui.pixel.CountUpText
import com.km1quiz.app.ui.pixel.PixelBlockBar
import com.km1quiz.app.ui.pixel.PixelButton
import com.km1quiz.app.ui.pixel.PixelCarDrive
import com.km1quiz.app.ui.pixel.PixelCard
import com.km1quiz.app.ui.pixel.PixelIcon
import com.km1quiz.app.ui.pixel.StaggerIn
import com.km1quiz.app.ui.theme.LocalPx

/** 首页：统计 + 五个入口 + 行驶的像素小汽车 */
@Composable
fun HomeScreen(vm: QuizViewModel) {
    val px = LocalPx.current
    val h = vm.homeUi

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelIcon(Glyphs.paperCheck, px.accent, Modifier.size(26.dp, 26.dp))
            Spacer(Modifier.width(8.dp))
            Text("科目一刷题", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))
            PixelButton(px = px, onClick = { vm.toggleSound() }, shadow = 3.dp) {
                PixelIcon(
                    if (vm.soundEnabled) Glyphs.speakerOn else Glyphs.speakerOff,
                    if (vm.soundEnabled) px.text else px.textDim,
                    Modifier.padding(5.dp).size(24.dp, 24.dp)
                )
            }
        }

        // 学习进度
        StaggerIn(0) {
            PixelCard(px = px) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("学习进度", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${h.answered} / ${h.total}",
                        style = MaterialTheme.typography.titleMedium,
                        color = px.accent
                    )
                }
                Spacer(Modifier.height(8.dp))
                PixelBlockBar(
                    progress = if (h.total > 0) h.answered.toFloat() / h.total else 0f,
                    px = px,
                    modifier = Modifier.fillMaxWidth(),
                    blocks = 16
                )
            }
        }

        // 三格统计
        StaggerIn(1) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCell("正确率", h.accuracy, "%", px.ok, Modifier.weight(1f))
                StatCell("错题", h.wrongCount, "", px.bad, Modifier.weight(1f))
                StatCell("收藏", h.starredCount, "", px.warn, Modifier.weight(1f))
            }
        }

        // 最近模拟
        StaggerIn(2) {
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                val last = h.lastExam
                Text("最近模拟：", style = MaterialTheme.typography.bodyMedium, color = px.textDim)
                if (last == null) {
                    Text("未测试", style = MaterialTheme.typography.bodyMedium, color = px.textDim)
                } else {
                    val passed = last.score >= QuizEngine.PASS_SCORE
                    Text(
                        "${last.score}分 ${if (passed) "及格" else "不及格"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (passed) px.ok else px.bad
                    )
                }
            }
        }

        // 五个入口（模拟考试主色突出，其余次级色）
        MenuEntry(3, Glyphs.list, px.cyan, "顺序练习", "按顺序过一遍全库", 0) { vm.startPractice(PracticeMode.SEQUENTIAL) }
        MenuEntry(4, Glyphs.dice, px.accent, "随机练习", "随机打乱刷题", 0) { vm.startPractice(PracticeMode.RANDOM) }
        MenuEntry(5, Glyphs.paperCheck, px.ok, "模拟考试", "100题 / 45分钟 / 90分及格", 0, highlight = true) { vm.startExam() }
        MenuEntry(6, Glyphs.xCircle, px.bad, "错题重练", "答对自动移出错题本", h.wrongCount) { vm.startPractice(PracticeMode.WRONG) }
        MenuEntry(7, Glyphs.star, px.warn, "收藏夹", "练习中点击星标收藏", h.starredCount) { vm.startPractice(PracticeMode.STARRED) }

        Spacer(Modifier.height(6.dp))
        Text(
            "题库 ${vm.bankSize} 题 · 2026-07 更新 · 完全离线",
            style = MaterialTheme.typography.labelMedium,
            color = px.textDim,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        // 吉祥物压轴
        PixelCarDrive(
            carColor = px.accent,
            windowColor = px.cyan,
            wheelColor = px.text,
            roadColor = px.border,
            headlightColor = Color(0xFFFFF3B0),
            modifier = Modifier.fillMaxWidth().height(70.dp).padding(bottom = 18.dp)
        )
    }
}

@Composable
private fun StatCell(label: String, value: Int, suffix: String, color: Color, modifier: Modifier = Modifier) {
    val px = LocalPx.current
    PixelCard(px = px, modifier = modifier, shadow = 3.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            CountUpText(value, MaterialTheme.typography.headlineMedium, suffix = suffix, color = color)
            Text(label, style = MaterialTheme.typography.labelMedium, color = px.textDim)
        }
    }
}

@Composable
private fun MenuEntry(
    stagger: Int,
    glyph: List<String>,
    tint: Color,
    name: String,
    desc: String,
    badge: Int,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    val px = LocalPx.current
    StaggerIn(stagger) {
        PixelButton(
            px = px,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            bg = if (highlight) px.accent else px.surfaceAlt
        ) {
            PixelIcon(glyph, tint, Modifier.size(24.dp, 24.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.padding(vertical = 8.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (highlight) px.accentFg else px.text
                    )
                    if (badge > 0) {
                        Spacer(Modifier.width(8.dp))
                        com.km1quiz.app.ui.pixel.PixelBadge(badge, px)
                    }
                }
                Text(
                    desc,
                    style = MaterialTheme.typography.labelMedium,
                    color = (if (highlight) px.accentFg else px.textDim).copy(alpha = 0.85f)
                )
            }
        }
    }
}
