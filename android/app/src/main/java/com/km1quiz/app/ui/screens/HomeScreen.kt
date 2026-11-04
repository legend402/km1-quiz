package com.km1quiz.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.km1quiz.app.logic.PracticeMode
import com.km1quiz.app.logic.QuizEngine
import com.km1quiz.app.ui.QuizViewModel
import com.km1quiz.app.ui.pixel.CountUpText
import com.km1quiz.app.ui.pixel.Glyphs
import com.km1quiz.app.ui.pixel.PixelBadge
import com.km1quiz.app.ui.pixel.PixelBlockBar
import com.km1quiz.app.ui.pixel.PixelButton
import com.km1quiz.app.ui.pixel.PixelCard
import com.km1quiz.app.ui.pixel.PixelCarDrive
import com.km1quiz.app.ui.pixel.PixelChip
import com.km1quiz.app.ui.pixel.PixelDialog
import com.km1quiz.app.ui.pixel.PixelDivider
import com.km1quiz.app.ui.pixel.PixelIcon
import com.km1quiz.app.ui.pixel.PixelQuads
import com.km1quiz.app.ui.pixel.SectionTitle
import com.km1quiz.app.ui.pixel.StaggerIn
import com.km1quiz.app.ui.theme.LocalPx
import kotlin.math.roundToInt

/**
 * 首页 v2：移动端原生结构
 * 头部 → 闯关进度 Hero（车在轨道上前进）→ 三格统计 → 2x2 模式宫格 → 模拟考试大 CTA
 */
