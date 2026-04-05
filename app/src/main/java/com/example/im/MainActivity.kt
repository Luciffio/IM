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
import com.example.im.ui.persona.PersonaChatSettingsScreen
import com.example.im.ui.persona.PersonaSettingsScreen
import com.example.im.ui.persona.SampleParticipants
import com.example.im.ui.persona.rememberPersonaChatState
import com.example.im.ui.theme.IMTheme

private sealed class Screen {
    object ChatList    : Screen()
    data class Chat(val chat: ChatPreview) : Screen()
    object Settings    : Screen()
    object ChatSettings : Screen()

    val depth: Int get() = when (this) {
        ChatList    -> 0
        is Chat, Settings -> 1
        ChatSettings -> 2
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.black)
        enableEdgeToEdge()
        setContent {
            IMTheme {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                ) {
                    var screen         by remember { mutableStateOf<Screen>(Screen.ChatList) }
                    var showTimestamps by remember { mutableStateOf(true) }

                    AnimatedContent(
                        targetState  = screen,
                        transitionSpec = {
                            val forward = targetState.depth >= initialState.depth
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
                    ) { currentScreen ->
                        when (currentScreen) {
                            Screen.ChatList -> PersonaChatListScreen(
                                onChatClick     = { screen = Screen.Chat(it) },
                                onSettingsClick = { screen = Screen.Settings },
                            )
                            is Screen.Chat -> {
                                BackHandler { screen = Screen.ChatList }
                                PersonaChatScreen(
                                    state          = rememberPersonaChatState(participants = SampleParticipants),
                                    onBack         = { screen = Screen.ChatList },
                                    date           = currentScreen.chat.date,
                                    showTimestamps = showTimestamps,
                                )
                            }
                            Screen.Settings -> {
                                BackHandler { screen = Screen.ChatList }
                                PersonaSettingsScreen(
                                    onBack              = { screen = Screen.ChatList },
                                    onChatSettingsClick = { screen = Screen.ChatSettings },
                                )
                            }
                            Screen.ChatSettings -> {
                                BackHandler { screen = Screen.Settings }
                                PersonaChatSettingsScreen(
                                    showTimestamps         = showTimestamps,
                                    onShowTimestampsChange = { showTimestamps = it },
                                    onBack                 = { screen = Screen.Settings },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
