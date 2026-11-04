package com.km1quiz.app.ui.pixel

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.random.Random

/**
 * 像素图元：网格字符串 → Canvas 逐格矩形。
 * 'X' 主色，其他字符经 palette 映射；放大用整数格绘制，天然无锯齿。
 */
object Glyphs {
    // 12x12
    val star = listOf(
        ".....XX.....",
        "....XXXX....",
        "....XXXX....",
        "XXXXXXXXXXXX",
        ".XXXXXXXXXX.",
        "..XXXXXXXX..",
        "...XXXXXX...",
        "..XXXXXXX...",
        "..XXX..XXX..",
        ".XXX....XXX.",
        ".XX......XX.",
        "XX........XX"
    )
    val dice = listOf(
        "XXXXXXXXXXXX",
        "X..........X",
        "X..XX..XX..X",
        "X..XX..XX..X",
        "X..........X",
        "X....XX....X",
        "X....XX....X",
        "X..........X",
        "X..XX..XX..X",
        "X..XX..XX..X",
        "X..........X",
        "XXXXXXXXXXXX"
    )
    val list = listOf(
        "............",
        "XX..XXXXXXX.",
        "XX..XXXXXXX.",
        "............",
        "XX..XXXXXXX.",
        "XX..XXXXXXX.",
        "............",
        "XX..XXXXXXX.",
        "XX..XXXXXXX.",
        "............",
        "............",
        "............"
    )
    val paperCheck = listOf(
        "............",
        ".XXXXXXXXX..",
        ".X.......X..",
        ".X.XXXXX.X..",
        ".X.......X..",
        ".X.XXXXX.X..",
        ".X.......X..",
        ".X..XX.X.X..",
        ".X.XX.XX.X..",
        ".X.X..XX.X..",
        ".XXXXXXXXX..",
        "............"
    )
    val xCircle = listOf(
        "...XXXXXX...",
        "..X......X..",
        ".X........X.",
        "X..XX..XX..X",
        "X...XXXX...X",
        "X....XX....X",
        "X....XX....X",
        "X...XXXX...X",
        "X..XX..XX..X",
        ".X........X.",
        "..X......X..",
        "...XXXXXX..."
    )
    val clock = listOf(
        "...XXXXXX...",
        "..X......X..",
        ".X...X....X.",
        "X....X.....X",
        "X....X.....X",
        "X....XXX...X",
        "X..........X",
        "X..........X",
        ".X........X.",
        "..X......X..",
        "...XXXXXX...",
        "............"
    )
    val close = listOf(
        "XX........XX",
        "XXX......XXX",
        ".XXX....XXX.",
        "..XXX..XXX..",
        "...XXXXXX...",
        "....XXXX....",
        "....XXXX....",
        "...XXXXXX...",
        "..XXX..XXX..",
        ".XXX....XXX.",
        "XXX......XXX",
        "XX........XX"
    )
    val check = listOf(
        "............",
        "..........X.",
        ".........XX.",
        "........XX..",
        ".......XX...",
        "..X..XXX....",
        "..XXXXX.....",
        "..XXXX......",
        "...XXX......",
        "..XX........",
        ".XX.........",
        "............"
    )
    val speakerOn = listOf(
        "............",
        "......XX....",
        ".....XXX..X.",
        "X...XXXX.X..",
        "XX..XXXX..X.",
        "XXX.XXXX.X..",
        "XXX.XXXX.X..",
        "XX..XXXX..X.",
        "X...XXXX.X..",
        ".....XXX..X.",
        "......XX....",
        "............"
    )
    val speakerOff = listOf(
        "............",
        "......XX....",
        ".....XXX....",
        "X...XXXX....",
        "XX..XXXX....",
        "XXX.XXXX.XX.",
        "XXX.XXXX..XX",
        "XX..XXXX.XX.",
        "X...XXXX....",
        ".....XXX....",
        "......XX....",
        "............"
    )
    val arrowLeft = listOf(
        "............",
        "........X...",
        ".......XX...",
        "......XXX...",
        ".....XXXX...",
        "..XXXXXXXXX.",
        "XXXXXXXXXXXX",
        "XXXXXXXXXXXX",
        "..XXXXXXXXX.",
        ".....XXXX...",
        "......XXX...",
        "............"
    )
    val arrowRight = arrowLeft.map { it.reversed() }

