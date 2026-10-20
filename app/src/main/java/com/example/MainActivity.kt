package com.example

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.ui.screens.GlyphCamStudioScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ProCameraViewModel
import com.example.viewmodel.isCameraMode

class MainActivity : ComponentActivity() {
    private val cameraViewModel: ProCameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                GlyphCamStudioScreen(
                    viewModel = cameraViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    /** Volume keys work as a shutter release while the camera is on screen (can be turned off in settings). */
    private fun volumeKeyIsShutter(keyCode: Int): Boolean {
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) return false
        val s = cameraViewModel.uiState.value
        return s.volumeKeyShutter && s.activeMode.isCameraMode
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (volumeKeyIsShutter(keyCode)) {
            if (event == null || event.repeatCount == 0) cameraViewModel.onVolumeKey()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (volumeKeyIsShutter(keyCode)) return true
        return super.onKeyUp(keyCode, event)
    }
}
