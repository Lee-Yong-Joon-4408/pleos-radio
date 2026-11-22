package com.pleos.radio.presentation

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.pleos.radio.presentation.ui.RadioApp
import com.pleos.radio.presentation.ui.theme.RadioTheme

/** Launches straight into the Radio main screen. */
class MainActivity : ComponentActivity() {

    private val viewModel: RadioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RadioTheme {
                RadioApp(viewModel)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val key = when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_NEXT -> MediaKey.NEXT
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> MediaKey.PREVIOUS
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> MediaKey.PLAY_PAUSE
            KeyEvent.KEYCODE_MEDIA_PLAY -> MediaKey.PLAY
            KeyEvent.KEYCODE_MEDIA_PAUSE -> MediaKey.PAUSE
            else -> return super.onKeyDown(keyCode, event)
        }
        viewModel.onMediaKey(key)
        return true
    }
}
