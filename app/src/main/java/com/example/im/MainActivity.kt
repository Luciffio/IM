package com.example.im

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.im.ui.persona.PersonaChatScreen
import com.example.im.ui.persona.rememberPersonaChatState
import com.example.im.ui.theme.IMTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IMTheme {
                val state = rememberPersonaChatState()
                PersonaChatScreen(state = state)
            }
        }
    }
}
