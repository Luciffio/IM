package com.example.im.ui.persona

import androidx.compose.ui.graphics.Color

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

/** Single chat message — maps 1-to-1 with Telegram's Message model. */
data class TelegramMessage(
    val id: Long,
    val text: String,
    /** 0L for the current user (outgoing). */
    val senderId: Long,
    val senderName: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
)

// ---------------------------------------------------------------------------
// Sample data (replace with real Telegram API responses)
// ---------------------------------------------------------------------------

val SampleParticipants: Map<Long, ChatParticipant> = mapOf(
    1L to ChatParticipant(1L, "Ann",    P5ColorAnn),
    2L to ChatParticipant(2L, "Ryuji",  P5ColorRyuji),
    3L to ChatParticipant(3L, "Yusuke", P5ColorYusuke),
)

val SampleMessages = listOf(
    TelegramMessage(1L,  "We have to find them tomorrow for sure. This is the only lead we have right now.",           1L, "Ann",    1_000L, false),
    TelegramMessage(2L,  "Yes. It is highly likely that this part-time solicitor is somehow related to the mafia.",    3L, "Yusuke", 2_000L, false),
    TelegramMessage(3L,  "If we tail him, he may lead us straight back to his boss.",                                  3L, "Yusuke", 3_000L, false),
    TelegramMessage(4L,  "He talked to Iida and Nishiyama over at Central Street, right?",                             2L, "Ryuji",  4_000L, false),
    TelegramMessage(5L,  "Indeed, it seems that is where our target waits. But then... who should be the one to go?", 3L, "Yusuke", 5_000L, false),
    TelegramMessage(6L,  "Morgana, I choose you.",                                                                     0L, "Me",     6_000L, true),
    TelegramMessage(7L,  "That's not a bad idea. Cats have nine lives, right? Morgana can spare one for this.",        1L, "Ann",    7_000L, false),
    TelegramMessage(8L,  "Wouldn't the mafia get caught off guard if they had a cat coming to deliver for 'em?",       2L, "Ryuji",  8_000L, false),
    TelegramMessage(9L,  "In other words, Maaku will be going. I have no objections.",                                 3L, "Yusuke", 9_000L, false),
    TelegramMessage(10L, "Tricking people and using that as blackmail… These bastards are true cowards.",              3L, "Yusuke", 10_000L, false),
    TelegramMessage(11L, "It's kinda scary to think people like that are all around us in this city...",               1L, "Ann",    11_000L, false),
    TelegramMessage(12L, "Well guys, we gotta brace ourselves. We're up against a serious criminal here.",             2L, "Ryuji",  12_000L, false),
)
