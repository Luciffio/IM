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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.im.ui.persona.AuthViewModel
import com.example.im.ui.persona.AuthState
import com.example.im.ui.persona.ChatParticipant
import com.example.im.ui.persona.ChatPreview
import com.example.im.ui.persona.PersonaAuthScreen
import com.example.im.ui.persona.PersonaChatListScreen
import com.example.im.ui.persona.PersonaChatScreen
import com.example.im.ui.persona.PersonaChatSettingsScreen
import com.example.im.ui.persona.PersonaSettingsScreen
import com.example.im.ui.persona.rememberPersonaChatState
import com.example.im.ui.theme.IMTheme

private sealed class Screen {
    /** Auth is at depth -1 so forward transitions animate correctly. */
    object Auth         : Screen()
    object ChatList     : Screen()
    data class Chat(val chat: ChatPreview) : Screen()
    object Settings     : Screen()
    object ChatSettings : Screen()

    val depth: Int get() = when (this) {
        Auth        -> -1
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
                    var screen         by remember { mutableStateOf<Screen>(Screen.Auth) }
                    var showTimestamps by remember { mutableStateOf(true) }

                    // Single shared ViewModel — keeps the WS alive for the whole session.
                    val vm: AuthViewModel = viewModel()

                    // Navigate to ChatList as soon as auth succeeds.
                    val authState by vm.state.collectAsState()
                    LaunchedEffect(authState) {
                        if (authState is AuthState.Authenticated && screen == Screen.Auth) {
                            screen = Screen.ChatList
                        }
                    }

                    AnimatedContent(
                        targetState = screen,
                        transitionSpec = {
                            val forward = targetState.depth >= initialState.depth
                            if (forward) {
                                (slideInHorizontally(tween(250, easing = FastOutSlowInEasing)) { it } +
                                 fadeIn(tween(230, easing = FastOutSlowInEasing))) togetherWith
                                (slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { -it / 4 } +
                                 fadeOut(tween(100)))
                            } else {
                                (slideInHorizontally(tween(250, easing = FastOutSlowInEasing)) { -it / 4 } +
                                 fadeIn(tween(230, easing = FastOutSlowInEasing))) togetherWith
                                (slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { it } +
                                 fadeOut(tween(100)))
                            }
                        },
                        label = "navTransition",
                    ) { currentScreen ->
                        when (currentScreen) {

                            // ── Auth ─────────────────────────────────────────
                            Screen.Auth -> {
                                PersonaAuthScreen(
                                    vm             = vm,
                                    onAuthenticated = { screen = Screen.ChatList },
                                )
                            }

                            // ── Chat list ─────────────────────────────────────
                            Screen.ChatList -> {
                                val chats by vm.chatList.collectAsState()
                                PersonaChatListScreen(
                                    chats           = chats,
                                    onChatClick     = { screen = Screen.Chat(it) },
                                    onSettingsClick = { screen = Screen.Settings },
                                )
                            }

                            // ── Chat screen ───────────────────────────────────
                            is Screen.Chat -> {
                                val chat   = currentScreen.chat
                                val chatId = chat.id

                                // One participant — the contact we're chatting with.
                                val participant = remember(chat) {
                                    mapOf(chatId to ChatParticipant(chatId, chat.name, chat.color))
                                }

                                val state = rememberPersonaChatState(
                                    participants = participant,
                                    messages     = emptyList(), // start empty; history loads below
                                    onSend       = { text -> vm.sendMessage(chatId, text) },
                                )

                                // Clear stale cache then request fresh history.
                                LaunchedEffect(chatId) {
                                    vm.clearMessageCache(chatId)
                                    vm.loadMessages(chatId)
                                }

                                // When history arrives, populate the transcript (no animation).
                                LaunchedEffect(chatId) {
                                    vm.messageFlow(chatId).collect { msgs ->
                                        if (msgs.isNotEmpty()) state.loadHistory(msgs)
                                    }
                                }

                                // Animate in each new real-time incoming message.
                                LaunchedEffect(chatId) {
                                    vm.incomingMessage.collect { wsMsg ->
                                        if (wsMsg.chatId == chatId && wsMsg.isIncoming) {
                                            state.receiveIncoming(wsMsg.toTelegramMessage())
                                        }
                                    }
                                }

                                BackHandler { screen = Screen.ChatList }
                                PersonaChatScreen(
                                    state          = state,
                                    onBack         = { screen = Screen.ChatList },
                                    date           = chat.date,
                                    contactName    = chat.name.uppercase(),
                                    contactColor   = chat.color,
                                    contactInitial = chat.name.firstOrNull()?.toString() ?: "?",
                                    showTimestamps = showTimestamps,
                                )
                            }

                            // ── Settings ──────────────────────────────────────
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
