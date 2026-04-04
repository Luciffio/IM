package com.example.im.ui.persona

import androidx.compose.ui.graphics.Color
import java.time.LocalDate

// Base timestamp: 2026-04-03 10:00:00 UTC (epoch millis)
private const val BASE_TS = 1743674400000L

// ---------------------------------------------------------------------------
// Telegram-like data model stubs
// ---------------------------------------------------------------------------

/** Represents a single participant in the group chat (non-outgoing side). */
data class ChatParticipant(
    val id: Long,
    val name: String,
    val color: Color,
    // In a real Telegram integration, replace with an actual drawable res or URL.
    // Set to null to render a solid-color avatar placeholder.
    val avatarRes: Int? = null,
)

/**
 * Telegram message — UI-layer model.
 *
 * This is the *display* model used by Compose components; it is intentionally
 * separate from [TgMessage] (the full Telegram API model in TelegramRepository.kt)
 * so that Compose previews work without any network dependency.
 *
 * When integrating TDLib: call [TgMessage.toLegacy()] to produce this.
 */
data class TelegramMessage(
    val id:             Long,
    val text:           String,
    /** 0L = current user (outgoing). */
    val senderId:       Long,
    val senderName:     String,
    val timestamp:      Long,
    val isOutgoing:     Boolean,
    /** True when the message contains an image attachment. */
    val hasImage:       Boolean            = false,
    /** Tint for the image placeholder; replace with Coil AsyncImage + real URL. */
    val imageColor:     Color              = Color.Gray,
    /**
     * Delivery status shown next to outgoing messages (✓ / ✓✓ / ✓✓ colored).
     * Stub default = READ so sample data looks delivered.
     */
    val deliveryStatus: TgDeliveryStatus   = TgDeliveryStatus.READ,
    /** Non-null when this is a reply — use to fetch & show quoted message header. */
    val replyToId:      Long?              = null,
    /** Non-null when the message was edited. */
    val editedAt:       Long?              = null,
)

// ---------------------------------------------------------------------------
// Sample data (replace with real Telegram API responses)
// ---------------------------------------------------------------------------

val SampleParticipants: Map<Long, ChatParticipant> = mapOf(
    1L to ChatParticipant(1L, "Ann",    P5ColorAnn),
    2L to ChatParticipant(2L, "Ryuji",  P5ColorRyuji),
    3L to ChatParticipant(3L, "Yusuke", P5ColorYusuke),
)

// ---------------------------------------------------------------------------
// Chat list model
// ---------------------------------------------------------------------------

data class ChatPreview(
    val id:           Long,
    val name:         String,
    val lastMessage:  String,
    val date:         LocalDate,
    val hasUnread:    Boolean = false,
    val color:        Color   = Color(0xFFFE93C9),
)

val SampleChatList = listOf(
    ChatPreview(1L, "Ann",    "Hello, Senpai",               LocalDate.of(2026, 3, 3),  hasUnread = false, color = P5ColorAnn),
    ChatPreview(2L, "Ryuji",  "After school tomorrow",       LocalDate.of(2026, 3, 2),  hasUnread = true,  color = P5ColorRyuji),
    ChatPreview(3L, "Yusuke", "I'm glad you made it back…",  LocalDate.of(2026, 2, 13), hasUnread = false, color = P5ColorYusuke),
    ChatPreview(4L, "Ann",    "Tomorrow's finally the day",  LocalDate.of(2026, 2, 2),  hasUnread = false, color = P5ColorAnn),
    ChatPreview(5L, "Ryuji",  "If he's comin', it'll be today", LocalDate.of(2026, 2, 2), hasUnread = false, color = P5ColorRyuji),
    ChatPreview(6L, "Yusuke", "It's already February!",      LocalDate.of(2026, 2, 1),  hasUnread = false, color = P5ColorYusuke),
    ChatPreview(7L, "Ann",    "Don't forget to study!",      LocalDate.of(2026, 1, 28), hasUnread = false, color = P5ColorAnn),
)

val SampleMessages = listOf(
    TelegramMessage(1L,  "We have to find them tomorrow for sure. This is the only lead we have right now.",           1L, "Ann",    BASE_TS,              false),
    TelegramMessage(2L,  "Yes. It is highly likely that this part-time solicitor is somehow related to the mafia.",    3L, "Yusuke", BASE_TS + 180_000,    false),
    TelegramMessage(3L,  "If we tail him, he may lead us straight back to his boss.",                                  3L, "Yusuke", BASE_TS + 240_000,    false),
    TelegramMessage(4L,  "He talked to Iida and Nishiyama over at Central Street, right?",                             2L, "Ryuji",  BASE_TS + 420_000,    false),
    TelegramMessage(5L,  "Look — I photographed the meeting spot.",                                                    3L, "Yusuke", BASE_TS + 480_000,    false,
                         hasImage = true, imageColor = P5ColorYusuke),
    TelegramMessage(6L,  "Morgana, I choose you.",                                                                     0L, "Me",     BASE_TS + 720_000,    true),
    TelegramMessage(7L,  "That's not a bad idea. Cats have nine lives, right? Morgana can spare one for this.",        1L, "Ann",    BASE_TS + 900_000,    false),
    TelegramMessage(8L,  "Wouldn't the mafia get caught off guard if they had a cat coming to deliver for 'em?",       2L, "Ryuji",  BASE_TS + 1_020_000,  false),
    TelegramMessage(9L,  "In other words, Maaku will be going. I have no objections.",                                 3L, "Yusuke", BASE_TS + 1_140_000,  false),
    TelegramMessage(10L, "Tricking people and using that as blackmail… These bastards are true cowards.",              3L, "Yusuke", BASE_TS + 1_260_000,  false),
    TelegramMessage(11L, "It's kinda scary to think people like that are all around us in this city...",               1L, "Ann",    BASE_TS + 1_380_000,  false),
    TelegramMessage(12L, "Well guys, we gotta brace ourselves. We're up against a serious criminal here.",             2L, "Ryuji",  BASE_TS + 1_500_000,  false),
)
