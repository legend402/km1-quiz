package com.km1quiz.app.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView

/** m:ss 格式（考试倒计时/用时） */
fun fmtTime(sec: Int): String {
    val s = sec.coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

/** 触感反馈：答对 CONFIRM / 答错 REJECT（低版本回退 VIRTUAL_KEY） */
@Composable
fun rememberHaptics(): (ok: Boolean) -> Unit {
    val view = LocalView.current
    return { ok ->
        if (Build.VERSION.SDK_INT >= 30) {
            view.performHapticFeedback(
                if (ok) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.REJECT
            )
        } else {
            @Suppress("DEPRECATION")
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
}
