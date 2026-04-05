package com.example.im.ui.persona

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// Workaround for https://issuetracker.google.com/issues/354405919
private val BetterEaseOutBack: Easing = Easing { fraction ->
    try { EaseOutBack.transform(fraction) } catch (e: IllegalArgumentException) { 1f }
}

// ---------------------------------------------------------------------------
// Entry — equivalent to the reference Entry data class.
// ---------------------------------------------------------------------------

data class PersonaEntry(
    val message: TelegramMessage,
    val participant: ChatParticipant?,
    val lineCoordinates: LineCoordinates,
    val drawPunctuation: Boolean,
    val lineProgress: State<Float>,
    val avatarBackgroundScale: State<Float>,
    val avatarForegroundScale: State<Float>,
    val messageHorizontalScale: State<Float>,
    val messageVerticalScale: State<Float>,
    val messageTextAlpha: State<Float>,
    val punctuationScale: State<Float>,
)

data class LineCoordinates(
    val leftPoint: Offset,
    val rightPoint: Offset,
)

// ---------------------------------------------------------------------------
// TranscriptSizes
// ---------------------------------------------------------------------------

object TranscriptSizes {
    val AvatarSize   = PersonaSizes.AvatarSize
    val EntrySpacing = PersonaSizes.EntrySpacing

    fun getTopDrawingOffset(scope: CacheDrawScope, entry: PersonaEntry): Offset = Offset.Zero

    fun getBottomDrawingOffset(scope: CacheDrawScope, entry: PersonaEntry): Offset = with(scope) {
        Offset(x = 0f, y = size.height + EntrySpacing.toPx())
    }
}

// ---------------------------------------------------------------------------
// rememberPersonaChatState
// ---------------------------------------------------------------------------

/**
 * @param onSend  Called when the user submits a message via the input field.
 *                Use this to forward the text to the real network layer.
 */
@Composable
fun rememberPersonaChatState(
    participants: Map<Long, ChatParticipant> = SampleParticipants,
    messages:     List<TelegramMessage>      = SampleMessages,
    onSend:       (String) -> Unit           = {},
): PersonaChatState {
    val density      = LocalDensity.current
    val scope        = rememberCoroutineScope()
    val screenWidthPx = with(density) {
        LocalConfiguration.current.screenWidthDp.toFloat() * density.density
    }
    return remember(density, screenWidthPx) {
        PersonaChatState(density, scope, participants, messages, screenWidthPx, onSend)
    }
}

// ---------------------------------------------------------------------------
// PersonaChatState
// ---------------------------------------------------------------------------

