package com.km1quiz.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.km1quiz.app.ui.screens.EmptyScreen
import com.km1quiz.app.ui.screens.ExamScreen
import com.km1quiz.app.ui.screens.HomeScreen
import com.km1quiz.app.ui.screens.LoadingScreen
import com.km1quiz.app.ui.screens.PracticeScreen
import com.km1quiz.app.ui.screens.ResultScreen
import com.km1quiz.app.ui.theme.LocalPx

/** 总路由：屏幕转场（方向性滑动+淡入淡出）、返回键、棋盘底纹、考试常亮 */
@Composable
fun AppRoot(vm: QuizViewModel) {
    val px = LocalPx.current
    val context = LocalContext.current

    // 首页双击返回退出
    var lastBackAt by remember { mutableLongStateOf(0L) }
    BackHandler(enabled = vm.screen == Screen.HOME && vm.loaded) {
        val now = System.currentTimeMillis()
        if (now - lastBackAt < 2000) {
            (context as? android.app.Activity)?.finish()
        } else {
            lastBackAt = now
            Toast.makeText(context, "再按一次退出", Toast.LENGTH_SHORT).show()
        }
    }

    // 模拟考试期间屏幕常亮
    DisposableEffect(vm.screen) {
        val window = (context as? android.app.Activity)?.window
        if (vm.screen == Screen.EXAM) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(px.bg)
            .drawChecker(px)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (!vm.loaded) {
            LoadingScreen()
            return@Box
        }
        // 屏幕切换（顺序号决定滑动方向）
        AnimatedContent(
            targetState = vm.screen,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val dir = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                (slideInHorizontally(tween(260)) { dir * it / 3 } + fadeIn(tween(260))) togetherWith
                    (slideOutHorizontally(tween(200)) { -dir * it / 3 } + fadeOut(tween(150)))
            },
            label = "screen"
        ) { target ->
            Column(Modifier.fillMaxWidth()) {
                when (target) {
                    Screen.HOME -> HomeScreen(vm)
                    Screen.PRACTICE -> vm.practiceUi?.let { PracticeScreen(vm, it) }
                    Screen.EXAM -> vm.examUi?.let { ExamScreen(vm, it) }
                    Screen.RESULT -> vm.resultUi?.let { ResultScreen(vm, it) }
                    Screen.EMPTY -> EmptyScreen(vm, vm.emptyReason)
                }
            }
        }
    }
}

/** 棋盘格底纹：16dp 一格，低对比，营造复古像素纸面 */
private fun Modifier.drawChecker(px: com.km1quiz.app.ui.theme.Px): Modifier = this.then(
    Modifier.drawBehind {
        val cell = 16.dp.toPx()
        val alt = px.surface.copy(alpha = if (px == com.km1quiz.app.ui.theme.PxDark) 0.30f else 0.45f)
        var y = 0f
        var row = 0
        while (y < size.height) {
            var x = 0f
            var col = 0
            while (x < size.width) {
                if ((row + col) % 2 == 0) {
                    drawRect(alt, Offset(x, y), Size(cell, cell))
                }
                x += cell
                col++
            }
            y += cell
            row++
        }
    }
)
