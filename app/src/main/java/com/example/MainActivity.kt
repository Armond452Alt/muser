package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.audio.SoundboardClip
import com.example.ui.MuserViewModel
import com.example.ui.screens.ConsoleGuideScreen
import com.example.ui.screens.MainMicScreen
import com.example.ui.theme.MyApplicationTheme

enum class MuserScreen {
    MAIN_MIC,
    CONSOLE_GUIDE
}

class MainActivity : ComponentActivity() {

    private val viewModel: MuserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MuserApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MuserApp(
    viewModel: MuserViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(MuserScreen.MAIN_MIC) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_nav_transition",
        modifier = modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            MuserScreen.MAIN_MIC -> {
                MainMicScreen(
                    viewModel = viewModel,
                    onNavigateToGuide = {
                        currentScreen = MuserScreen.CONSOLE_GUIDE
                    }
                )
            }
            MuserScreen.CONSOLE_GUIDE -> {
                ConsoleGuideScreen(
                    onBack = {
                        currentScreen = MuserScreen.MAIN_MIC
                    },
                    onPlayCalibrationTone = {
                        viewModel.playSoundboardClip(SoundboardClip.CALIBRATION_1KHZ)
                    }
                )
            }
        }
    }
}