@Stable
class PersonaChatState internal constructor(
    private val density:       Density,
    private val coroutineScope: CoroutineScope,
    private val participants:  Map<Long, ChatParticipant>,
    private val allMessages:   List<TelegramMessage>,
    private val screenWidthPx: Float = 0f,
    private val onSend:        (String) -> Unit = {},
) {
    private var cursor     = 0
    private var nextId     = 100L
    private val entryStates = mutableListOf<EntryState>()
    private val _entries   = mutableStateOf<List<PersonaEntry>>(emptyList())
    val entries: List<PersonaEntry> get() = _entries.value

    /** True after [loadHistory] has been called once — prevents double-loads. */
    var historyLoaded = false
        private set

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Send an outgoing message: appends it locally AND forwards it via [onSend].
     * Used by the input field in [PersonaChatScreen].
     */
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val trimmed = text.trim()
        val msg = TelegramMessage(
            id         = nextId++,
            text       = trimmed,
            senderId   = 0L,
            senderName = "Me",
            timestamp  = System.currentTimeMillis(),
            isOutgoing = true,
        )
        appendMessage(msg, animated = true)
        onSend(trimmed)
    }

    /**
     * Receive an incoming message from the server and animate it in.
     * Call this from a `LaunchedEffect` that collects the WS incoming flow.
     */
    fun receiveIncoming(msg: TelegramMessage) {
        appendMessage(msg, animated = true)
    }

    /**
     * Populate the transcript with historical messages instantly (no pop-in).
     * Safe to call multiple times — only the first call takes effect.
     */
    fun loadHistory(messages: List<TelegramMessage>) {
        if (historyLoaded || messages.isEmpty()) return
        historyLoaded = true
        entryStates.clear()
        cursor = 0
        messages.forEach { msg ->
            entryStates += createEntryState(entryStates.size, msg, animated = false)
        }
        // Draw all connecting lines immediately
        _entries.value = entryStates.map { it.toEntry() }
    }

    /** Show the next pre-set message from the sample conversation (demo mode). */
    fun advance() {
        if (cursor >= allMessages.size) {
            cursor = 0
            entryStates.clear()
        }
        appendMessage(allMessages[cursor++], animated = true)
    }

    fun advanceAll() {
        while (cursor < allMessages.size) advance()
    }

    /** Debug helper — inject a random incoming test message. */
    fun sendTestIncoming() {
        val participant = participants.values.randomOrNull() ?: return
        appendMessage(
            TelegramMessage(
                id         = nextId++,
                text       = "Test message $nextId 👋",
                senderId   = participant.id,
                senderName = participant.name,
                timestamp  = System.currentTimeMillis(),
                isOutgoing = false,
            ),
            animated = true,
        )
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun appendMessage(msg: TelegramMessage, animated: Boolean) {
        entryStates += createEntryState(entryStates.size, msg, animated)

        if (animated && entryStates.size > 1) {
            finalizeEntryState(entryStates[entryStates.lastIndex - 1])
        }

        _entries.value = entryStates.map { it.toEntry() }
    }

    private fun createEntryState(index: Int, msg: TelegramMessage, animated: Boolean): EntryState =
        with(density) {
            val width = randomBetween(
                PersonaSizes.MinLineWidth.toPx(),
                PersonaSizes.MaxLineWidth.toPx(),
            )

            val direction       = if (index % 2 == 0) 1f else -1f
            val horizontalShift = if (index > 0)
                randomBetween(PersonaSizes.MinLineShift.toPx(), PersonaSizes.MaxLineShift.toPx()) * direction
            else 0f

            val lineCoordinates = if (msg.isOutgoing) {
                val cx     = PersonaSizes.OutgoingCenterX.toPx()
                val cy     = PersonaSizes.OutgoingCenterY.toPx()
                val anchorX = if (screenWidthPx > 0f) screenWidthPx - cx else cx
                LineCoordinates(
                    leftPoint  = Offset(anchorX - width / 2f, cy),
                    rightPoint = Offset(anchorX + width / 2f, cy),
                )
            } else {
                val ax = PersonaSizes.AvatarSize.width.toPx() / 2f
                val ay = PersonaSizes.AvatarSize.height.toPx() / 2f
                LineCoordinates(
                    leftPoint  = Offset(ax - width / 2f + horizontalShift, ay),
                    rightPoint = Offset(ax + width / 2f + horizontalShift, ay),
                )
            }

            // For history entries, start all animatables at their final value.
            val lineFinal = if (animated) 0f else 1f
            val scaleFinal = if (animated) 0f else 1f

            EntryState(
                position         = index,
                message          = msg,
                participant      = participants[msg.senderId],
                lineCoordinates  = lineCoordinates,
                lineProgress     = Animatable(lineFinal),
                avatarBackgroundScale = Animatable(if (animated) 0.6f else 1f).also { a ->
                    if (animated) coroutineScope.launch {
                        a.animateTo(1f, tween(180, easing = BetterEaseOutBack))
                    }
                },
                avatarForegroundScale = Animatable(scaleFinal).also { a ->
                    if (animated) coroutineScope.launch {
                        delay(80L); a.snapTo(0.8f)
                        a.animateTo(1f, tween(90, easing = BetterEaseOutBack))
                    }
                },
                messageHorizontalScale = Animatable(if (animated) 0.3f else 1f).also { a ->
                    if (animated) coroutineScope.launch {
                        a.animateTo(1f, tween(110, easing = BetterEaseOutBack))
                    }
                },
                messageVerticalScale = Animatable(if (animated) 0.8f else 1f).also { a ->
                    if (animated) coroutineScope.launch {
                        a.animateTo(1f, tween(110, easing = BetterEaseOutBack))
                    }
                },
                messageTextAlpha = Animatable(scaleFinal).also { a ->
                    if (animated) coroutineScope.launch {
                        delay(50L); a.animateTo(1f, tween(80))
                    }
                },
                punctuationScale = Animatable(
                    if (!animated && msg.text.endsWith('?')) 1f else 0f
                ).also { a ->
                    if (animated && msg.text.endsWith('?')) coroutineScope.launch {
                        delay(70L); a.snapTo(0.4f)
                        a.animateTo(1f, tween(60))
                    }
                },
            )
        }

    private fun finalizeEntryState(state: EntryState) {
        coroutineScope.launch {
            state.lineProgress.animateTo(1f, tween(100))
        }
    }
}

// ---------------------------------------------------------------------------
// Internal helpers
// ---------------------------------------------------------------------------

private fun randomBetween(start: Float, end: Float): Float =
    start + Random.nextFloat() * (end - start)

private class EntryState(
    val position:              Int,
    val message:               TelegramMessage,
    val participant:           ChatParticipant?,
    var lineCoordinates:       LineCoordinates,
    val lineProgress:          Animatable<Float, AnimationVector1D>,
    val avatarBackgroundScale: Animatable<Float, AnimationVector1D>,
    val avatarForegroundScale: Animatable<Float, AnimationVector1D>,
    val messageHorizontalScale: Animatable<Float, AnimationVector1D>,
    val messageVerticalScale:  Animatable<Float, AnimationVector1D>,
    val messageTextAlpha:      Animatable<Float, AnimationVector1D>,
    val punctuationScale:      Animatable<Float, AnimationVector1D>,
)

private fun EntryState.toEntry() = PersonaEntry(
    message                = message,
    participant            = participant,
    lineCoordinates        = lineCoordinates,
    drawPunctuation        = message.text.endsWith('?'),
    lineProgress           = lineProgress.asState(),
    avatarBackgroundScale  = avatarBackgroundScale.asState(),
    avatarForegroundScale  = avatarForegroundScale.asState(),
    messageHorizontalScale = messageHorizontalScale.asState(),
    messageVerticalScale   = messageVerticalScale.asState(),
    messageTextAlpha       = messageTextAlpha.asState(),
    punctuationScale       = punctuationScale.asState(),
)
