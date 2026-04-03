package com.example.im

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.im.ui.persona.ChatPreview
import com.example.im.ui.persona.PersonaChatListScreen
import com.example.im.ui.persona.PersonaChatScreen
import com.example.im.ui.persona.SampleParticipants
import com.example.im.ui.persona.rememberPersonaChatState
import com.example.im.ui.theme.IMTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IMTheme {
                var openChat by remember { mutableStateOf<ChatPreview?>(null) }

                if (openChat == null) {
                    PersonaChatListScreen(
                        onChatClick = { openChat = it },
                    )
                } else {
                    val chat  = openChat!!
                    val state = rememberPersonaChatState(
                        participants = SampleParticipants,
                    )

                    BackHandler { openChat = null }

                    PersonaChatScreen(
                        state  = state,
                        onBack = { openChat = null },
                        // Pass the date from the selected chat preview
                        date   = chat.date,
                    )
                }
            }
        }
    }
}
