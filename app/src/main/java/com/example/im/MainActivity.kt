package com.example.im

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.im.ui.persona.ChatPreview
import com.example.im.ui.persona.PersonaChatListScreen
import com.example.im.ui.persona.PersonaChatScreen
import com.example.im.ui.persona.SampleParticipants
import com.example.im.ui.persona.rememberPersonaChatState
import com.example.im.ui.theme.IMTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Remove the system window background so it never bleeds through during transitions
        window.setBackgroundDrawableResource(android.R.color.black)
        enableEdgeToEdge()
        setContent {
            IMTheme {
                // Black base layer — prevents the Material white background from
                // showing through during AnimatedContent cross-fade transitions.
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                ) {
                    var openChat by remember { mutableStateOf<ChatPreview?>(null) }

                    AnimatedContent(
                        targetState  = openChat,
                        transitionSpec = {
                            val forward = targetState != null
                            if (forward) {
                                (slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { it } +
                                 fadeIn(tween(360, easing = FastOutSlowInEasing))) togetherWith
                                (slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { -it / 4 } +
                                 fadeOut(tween(160)))
                            } else {
                                (slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { -it / 4 } +
                                 fadeIn(tween(360, easing = FastOutSlowInEasing))) togetherWith
                                (slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { it } +
                                 fadeOut(tween(160)))
                            }
                        },
                        label = "navTransition",
                    ) { chat ->
                        if (chat == null) {
                            PersonaChatListScreen(
                                onChatClick = { openChat = it },
                            )
                        } else {
                            BackHandler { openChat = null }
                            PersonaChatScreen(
                                state  = rememberPersonaChatState(participants = SampleParticipants),
                                onBack = { openChat = null },
                                date   = chat.date,
                            )
                        }
                    }
                }
            }
        }
    }
}
