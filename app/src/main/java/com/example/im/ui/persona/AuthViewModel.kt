package com.example.im.ui.persona

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val PREFS_NAME     = "im_prefs"
private const val KEY_WS_URL     = "ws_url"
private const val DEFAULT_WS_URL = "ws://127.0.0.1:8080/ws"

// ── Wire-format models (mirror Go structs) ────────────────────────────────────

/** A message received from the WebSocket (mirrors Go's tg.Message). */
data class WsMessage(
    val msgId:      Int,
    val chatId:     Long,
    val chatTitle:  String,
    val sender:     String,
    val text:       String,
    val timestamp:  String,   // ISO-8601
    val isIncoming: Boolean,
) {
    fun toTelegramMessage(): TelegramMessage {
        val ts = runCatching { Instant.parse(timestamp).toEpochMilli() }
            .getOrElse { System.currentTimeMillis() }
        return TelegramMessage(
            id         = msgId.toLong(),
            text       = text,
            senderId   = if (isIncoming) chatId else 0L,
            senderName = sender,
            timestamp  = ts,
            isOutgoing = !isIncoming,
        )
    }
}

// ── Auth state ────────────────────────────────────────────────────────────────

sealed class AuthState {
    object Idle             : AuthState()
    object Connecting       : AuthState()
    object WaitingPhone     : AuthState()
    object WaitingCode      : AuthState()
    object WaitingPassword  : AuthState()
    data class Authenticated(val name: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    // Auth state
    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    // Server URL (persisted)
    private val _wsUrl = MutableStateFlow(loadWsUrl())
    val wsUrl: StateFlow<String> = _wsUrl

    // Chat list from WS
    private val _chatList = MutableStateFlow<List<ChatPreview>>(emptyList())
    val chatList: StateFlow<List<ChatPreview>> = _chatList

    // Real-time incoming messages (new messages only, not history)
    private val _incomingMessage = MutableSharedFlow<WsMessage>(extraBufferCapacity = 64)
    val incomingMessage: SharedFlow<WsMessage> = _incomingMessage

    // Message history per chatId (loaded on demand)
    private val _messageCache = MutableStateFlow<Map<Long, List<TelegramMessage>>>(emptyMap())

    // OkHttp
    private val httpClient = OkHttpClient()
    private var ws: WebSocket? = null

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun loadWsUrl(): String =
        getApplication<Application>()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_WS_URL, DEFAULT_WS_URL) ?: DEFAULT_WS_URL

