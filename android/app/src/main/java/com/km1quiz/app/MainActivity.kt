package com.km1quiz.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.km1quiz.app.ui.QuizViewModel
import com.km1quiz.app.ui.Sfx
import com.km1quiz.app.ui.AppRoot
import com.km1quiz.app.ui.theme.PixelTheme

class MainActivity : ComponentActivity() {

    private val vm: QuizViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Sfx.enabled = vm.soundEnabled
        setContent {
            PixelTheme {
                AppRoot(vm)
            }
        }
    }

    override fun onDestroy() {
        Sfx.release()
        super.onDestroy()
    }
}
