package com.example.data.local

import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import com.example.data.model.ToolCallInfo
import com.example.data.model.ToolExecutionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ConversationRepository(private val conversationDao: ConversationDao) {

    val messagesFlow: Flow<List<ChatMessage>> = conversationDao.getAllMessagesFlow()
        .map { list -> list.map { it.toChatMessage() } }

    val messageCountFlow: Flow<Int> = conversationDao.getMessageCountFlow()

    suspend fun getAllMessages(): List<ChatMessage> = withContext(Dispatchers.IO) {
        conversationDao.getAllMessages().map { it.toChatMessage() }
    }

    fun getRecentMessagesFlow(limit: Int): Flow<List<ChatMessage>> =
        conversationDao.getRecentMessagesFlow(limit)
            .map { list -> list.map { it.toChatMessage() } }

    suspend fun getRecentMessages(limit: Int): List<ChatMessage> = withContext(Dispatchers.IO) {
        conversationDao.getRecentMessages(limit).map { it.toChatMessage() }
    }

    fun searchMessagesFlow(query: String): Flow<List<ChatMessage>> =
        conversationDao.searchMessagesFlow(query)
            .map { list -> list.map { it.toChatMessage() } }

    suspend fun searchMessages(query: String): List<ChatMessage> = withContext(Dispatchers.IO) {
        conversationDao.searchMessages(query).map { it.toChatMessage() }
    }

    suspend fun getMessageCount(): Int = withContext(Dispatchers.IO) {
        conversationDao.getMessageCount()
    }

    suspend fun saveMessage(chatMessage: ChatMessage): Long = withContext(Dispatchers.IO) {
        conversationDao.insertMessage(chatMessage.toEntity())
    }

    suspend fun saveMessages(messages: List<ChatMessage>) = withContext(Dispatchers.IO) {
        conversationDao.insertMessages(messages.map { it.toEntity() })
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        conversationDao.clearAllMessages()
    }

    suspend fun deleteMessage(messageId: String) = withContext(Dispatchers.IO) {
        conversationDao.deleteMessageByMessageId(messageId)
    }

    /**
     * Formats recent conversational history into a structured transcript
     * to inject into LLM prompts or memory views as persistent memory context.
     */
    suspend fun getConversationContextForMemory(limit: Int = 10): String = withContext(Dispatchers.IO) {
        val recent = conversationDao.getRecentMessages(limit).reversed()
        if (recent.isEmpty()) return@withContext ""

        buildString {
            appendLine("=== PERSISTENT CONVERSATIONAL MEMORY TRANSCRIPT ===")
            recent.forEach { entity ->
                val senderTag = when (entity.sender) {
                    "USER" -> "User"
                    "JARVIS" -> "JARVIS"
                    "SYSTEM" -> "System"
                    else -> entity.sender
                }
                appendLine("$senderTag: ${entity.text.trim()}")
            }
            appendLine("==================================================")
        }.trim()
    }

    private fun ConversationMessageEntity.toChatMessage(): ChatMessage {
        val messageSender = try {
            MessageSender.valueOf(this.sender)
        } catch (_: Exception) {
            MessageSender.JARVIS
        }

        val toolCallInfo = if (!toolName.isNullOrEmpty()) {
            ToolCallInfo(
                toolName = toolName,
                argumentsJson = toolArgumentsJson ?: "{}",
                summary = toolSummary ?: "",
                status = ToolExecutionStatus.COMPLETED,
                resultMessage = toolResultMessage ?: "",
                isSimulated = isSimulatedTool
            )
        } else null

        return ChatMessage(
            id = messageId,
            sender = messageSender,
            text = text,
            timestamp = timestamp,
            toolCall = toolCallInfo,
            language = language
        )
    }

    private fun ChatMessage.toEntity(): ConversationMessageEntity {
        return ConversationMessageEntity(
            messageId = id,
            sender = sender.name,
            text = text,
            timestamp = timestamp,
            toolName = toolCall?.toolName,
            toolArgumentsJson = toolCall?.argumentsJson,
            toolSummary = toolCall?.summary,
            toolResultMessage = toolCall?.resultMessage,
            isSimulatedTool = toolCall?.isSimulated ?: false,
            language = language
        )
    }
}
