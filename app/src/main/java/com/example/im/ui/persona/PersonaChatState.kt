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
// Carries all animated State<Float> values needed by each composable.
// ---------------------------------------------------------------------------

data class PersonaEntry(
    val message: TelegramMessage,
    /** Non-null for incoming messages; null for outgoing. */
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

/** Left/right X-span of the connecting line segment anchored to this entry. */
data class LineCoordinates(
    val leftPoint: Offset,
    val rightPoint: Offset,
)

// ---------------------------------------------------------------------------
// TranscriptSizes — mirrors the object from the reference exactly.
// ---------------------------------------------------------------------------

object TranscriptSizes {
    val AvatarSize   = PersonaSizes.AvatarSize
    val EntrySpacing = PersonaSizes.EntrySpacing

    /**
     * Returns the top-of-entry drawing offset for the connecting line.
     *
     * Outgoing lineCoordinates are stored in screen-absolute X (see createEntryState),
     * so no horizontal shift is needed here for either sender direction.
     */
    fun getTopDrawingOffset(scope: CacheDrawScope, entry: PersonaEntry): Offset = Offset.Zero

    /**
     * Returns the bottom-of-entry drawing offset for the connecting line.
     *
     * X is always 0 because lineCoordinates already encode the correct screen-absolute X.
     * Previously the X shift used [CacheDrawScope.size.width], which broke for incoming
     * entries (narrower than full screen width) pointing to outgoing entries.
     */
    fun getBottomDrawingOffset(scope: CacheDrawScope, entry: PersonaEntry): Offset = with(scope) {
        Offset(x = 0f, y = size.height + EntrySpacing.toPx())
    }
}

// ---------------------------------------------------------------------------
// PersonaChatState — drives the advance-one-at-a-time reveal mechanic.
// ---------------------------------------------------------------------------

@Composable
fun rememberPersonaChatState(
    participants: Map<Long, ChatParticipant> = SampleParticipants,
    messages: List<TelegramMessage> = SampleMessages,
): PersonaChatState {
    val density      = LocalDensity.current
    val scope        = rememberCoroutineScope()
    // screenWidthPx is used to store outgoing lineCoordinates in screen-absolute X,
    // so the connecting line reaches the outgoing bubble from ANY entry width.
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.toFloat() * density.density }
    return remember(density, screenWidthPx) {
        PersonaChatState(density, scope, participants, messages, screenWidthPx)
    }
}

