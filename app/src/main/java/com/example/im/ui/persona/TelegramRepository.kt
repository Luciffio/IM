package com.example.im.ui.persona

// ---------------------------------------------------------------------------
// Telegram client-side stubs
//
// These interfaces and enums mirror the Telegram Bot/TDLib API surface.
// Swap the FakeTelegramRepository with a real TDLib / Telegram4J / Telethon
// wrapper when integrating the actual client.
// ---------------------------------------------------------------------------

// ── Media types ───────────────────────────────────────────────────────────────

/** All content types that a Telegram message can carry. */
enum class TgMediaType {
    TEXT,
    PHOTO,
    VIDEO,
    VOICE,
    AUDIO,
    DOCUMENT,
    STICKER,
    GIF,
    LOCATION,
    CONTACT,
    POLL,
}

// ── Message delivery status ───────────────────────────────────────────────────

/** Mirrors TDLib's MessageSendingState / read receipts. */
enum class TgDeliveryStatus {
    PENDING,    // in send queue
    SENT,       // server acknowledged (single tick)
    DELIVERED,  // reached recipient device (double tick)
    READ,       // read receipt received (double tick, colored)
    FAILED,     // send error
}

// ── Extended message model ────────────────────────────────────────────────────

/**
 * Full Telegram message representation.
 *
 * [mediaType] defaults to [TgMediaType.TEXT]; set to PHOTO / VIDEO etc. to
 * trigger the corresponding media renderer stub in [PersonaIncomingMessage] /
 * [PersonaOutgoingMessage].
 *
 * [replyToMessageId] is non-null when the message is a reply; the UI should
 * fetch the quoted message from the repository and render a reply bubble header.
 *
 * [deliveryStatus] is only meaningful for outgoing messages.
 */
data class TgMessage(
    val id:              Long,
    val chatId:          Long,
    val text:            String,
    val senderId:        Long,
    val senderName:      String,
    val timestamp:       Long,
    val isOutgoing:      Boolean,
    val mediaType:       TgMediaType        = TgMediaType.TEXT,
    /** Filled only when mediaType != TEXT — remote URL or local cache path. */
    val mediaUrl:        String?            = null,
    val deliveryStatus:  TgDeliveryStatus   = TgDeliveryStatus.READ,
    val replyToId:       Long?              = null,
    val editedAt:        Long?              = null,
    val isPinned:        Boolean            = false,
    val reactions:       Map<String, Int>   = emptyMap(),
)

/** Adapter: converts a [TgMessage] into the legacy [TelegramMessage] used by the current UI. */
fun TgMessage.toLegacy(): TelegramMessage = TelegramMessage(
    id         = id,
    text       = text,
    senderId   = senderId,
    senderName = senderName,
    timestamp  = timestamp,
    isOutgoing = isOutgoing,
    hasImage   = mediaType == TgMediaType.PHOTO,
)

// ── Chat model ────────────────────────────────────────────────────────────────

enum class TgChatType { PRIVATE, GROUP, SUPERGROUP, CHANNEL }

data class TgChat(
    val id:          Long,
    val title:       String,
    val type:        TgChatType,
    val photoUrl:    String?  = null,
    val memberCount: Int      = 0,
    val isVerified:  Boolean  = false,
    val isMuted:     Boolean  = false,
)

// ── User / contact model ──────────────────────────────────────────────────────

data class TgUser(
    val id:           Long,
    val firstName:    String,
    val lastName:     String  = "",
    val username:     String  = "",
    val phoneNumber:  String  = "",
    val photoUrl:     String? = null,
    val isBot:        Boolean = false,
    val isOnline:     Boolean = false,
    val lastSeen:     Long?   = null,
)

// ── Repository interface ──────────────────────────────────────────────────────

/**
 * All Telegram data access goes through this interface.
 *
 * Real implementation: wrap TDLib's Client or a Kotlin Telegram library.
 * Test/preview implementation: [FakeTelegramRepository].
 */
interface TelegramRepository {

    // ── Auth ─────────────────────────────────────────────────────────────────

    /** Returns the authenticated user, or null if not logged in. */
    suspend fun getMe(): TgUser?