    /** 像素小汽车（16x8）：X 车身 / S 车窗 / W 车轮 / L 车灯 */
    val car = listOf(
        ".....XXXXXX.....",
        "....XSSSSSSX....",
        "...XSSSSSSSSX...",
        "..XXXXXXXXXXXXL.",
        "XXXXXXXXXXXXXXLL",
        "XXXXXXXXXXXXXXXX",
        "..WWW.....WWW...",
        ".WWWWW...WWWWW.."
    )
}

/** 通用像素图标：glyph 网格 + palette（'X' 用 color，其余字符查表） */
@Composable
fun PixelIcon(
    glyph: List<String>,
    color: Color,
    modifier: Modifier = Modifier,
    palette: Map<Char, Color> = emptyMap(),
    scale: Dp = 2.dp
) {
    val block = with(LocalDensity.current) { scale.toPx() }
    Canvas(modifier) {
        glyph.forEachIndexed { y, row ->
            row.forEachIndexed { x, c ->
                if (c == '.') { return@forEachIndexed }
                drawRect(
                    color = if (c == 'X') color else palette[c] ?: color,
                    topLeft = Offset(x * block, y * block),
                    size = Size(block, block)
                )
            }
        }
    }
}

/** 吉祥物：行驶中的像素小汽车 + 虚线路面 + 颠簸弹跳 */
@Composable
fun PixelCarDrive(
    carColor: Color,
    windowColor: Color,
    wheelColor: Color,
    roadColor: Color,
    headlightColor: Color,
    modifier: Modifier = Modifier
) {
    val t = rememberInfiniteTransition(label = "car")
    val drive by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
        label = "drive"
    )
    val bounce by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(260, easing = LinearEasing), RepeatMode.Reverse),
        label = "bounce"
    )
    val block = with(LocalDensity.current) { 3.dp.toPx() }
    val palette = mapOf(
        'S' to windowColor,
        'W' to wheelColor,
        'L' to headlightColor
    )
    Canvas(modifier) {
        val w = size.width
        // 路面：两条虚线随车流动
        val dash = block * 4
        val gap = block * 4
        val cycle = dash + gap
        val offset = (drive * cycle * 6f) % cycle
        var x = -cycle + offset
        while (x < w) {
            drawRect(roadColor, Offset(x, size.height - block * 1.2f), Size(dash, block * 0.8f))
            x += cycle
        }
        // 车在 1/3 处巡航，颠簸 = 每 3px 抬一次
        val bob = floor(bounce * 2f) * block * 0.34f
        val carW = 16 * block
        val carX = w * 0.5f - carW / 2
        val carY = size.height - 8 * block - block * 2 - bob
        Glyphs.car.forEachIndexed { y, row ->
            row.forEachIndexed { i, c ->
                if (c == '.') { return@forEachIndexed }
                val col = if (c == 'X') carColor else palette[c] ?: carColor
                drawRect(col, Offset(carX + i * block, carY + y * block), Size(block, block))
            }
        }
    }
}

/** 像素五彩纸屑：仅在有 content 时播放，方块粒子下落 + 摆动 */
@Composable
fun PixelConfetti(colors: List<Color>, modifier: Modifier = Modifier, seed: Int = 7) {
    val t = rememberInfiniteTransition(label = "confetti")
    val time by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label = "time"
    )
    val rnd = remember(seed) { Random(seed) }
    val blockPx = with(LocalDensity.current) { 3.dp.toPx() }
    Canvas(modifier) {
        val n = 70
        repeat(n) { i ->
            val laneSeed = rnd.nextFloat()
            val speed = 0.35f + laneSeed * 0.5f
            val laneX = rnd.nextFloat() * size.width
            val sway = rnd.nextFloat()
            val sizeBlocks = 1 + (i % 3)
            val s = blockPx * sizeBlocks
            val y = (time * speed + laneSeed) % 1f * (size.height + s * 2) - s
            val x = laneX + kotlin.math.sin((time * 4f + sway * 6.28f) * 2f) * blockPx * 3f
            drawRect(colors[i % colors.size], Offset(x, y), Size(s, s))
        }
    }
}