@Stable
class PersonaChatState internal constructor(
    private val density: Density,
    private val coroutineScope: CoroutineScope,
    private val participants: Map<Long, ChatParticipant>,
    private val allMessages: List<TelegramMessage>,
    /** Full screen width in px — used to anchor outgoing line coordinates to the right edge. */
    private val screenWidthPx: Float = 0f,
) {
    private var cursor = 0
    private var nextId = 100L
    private val entryStates = mutableListOf<EntryState>()
    private val _entries = mutableStateOf<List<PersonaEntry>>(emptyList())
    val entries: List<PersonaEntry> get() = _entries.value

    /** Send a custom outgoing message from the input field. */
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val msg = TelegramMessage(
            id         = nextId++,
            text       = text.trim(),
            senderId   = 0L,
            senderName = "Me",
            timestamp  = System.currentTimeMillis(),
            isOutgoing = true,
        )
        appendMessage(msg)
    }

    /** Show the next pre-set message from the sample conversation. */
    fun advance() {
        if (cursor >= allMessages.size) {
            cursor = 0
            entryStates.clear()
        }

        val msg = allMessages[cursor++]
        appendMessage(msg)
    }

    private fun appendMessage(msg: TelegramMessage) {
        entryStates += createEntryState(entryStates.size, msg)

        if (entryStates.size > 1) {
            finalizeEntryState(entryStates[entryStates.lastIndex - 1])
        }

        _entries.value = entryStates.map { it.toEntry() }
    }

    /** Advance all remaining messages at once (auto-play). */
    fun advanceAll() {
        while (cursor < allMessages.size) advance()
    }

    /** Inject a test incoming message from a random participant (debug only). */
    fun sendTestIncoming() {
        val participant = participants.values.randomOrNull() ?: return
        val msg = TelegramMessage(
            id         = nextId++,
            text       = "Test message ${nextId} 👋",
            senderId   = participant.id,
            senderName = participant.name,
            timestamp  = System.currentTimeMillis(),
            isOutgoing = false,
        )
        appendMessage(msg)
    }

    private fun createEntryState(index: Int, msg: TelegramMessage): EntryState = with(density) {
        val width = randomBetween(
            PersonaSizes.MinLineWidth.toPx(),
            PersonaSizes.MaxLineWidth.toPx(),
        )

        // Apply the horizontal shift HERE at creation time so that lineCoordinates
        // never change after the entry is first rendered.  Previously the shift was
        // applied in finalizeEntryState (when the *next* message arrived), which
        // caused the already-fully-drawn N-2→N-1 line to jump because its bottom
        // endpoint (= entry N-1's lineCoordinates) suddenly changed.
        val direction      = if (index % 2 == 0) 1f else -1f
        val horizontalShift = if (index > 0) {
            randomBetween(PersonaSizes.MinLineShift.toPx(), PersonaSizes.MaxLineShift.toPx()) * direction
        } else 0f

        val lineCoordinates = if (msg.isOutgoing) {
            val cx = PersonaSizes.OutgoingCenterX.toPx()
            val cy = PersonaSizes.OutgoingCenterY.toPx()
            // Store X in screen-absolute terms: (screenWidth - cx) is the right-side anchor.
            // This makes getTopDrawingOffset / getBottomDrawingOffset return x=0 (no shift),
            // so the line endpoint is correct regardless of which entry's drawConnectingLine
            // is computing the coordinates (incoming entries are narrower than screen width).
            val anchorX = if (screenWidthPx > 0f) screenWidthPx - cx else cx
            LineCoordinates(
                leftPoint  = Offset(anchorX - width / 2f, cy),
                rightPoint = Offset(anchorX + width / 2f, cy),
            )
        } else {
            val avatarCenterX = PersonaSizes.AvatarSize.width.toPx() / 2f
            val avatarCenterY = PersonaSizes.AvatarSize.height.toPx() / 2f
            LineCoordinates(
                leftPoint  = Offset(avatarCenterX - width / 2f + horizontalShift, avatarCenterY),
                rightPoint = Offset(avatarCenterX + width / 2f + horizontalShift, avatarCenterY),
            )
        }

        EntryState(
            position         = index,
            message          = msg,
            participant      = participants[msg.senderId],
            lineCoordinates  = lineCoordinates,
            lineProgress     = Animatable(0f),
            avatarBackgroundScale = Animatable(0.6f).also { anim ->
                coroutineScope.launch {
                    anim.animateTo(1f, tween(300, easing = BetterEaseOutBack))
                }
            },
            avatarForegroundScale = Animatable(0f).also { anim ->
                coroutineScope.launch {
                    delay(160L)
                    anim.snapTo(0.8f)
                    anim.animateTo(1f, tween(150, easing = BetterEaseOutBack))
                }
            },
            messageHorizontalScale = Animatable(0.3f).also { anim ->
                coroutineScope.launch {
                    anim.animateTo(1f, tween(180, easing = BetterEaseOutBack))
                }
            },
            messageVerticalScale = Animatable(0.8f).also { anim ->
                coroutineScope.launch {
                    anim.animateTo(1f, tween(180, easing = BetterEaseOutBack))
                }
            },
            messageTextAlpha = Animatable(0f).also { anim ->
                coroutineScope.launch {
                    delay(100L)
                    anim.animateTo(1f, tween(130))
                }
            },
            punctuationScale = Animatable(0f).also { anim ->
                if (msg.text.endsWith('?')) {
                    coroutineScope.launch {
                        delay(130L)
                        anim.snapTo(0.4f)
                        anim.animateTo(1f, tween(100))
                    }
                }
            },
        )
    }

    /** Triggers the connecting-line grow animation on the previous entry. */
    private fun finalizeEntryState(state: EntryState) {
        // lineCoordinates are already in their final position (shift was applied at
        // createEntryState time), so we only need to start the grow animation here.
        coroutineScope.launch {
            state.lineProgress.animateTo(1f, tween(180))
        }
    }
}

// ---------------------------------------------------------------------------
// Internal helpers
// ---------------------------------------------------------------------------

private fun randomBetween(start: Float, end: Float): Float =
    start + Random.nextFloat() * (end - start)

private class EntryState(
    val position: Int,
    val message: TelegramMessage,
    val participant: ChatParticipant?,
    var lineCoordinates: LineCoordinates,
    val lineProgress: Animatable<Float, AnimationVector1D>,
    val avatarBackgroundScale: Animatable<Float, AnimationVector1D>,
    val avatarForegroundScale: Animatable<Float, AnimationVector1D>,
    val messageHorizontalScale: Animatable<Float, AnimationVector1D>,
    val messageVerticalScale: Animatable<Float, AnimationVector1D>,
    val messageTextAlpha: Animatable<Float, AnimationVector1D>,
    val punctuationScale: Animatable<Float, AnimationVector1D>,
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