    // ── Chats ─────────────────────────────────────────────────────────────────

    /** Returns the N most-recently-active chats. */
    suspend fun getChatList(limit: Int = 50): List<TgChat>

    suspend fun getChat(chatId: Long): TgChat?

    // ── Messages ──────────────────────────────────────────────────────────────

    /**
     * Load [limit] messages from [chatId] ending at [fromMessageId]
     * (pass 0 for the newest).
     */
    suspend fun getMessages(
        chatId:        Long,
        fromMessageId: Long  = 0,
        limit:         Int   = 40,
    ): List<TgMessage>

    /** Send a plain-text message. Returns the optimistic stub message. */
    suspend fun sendMessage(chatId: Long, text: String): TgMessage

    /** Send a photo. [localPath] is the device file URI. */
    suspend fun sendPhoto(chatId: Long, localPath: String, caption: String = ""): TgMessage

    /** Delete a message (only if the current user is the author or admin). */
    suspend fun deleteMessage(chatId: Long, messageId: Long): Boolean

    /** Toggle a reaction emoji on a message. */
    suspend fun toggleReaction(chatId: Long, messageId: Long, emoji: String)

    /** Forward a message to another chat. */
    suspend fun forwardMessage(fromChatId: Long, messageId: Long, toChatId: Long): TgMessage

    // ── Users ─────────────────────────────────────────────────────────────────

    suspend fun getUser(userId: Long): TgUser?

    suspend fun getChatMembers(chatId: Long): List<TgUser>
}

// ── Fake (preview / demo) implementation ─────────────────────────────────────

/**
 * Returns sample data so the Compose Preview and the demo [PersonaChatScreen]
 * work without any network access.
 *
 * Replace with a real TDLib-backed implementation for production.
 */
class FakeTelegramRepository : TelegramRepository {

    private val me = TgUser(id = 0L, firstName = "Ren", lastName = "Amamiya", username = "joker")

    override suspend fun getMe() = me

    override suspend fun getChatList(limit: Int) = listOf(
        TgChat(1L, "Ann",    TgChatType.PRIVATE,    memberCount = 2),
        TgChat(2L, "Ryuji",  TgChatType.PRIVATE,    memberCount = 2),
        TgChat(3L, "Yusuke", TgChatType.PRIVATE,    memberCount = 2),
        TgChat(4L, "Phantom Thieves", TgChatType.GROUP, memberCount = 5),
    )

    override suspend fun getChat(chatId: Long) = getChatList().firstOrNull { it.id == chatId }

    override suspend fun getMessages(chatId: Long, fromMessageId: Long, limit: Int) =
        SampleMessages.map { m ->
            TgMessage(
                id         = m.id,
                chatId     = chatId,
                text       = m.text,
                senderId   = m.senderId,
                senderName = m.senderName,
                timestamp  = m.timestamp,
                isOutgoing = m.isOutgoing,
                mediaType  = if (m.hasImage) TgMediaType.PHOTO else TgMediaType.TEXT,
            )
        }

    override suspend fun sendMessage(chatId: Long, text: String) = TgMessage(
        id         = System.currentTimeMillis(),
        chatId     = chatId,
        text       = text,
        senderId   = 0L,
        senderName = "Ren",
        timestamp  = System.currentTimeMillis(),
        isOutgoing = true,
        deliveryStatus = TgDeliveryStatus.PENDING,
    )

    override suspend fun sendPhoto(chatId: Long, localPath: String, caption: String) =
        sendMessage(chatId, caption).copy(mediaType = TgMediaType.PHOTO, mediaUrl = localPath)

    override suspend fun deleteMessage(chatId: Long, messageId: Long) = true

    override suspend fun toggleReaction(chatId: Long, messageId: Long, emoji: String) = Unit

    override suspend fun forwardMessage(fromChatId: Long, messageId: Long, toChatId: Long) =
        sendMessage(toChatId, "forwarded")

    override suspend fun getUser(userId: Long) =
        SampleParticipants[userId]?.let {
            TgUser(id = it.id, firstName = it.name)
        }

    override suspend fun getChatMembers(chatId: Long) =
        SampleParticipants.values.map { TgUser(id = it.id, firstName = it.name) }
}
