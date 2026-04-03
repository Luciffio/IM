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

    /** Returns the top-of-entry drawing offset for the connecting line. */
    fun getTopDrawingOffset(scope: CacheDrawScope, entry: PersonaEntry): Offset = with(scope) {
        return if (entry.message.isOutgoing) {
            val shift = size.width - (PersonaSizes.OutgoingCenterX.toPx() * 2f)
            Offset(x = shift, y = 0f)
        } else {
            Offset.Zero
        }
    }

    /** Returns the bottom-of-entry drawing offset for the connecting line. */
    fun getBottomDrawingOffset(scope: CacheDrawScope, entry: PersonaEntry): Offset = with(scope) {
        val verticalShift = size.height + EntrySpacing.toPx()
        return if (entry.message.isOutgoing) {
            val shift = size.width - (PersonaSizes.OutgoingCenterX.toPx() * 2f)
            Offset(x = shift, y = verticalShift)
        } else {
            Offset(x = 0f, y = verticalShift)
        }
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
    val density = LocalDensity.current
    val scope   = rememberCoroutineScope()
    return remember(density) {
        PersonaChatState(density, scope, participants, messages)
    }
}

@Stable
class PersonaChatState internal constructor(
    private val density: Density,
    private val coroutineScope: CoroutineScope,
    private val participants: Map<Long, ChatParticipant>,
    private val allMessages: List<TelegramMessage>,
) {
    private var cursor = 0
    private val entryStates = mutableListOf<EntryState>()
    private val _entries = mutableStateOf<List<PersonaEntry>>(emptyList())
    val entries: List<PersonaEntry> get() = _entries.value

    /** Show the next message from the conversation. Call repeatedly (e.g., on a button press). */
    fun advance() {
        if (cursor >= allMessages.size) {
            // Loop back to start
            cursor = 0
            entryStates.clear()
        }

        val msg = allMessages[cursor++]
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

    private fun createEntryState(index: Int, msg: TelegramMessage): EntryState = with(density) {
        val width = randomBetween(
            PersonaSizes.MinLineWidth.toPx(),
            PersonaSizes.MaxLineWidth.toPx(),
        )

        val lineCoordinates = if (msg.isOutgoing) {
            val cx = PersonaSizes.OutgoingCenterX.toPx()
            val cy = PersonaSizes.OutgoingCenterY.toPx()
            LineCoordinates(
                leftPoint  = Offset(cx - width / 2f, cy),
                rightPoint = Offset(cx + width / 2f, cy),
            )
        } else {
            val avatarCenterX = PersonaSizes.AvatarSize.width.toPx() / 2f
            val avatarCenterY = PersonaSizes.AvatarSize.height.toPx() / 2f
            LineCoordinates(
                leftPoint  = Offset(avatarCenterX - width / 2f, avatarCenterY),
                rightPoint = Offset(avatarCenterX + width / 2f, avatarCenterY),
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
    private fun finalizeEntryState(state: EntryState) = with(density) {
        val direction       = if (state.position % 2 == 0) 1f else -1f
        val horizontalShift = if (state.position > 0) {
            randomBetween(PersonaSizes.MinLineShift.toPx(), PersonaSizes.MaxLineShift.toPx()) * direction
        } else 0f

        val offset = if (state.message.isOutgoing) Offset.Zero
        else Offset(horizontalShift, 0f)

        state.lineCoordinates = state.lineCoordinates.copy(
            leftPoint  = state.lineCoordinates.leftPoint  + offset,
            rightPoint = state.lineCoordinates.rightPoint + offset,
        )

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