    private fun persistWsUrl(url: String) {
        getApplication<Application>()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_WS_URL, url).apply()
        _wsUrl.value = url
    }

    fun setWsUrl(url: String) {
        persistWsUrl(url.trim())
        ws?.close(1000, "url changed")
        ws = null
        _state.value = AuthState.Idle
        connect()
    }

    // ── Connection ────────────────────────────────────────────────────────────

    private var connectAttempts = 0

    fun connect() {
        if (_state.value is AuthState.Connecting) return
        connectAttempts = 0
        _state.value = AuthState.Connecting
        doConnect()
    }

    private fun doConnect() {
        val request = Request.Builder().url(_wsUrl.value).build()
        ws = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) = Unit

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                // Retry while the embedded backend is starting up (up to ~20 s).
                if (connectAttempts < 13) {
                    connectAttempts++
                    viewModelScope.launch(Dispatchers.IO) {
                        delay(1500)
                        doConnect()
                    }
                } else {
                    _state.value = AuthState.Error(t.message ?: "Connection failed")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }
        })
    }

    // ── Messaging API ─────────────────────────────────────────────────────────

    /** Request message history for a chat. Server responds with a "messages" event. */
    fun loadMessages(chatId: Long) {
        ws?.send("""{"type":"get_messages","chatId":$chatId}""")
    }

    /** Send a text message to a chat. */
    fun sendMessage(chatId: Long, text: String) {
        val escaped = text.replace("\\", "\\\\").replace("\"", "\\\"")
        ws?.send("""{"type":"send_message","chatId":$chatId,"text":"$escaped"}""")
    }

    /** Clear stale cached messages so loadHistory sees fresh server data. */
    fun clearMessageCache(chatId: Long) {
        _messageCache.update { it - chatId }
    }

    /** Returns a Flow of cached messages for a given chat. */
    fun messageFlow(chatId: Long): Flow<List<TelegramMessage>> =
        _messageCache.map { it[chatId] ?: emptyList() }

    // ── WS message parsing ────────────────────────────────────────────────────

    private fun handleMessage(text: String) {
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return
        when (json.optString("type")) {
            "auth_phone_needed" -> _state.value = AuthState.WaitingPhone
            "auth_code_needed"  -> _state.value = AuthState.WaitingCode
            "auth_2fa_needed"   -> _state.value = AuthState.WaitingPassword
            "auth_ok"           -> {
                _state.value = AuthState.Authenticated(json.optString("name", ""))
            }
            "auth_error" -> {
                val msg = json.optString("message", "Unknown error")
                viewModelScope.launch { _state.value = AuthState.Error(msg) }
            }

            "chats" -> {
                val data = json.optJSONArray("data") ?: return
                _chatList.value = parseChats(data)
            }

            "message" -> {
                val data = json.optJSONObject("data") ?: return
                val wsMsg = parseWsMessage(data)
                _incomingMessage.tryEmit(wsMsg)
                // Update last message in chat list
                _chatList.update { list ->
                    list.map { chat ->
                        if (chat.id == wsMsg.chatId) chat.copy(
                            lastMessage = if (wsMsg.isIncoming) wsMsg.text else "You: ${wsMsg.text}"
                        ) else chat
                    }
                }
            }

            "messages" -> {
                val chatId = json.optLong("chatId")
                val data   = json.optJSONArray("data") ?: return
                val msgs = (0 until data.length()).mapNotNull { i ->
                    data.optJSONObject(i)?.let { parseWsMessage(it).toTelegramMessage() }
                }
                _messageCache.update { it + (chatId to msgs) }
            }
        }
    }

    // ── JSON helpers ──────────────────────────────────────────────────────────

    private fun parseWsMessage(obj: JSONObject) = WsMessage(
        msgId      = obj.optInt("msgId"),
        chatId     = obj.optLong("chatId"),
        chatTitle  = obj.optString("chatTitle"),
        sender     = obj.optString("sender"),
        text       = obj.optString("text"),
        timestamp  = obj.optString("timestamp"),
        isIncoming = obj.optBoolean("isIncoming"),
    )

    private fun parseChats(array: JSONArray): List<ChatPreview> =
        (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            val date = runCatching {
                Instant.parse(obj.optString("timestamp"))
                    .atZone(ZoneId.systemDefault()).toLocalDate()
            }.getOrElse { LocalDate.now() }
            ChatPreview(
                id          = obj.optLong("id"),
                name        = obj.optString("title"),
                lastMessage = if (obj.optBoolean("isIncoming"))
                    obj.optString("lastMessage")
                else
                    "You: ${obj.optString("lastMessage")}",
                date        = date,
                hasUnread   = false,
                color       = chatColor(obj.optLong("id")),
            )
        }

    // ── Auth API ──────────────────────────────────────────────────────────────

    fun sendPhone(phone: String) {
        ws?.send("""{"type":"auth_phone","phone":"$phone"}""")
    }

    fun sendCode(code: String) {
        ws?.send("""{"type":"auth_code","code":"$code"}""")
    }

    fun sendPassword(password: String) {
        ws?.send("""{"type":"auth_password","password":"$password"}""")
    }

    fun retry() {
        ws?.close(1000, "retry")
        ws = null
        connect()
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        ws?.close(1000, "ViewModel cleared")
        httpClient.dispatcher.executorService.shutdown()
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private val chatPalette = listOf(
    Color(0xFFFE93C9), // pink  (P5ColorAnn)
    Color(0xFFFFD04E), // yellow (P5ColorRyuji)
    Color(0xFF68A7FF), // blue  (P5ColorYusuke)
    Color(0xFF7CFF7C), // green
    Color(0xFFFF8C42), // orange
)

private fun chatColor(id: Long): Color =
    chatPalette[(id % chatPalette.size).toInt().let { if (it < 0) it + chatPalette.size else it }]