@Composable
fun HomeScreen(vm: QuizViewModel) {
    val px = LocalPx.current
    val h = vm.homeUi
    val percent = if (h.total > 0) h.answered * 100 / h.total else 0
    var showStylePicker by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
        // ---------- 头部 ----------
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelIcon(Glyphs.paperCheck, px.accent, Modifier.size(30.dp, 30.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("科目一刷题", style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (px.skin == com.km1quiz.app.ui.theme.Skin.PIXEL) "驾照理论 · 像素修炼场" else "驾照理论 · 驾考修炼场",
                    style = MaterialTheme.typography.labelMedium,
                    color = px.textDim
                )
            }
            // 风格切换（2x2 四色块图标）
            PixelButton(px = px, onClick = { showStylePicker = true }, bg = px.surfaceAlt) {
                StyleQuads(Modifier.padding(9.dp).size(22.dp, 22.dp))
            }
            Spacer(Modifier.width(8.dp))
            PixelButton(px = px, onClick = { vm.toggleSound() }, bg = px.surfaceAlt) {
                PixelIcon(
                    if (vm.soundEnabled) Glyphs.speakerOn else Glyphs.speakerOff,
                    if (vm.soundEnabled) px.accent else px.textDim,
                    Modifier.padding(8.dp).size(22.dp, 22.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        PixelDivider(px)

        // ---------- 闯关进度 Hero ----------
        Spacer(Modifier.height(16.dp))
        StaggerIn(0) {
            PixelCard(px = px, elevated = true, contentPadding = 16.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("闯关进度", px)
                    Row(verticalAlignment = Alignment.Bottom) {
                        CountUpText(
                            percent,
                            style = MaterialTheme.typography.headlineMedium,
                            suffix = "%",
                            color = px.accent
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                ProgressRoad(progress = if (h.total > 0) h.answered.toFloat() / h.total else 0f)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("已刷 ${h.answered} / ${h.total} 题", style = MaterialTheme.typography.labelMedium, color = px.textDim)
                    Text("正确率 ${h.accuracy}%", style = MaterialTheme.typography.labelMedium, color = px.ok)
                }
            }
        }

        // ---------- 三格统计 ----------
        Spacer(Modifier.height(12.dp))
        StaggerIn(1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatChip("错题", h.wrongCount, px.bad, Modifier.weight(1f))
                StatChip("收藏", h.starredCount, px.warn, Modifier.weight(1f))
                StatChip("模拟", h.lastExam?.score ?: -1, px.cyan, Modifier.weight(1f))
            }
        }

        // ---------- 2x2 模式宫格 ----------
        Spacer(Modifier.height(12.dp))
        StaggerIn(2) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModeCard(Glyphs.list, px.cyan, "顺序练习", 0) { vm.startPractice(PracticeMode.SEQUENTIAL) }
                ModeCard(Glyphs.dice, px.accent, "随机练习", 0) { vm.startPractice(PracticeMode.RANDOM) }
            }
        }
        Spacer(Modifier.height(10.dp))
        StaggerIn(3) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModeCard(Glyphs.xCircle, px.bad, "错题重练", h.wrongCount) { vm.startPractice(PracticeMode.WRONG) }
                ModeCard(Glyphs.star, px.warn, "收藏夹", h.starredCount) { vm.startPractice(PracticeMode.STARRED) }
            }
        }

        // ---------- 模拟考试大 CTA ----------
        Spacer(Modifier.height(12.dp))
        StaggerIn(4) {
            PixelButton(
                px = px,
                onClick = { vm.startExam() },
                modifier = Modifier.fillMaxWidth(),
                elevated = true
            ) {
                PixelIcon(Glyphs.paperCheck, px.accentFg, Modifier.size(26.dp, 26.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                    Text("模拟考试", style = MaterialTheme.typography.titleMedium, color = px.accentFg)
                    Text(
                        "100 题 · 45 分钟 · 90 分及格",
                        style = MaterialTheme.typography.labelMedium,
                        color = px.accentFg.copy(alpha = 0.8f)
                    )
                }
                PixelIcon(Glyphs.arrowRight, px.accentFg, Modifier.size(22.dp, 22.dp))
            }
        }

        // ---------- 最近模拟 ----------
        Spacer(Modifier.height(14.dp))
        StaggerIn(5) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PixelQuads(px.cyan, Modifier.size(10.dp, 10.dp))
                Spacer(Modifier.width(6.dp))
                Text("最近模拟", style = MaterialTheme.typography.labelMedium, color = px.textDim)
                Spacer(Modifier.weight(1f))
                val last = h.lastExam
                if (last == null) {
                    Text("未测试", style = MaterialTheme.typography.labelMedium, color = px.textDim)
                } else {
                    val passed = last.score >= QuizEngine.PASS_SCORE
                    PixelChip(
                        "${last.score}分 ${if (passed) "及格" else "不及格"}",
                        px,
                        if (passed) px.ok else px.bad
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            "题库 ${vm.bankSize} 题 · 2026-07 更新 · 完全离线",
            style = MaterialTheme.typography.labelMedium,
            color = px.textDim,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
        )

        // ---------- 底部装饰带（仅像素皮肤；其余皮肤收起像素小车） ----------
        if (px.skin == com.km1quiz.app.ui.theme.Skin.PIXEL) {
            PixelCarDrive(
                carColor = px.accent,
                windowColor = px.cyan,
                wheelColor = px.text,
                roadColor = px.border,
                headlightColor = Color(0xFFFFF3B0),
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(bottom = 14.dp)
            )
        }
        }

        // ---------- 风格选择弹窗（皮肤 × 配色，点击即时预览） ----------
        if (showStylePicker) {
            PixelDialog(
                px = px,
                onDismiss = { showStylePicker = false },
                title = "界面风格"
            ) {
                Text("点击即时换装，点空白处关闭", style = MaterialTheme.typography.labelMedium, color = px.textDim)
                Spacer(Modifier.height(10.dp))
                com.km1quiz.app.ui.pixel.SectionTitle("皮肤", px)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    com.km1quiz.app.ui.theme.PxStyles.SKINS.forEach { (skinId, skinName) ->
                        val active = vm.skinId == skinId
                        com.km1quiz.app.ui.pixel.PixelTextButton(
                            text = skinName,
                            px = px,
                            onClick = { vm.setSkin(skinId) },
                            small = true,
                            bg = if (active) px.accent else px.surfaceAlt,
                            fg = if (active) px.accentFg else px.text,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                com.km1quiz.app.ui.pixel.SectionTitle("配色", px)
                Spacer(Modifier.height(6.dp))
                com.km1quiz.app.ui.theme.PxStyles.STYLES.forEach { meta ->
                    StyleRow(
                        meta = meta,
                        current = vm.styleId == meta.id,
                        onPick = { vm.setStyle(meta.id) }
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

/** 风格行：名称 + 五色色板条 + 当前标记，点击即时切换 */
@Composable
private fun StyleRow(meta: com.km1quiz.app.ui.theme.StyleMeta, current: Boolean, onPick: () -> Unit) {
    val px = LocalPx.current
    val sample = com.km1quiz.app.ui.theme.PxStyles.resolve(meta.id, systemDark = false)
    PixelButton(
        px = px,
        onClick = onPick,
        modifier = Modifier.fillMaxWidth(),
        bg = if (current) px.accent.copy(alpha = 0.16f) else px.surface
    ) {
        Text(
            meta.name,
            style = MaterialTheme.typography.titleMedium,
            color = px.text,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf(sample.accent, sample.ok, sample.cyan, sample.warn, sample.bad).forEach { c ->
                Box(Modifier.size(16.dp).background(c).border(1.dp, px.border))
            }
        }
        Spacer(Modifier.width(8.dp))
        if (current) {
            PixelChip("当前", px, px.accent)
        }
    }
}

/** 2x2 四色块小图标（风格切换入口；圆润皮肤画圆点） */
@Composable
private fun StyleQuads(modifier: Modifier = Modifier) {
    val px = LocalPx.current
    androidx.compose.foundation.Canvas(modifier) {
        val s = size.width / 3f
        val g = size.width / 6f
        val positions = listOf(
            androidx.compose.ui.geometry.Offset(0f, 0f) to px.accent,
            androidx.compose.ui.geometry.Offset(s + g, 0f) to px.ok,
            androidx.compose.ui.geometry.Offset(0f, s + g) to px.bad,
            androidx.compose.ui.geometry.Offset(s + g, s + g) to px.cyan
        )
        positions.forEach { (p, c) ->
            if (px.iconSoft) {
                drawCircle(c, radius = s / 2f, center = p + androidx.compose.ui.geometry.Offset(s / 2f, s / 2f))
            } else {
                drawRect(c, p, androidx.compose.ui.geometry.Size(s, s))
            }
        }
    }
}

/** 闯关轨道：块状路面 + 像素车停在进度位置（颠簸待发） */
@Composable
private fun ProgressRoad(progress: Float) {
    val px = LocalPx.current
    val t = rememberInfiniteTransition(label = "road")
    val bounce by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(240), RepeatMode.Reverse),
        label = "bounce"
    )
    val carX by animateFloatAsState(progress.coerceIn(0f, 1f), tween(600), label = "carX")
    val carPalette = mapOf('S' to px.cyan, 'W' to px.text, 'L' to Color(0xFFFFF3B0))

    BoxWithConstraints(Modifier.fillMaxWidth().height(38.dp)) {
        val carW = 32.dp
        val trackW = maxWidth - carW
        // 路面：像素=交替色块，其余=连续圆角轨
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(12.dp).align(Alignment.BottomCenter)) {
            if (px.barDiscrete) {
                val block = 10.dp.toPx()
                var x = 0f
                var i = 0
                while (x < size.width) {
                    drawRect(
                        if (i % 2 == 0) px.border else px.surfaceAlt,
                        androidx.compose.ui.geometry.Offset(x, 0f),
                        androidx.compose.ui.geometry.Size(block.coerceAtMost(size.width - x), size.height)
                    )
                    x += block
                    i++
                }
            } else {
                val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
                drawRoundRect(
                    px.surfaceAlt,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height),
                    cornerRadius = r
                )
                drawRoundRect(
                    px.accent.copy(alpha = 0.35f),
                    topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(size.width * progress.coerceIn(0f, 1f), size.height),
                    cornerRadius = r
                )
            }
        }
        // 小车：随进度前进 + 颠簸（像素皮肤=像素车，其余=平滑圆角车）
        val carMod = Modifier
            .align(Alignment.BottomStart)
            .offset(x = trackW * carX, y = 2.dp + (if (bounce > 0.5f) 2.dp else 0.dp))
        if (px.skin == com.km1quiz.app.ui.theme.Skin.PIXEL) {
            PixelIcon(
                glyph = Glyphs.car,
                color = px.accent,
                palette = carPalette,
                scale = 2.dp,
                modifier = carMod.size(32.dp, 16.dp)
            )
        } else {
            androidx.compose.foundation.Canvas(carMod.size(34.dp, 18.dp)) {
                val w = size.width
                val h = size.height
                val r = androidx.compose.ui.geometry.CornerRadius(h * 0.28f)
                // 车身 + 座舱
                drawRoundRect(px.accent, androidx.compose.ui.geometry.Offset(0f, h * 0.35f), androidx.compose.ui.geometry.Size(w, h * 0.5f), cornerRadius = r)
                drawRoundRect(px.accent, androidx.compose.ui.geometry.Offset(w * 0.24f, h * 0.08f), androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.5f), cornerRadius = r)
                // 车窗
                drawRoundRect(px.cyan, androidx.compose.ui.geometry.Offset(w * 0.31f, h * 0.16f), androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.3f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.1f))
                drawRoundRect(px.cyan, androidx.compose.ui.geometry.Offset(w * 0.51f, h * 0.16f), androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.3f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.1f))
                // 车轮
                drawCircle(px.text, radius = h * 0.17f, center = androidx.compose.ui.geometry.Offset(w * 0.24f, h * 0.85f))
                drawCircle(px.text, radius = h * 0.17f, center = androidx.compose.ui.geometry.Offset(w * 0.76f, h * 0.85f))
            }
        }
    }
}

/** 三格统计：数值 + 标签，数值颜色区分含义（value<0 显示"—"占位） */
@Composable
private fun StatChip(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    val px = LocalPx.current
    PixelCard(px = px, modifier = modifier, contentPadding = 10.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (value >= 0) {
                CountUpText(
                    target = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    color = color
                )
            } else {
                Text(
                    "—",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    color = color
                )
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = px.textDim)
        }
    }
}

/** 2x2 宫格的模式卡片：大图标 + 名称 + 角标悬浮右上（RowScope 以便 weight 均分） */
@Composable
private fun androidx.compose.foundation.layout.RowScope.ModeCard(
    glyph: List<String>,
    tint: Color,
    name: String,
    badge: Int,
    onClick: () -> Unit
) {
    val px = LocalPx.current
    Box(Modifier.weight(1f)) {
        PixelButton(px = px, onClick = onClick, modifier = Modifier.fillMaxWidth(), bg = px.surface) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PixelIcon(glyph, tint, Modifier.size(34.dp, 34.dp), scale = 2.8.dp)
                Spacer(Modifier.height(8.dp))
                Text(name, style = MaterialTheme.typography.titleMedium, color = px.text)
            }
        }
        if (badge > 0) {
            PixelBadge(
                badge, px,
                Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = (-6).dp).scale(0.9f)
            )
        }
    }
}
