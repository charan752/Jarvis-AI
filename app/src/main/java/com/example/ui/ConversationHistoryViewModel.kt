package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ConversationRepository
import com.example.data.local.JarvisDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Dedicated ViewModel for managing persistent conversational history in Room.
 * Gives JARVIS long-term memory across sessions, application restarts, and reboots.
 */
class ConversationHistoryViewModel(
    application: Application,
    private val repository: ConversationRepository = ConversationRepository(
        JarvisDatabase.getDatabase(application).conversationDao()
    )
) : AndroidViewModel(application) {

    // Reactive flow of all stored messages from Room
    val conversationHistory: StateFlow<List<ChatMessage>> = repository.messagesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Total count of conversation turns persisted in Room
    val messageCount: StateFlow<Int> = repository.messageCountFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 0
        )

    // Search query state for filtering history
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filtered search results combining search query with conversation history
    val searchResults: StateFlow<List<ChatMessage>> = combine(
        conversationHistory,
        _searchQuery
    ) { history, query ->
        if (query.isBlank()) {
            history
        } else {
            val lower = query.lowercase().trim()
            history.filter { message ->
                message.text.lowercase().contains(lower) ||
                        message.sender.name.lowercase().contains(lower) ||
                        (message.toolCall?.toolName?.lowercase()?.contains(lower) == true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // Reactive persistent memory transcript for LLM context injection
    val persistentMemoryContext: StateFlow<String> = conversationHistory
        .combine(_searchQuery) { history, _ ->
            formatTranscript(history.takeLast(10))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ""
        )

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    /**
     * Stores a new message into Room persistent storage.
     */
    fun storeMessage(chatMessage: ChatMessage) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                repository.saveMessage(chatMessage)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Suspend version to store a message directly within an active coroutine scope.
     */
    suspend fun saveMessage(chatMessage: ChatMessage) {
        repository.saveMessage(chatMessage)
    }

    /**
     * Convenience suspend method to store a message from raw parameters.
     */
    suspend fun saveMessage(
        sender: MessageSender,
        text: String,
        language: String = "English"
    ): String {
        val id = UUID.randomUUID().toString()
        val message = ChatMessage(
            id = id,
            sender = sender,
            text = text,
            timestamp = System.currentTimeMillis(),
            language = language
        )
        repository.saveMessage(message)
        return id
    }

    /**
     * Convenience method to store a message from raw parameters.
     */
    fun storeMessage(
        sender: MessageSender,
        text: String,
        language: String = "English"
    ): String {
        val id = UUID.randomUUID().toString()
        val message = ChatMessage(
            id = id,
            sender = sender,
            text = text,
            timestamp = System.currentTimeMillis(),
            language = language
        )
        storeMessage(message)
        return id
    }

    /**
     * Deletes a specific message by its unique messageId.
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
            _statusMessage.value = "Conversation record deleted from neural memory."
        }
    }

    /**
     * Clears all conversational history from the Room database.
     */
    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _statusMessage.value = "Conversation memory cleared."
        }
    }

    /**
     * Updates the current search query.
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Retrieves all messages synchronously in a coroutine.
     */
    suspend fun retrieveHistory(): List<ChatMessage> {
        return repository.getAllMessages()
    }

    /**
     * Retrieves recent messages for memory injection or display.
     */
    suspend fun retrieveRecentHistory(limit: Int = 10): List<ChatMessage> {
        return repository.getRecentMessages(limit)
    }

    /**
     * Formats recent turns into persistent memory context string.
     */
    suspend fun getMemoryContextForAI(limit: Int = 10): String {
        return repository.getConversationContextForMemory(limit)
    }

    fun dismissStatusMessage() {
        _statusMessage.value = null
    }

    private fun formatTranscript(messages: List<ChatMessage>): String {
        if (messages.isEmpty()) return ""
        return buildString {
            appendLine("=== PERSISTENT CONVERSATIONAL MEMORY TRANSCRIPT ===")
            messages.forEach { msg ->
                val role = when (msg.sender) {
                    MessageSender.USER -> "User"
                    MessageSender.JARVIS -> "JARVIS"
                    MessageSender.SYSTEM -> "System"
                    MessageSender.TOOL -> "Tool"
                }
                appendLine("$role: ${msg.text.trim()}")
            }
            appendLine("==================================================")
        }.trim()
    }
}
