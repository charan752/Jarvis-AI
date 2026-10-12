package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ConversationRepository
import com.example.data.local.JarvisDatabase
import com.example.data.messaging.HomeArrivalManager
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import com.example.data.model.MessagingServiceType
import com.example.ui.ConversationHistoryViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("JARVIS", appName)
  }

  @Test
  fun testHomeArrival_authorizedServicesFiltering() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = HomeArrivalManager(context)

    // Only Instagram and Telegram authorized - PRODUCTION MODE (Zero fabrication)
    val prodReports = manager.checkAuthorizedServices(
      isInstagramAuthorized = true,
      isTelegramAuthorized = true,
      isWhatsAppAuthorized = false,
      isGmailAuthorized = false,
      unreadCountsEnabled = true,
      isSimulatedMode = false
    )

    assertEquals(2, prodReports.size)
    // Production responses must NEVER fabricate message counts
    assertEquals(0, prodReports.find { it.service == MessagingServiceType.INSTAGRAM }?.unreadCount)
    assertEquals(0, prodReports.find { it.service == MessagingServiceType.TELEGRAM }?.unreadCount)
    assertEquals("I couldn't check Instagram right now, sir.", prodReports.find { it.service == MessagingServiceType.INSTAGRAM }?.unavailableReason)
    assertEquals("I couldn't check Telegram right now, sir.", prodReports.find { it.service == MessagingServiceType.TELEGRAM }?.unavailableReason)
    assertFalse(prodReports.any { it.service == MessagingServiceType.WHATSAPP })
    assertFalse(prodReports.any { it.service == MessagingServiceType.GMAIL })

    // SIMULATED DEMO MODE - Clearly labeled as SIMULATED
    val simReports = manager.checkAuthorizedServices(
      isInstagramAuthorized = true,
      isTelegramAuthorized = true,
      isWhatsAppAuthorized = false,
      isGmailAuthorized = false,
      unreadCountsEnabled = true,
      isSimulatedMode = true
    )
    assertEquals(2, simReports.size)
    assertTrue(simReports.any { it.service == MessagingServiceType.INSTAGRAM && it.unreadCount == 2 && it.isSimulated })
    assertTrue(simReports.any { it.service == MessagingServiceType.TELEGRAM && it.unreadCount == 3 && it.isSimulated })
  }

  @Test
  fun testHomeArrival_privacyUnreadCountsDisabled() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = HomeArrivalManager(context)

    // If unreadCounts privacy setting is OFF, no unread counts are retrieved
    val reports = manager.checkAuthorizedServices(
      isInstagramAuthorized = true,
      isTelegramAuthorized = true,
      isWhatsAppAuthorized = true,
      isGmailAuthorized = true,
      unreadCountsEnabled = false
    )

    assertTrue(reports.isEmpty())
  }

  @Test
  fun testRoomDatabase_storeAndRetrieveConversationalHistory() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, JarvisDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    try {
      val repository = ConversationRepository(db.conversationDao())

      val msg1 = ChatMessage(
        id = "msg-101",
        sender = MessageSender.USER,
        text = "Hello JARVIS, remember my favorite color is blue.",
        timestamp = 1000L
      )
      val msg2 = ChatMessage(
        id = "msg-102",
        sender = MessageSender.JARVIS,
        text = "Understood, Sir. Favorite color blue recorded in neural matrix.",
        timestamp = 2000L
      )

      repository.saveMessage(msg1)
      repository.saveMessage(msg2)

      val all = repository.getAllMessages()
      assertEquals(2, all.size)
      assertEquals("msg-101", all[0].id)
      assertEquals("Hello JARVIS, remember my favorite color is blue.", all[0].text)
      assertEquals("msg-102", all[1].id)

      val count = repository.getMessageCount()
      assertEquals(2, count)

      // Test search
      val searchBlue = repository.searchMessages("blue")
      assertEquals(2, searchBlue.size)

      val searchNonExistent = repository.searchMessages("xyznonexistent")
      assertEquals(0, searchNonExistent.size)

      // Test recent turns
      val recent = repository.getRecentMessages(1)
      assertEquals(1, recent.size)
      assertEquals("msg-102", recent[0].id)

      // Test memory transcript format
      val transcript = repository.getConversationContextForMemory(5)
      assertTrue(transcript.contains("User: Hello JARVIS, remember my favorite color is blue."))
      assertTrue(transcript.contains("JARVIS: Understood, Sir. Favorite color blue recorded in neural matrix."))

      // Test deletion
      repository.deleteMessage("msg-101")
      val afterDelete = repository.getAllMessages()
      assertEquals(1, afterDelete.size)
      assertEquals("msg-102", afterDelete[0].id)

      // Test clear
      repository.clearHistory()
      val afterClear = repository.getAllMessages()
      assertTrue(afterClear.isEmpty())
    } finally {
      db.close()
    }
  }

  @Test
  fun testConversationHistoryViewModel_storeRetrieveAndContext() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val db = Room.inMemoryDatabaseBuilder(application, JarvisDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    try {
      val repository = ConversationRepository(db.conversationDao())
      val viewModel = ConversationHistoryViewModel(application, repository)

      // Store messages via ViewModel
      viewModel.saveMessage(MessageSender.USER, "What is the status of project Stark?")
      viewModel.saveMessage(MessageSender.JARVIS, "All systems operational, Sir. Reactor output at 100%.")

      val history = viewModel.retrieveHistory()
      assertEquals(2, history.size)
      assertEquals(MessageSender.USER, history[0].sender)
      assertEquals(MessageSender.JARVIS, history[1].sender)

      // Test memory transcript for AI injection
      val memoryContext = viewModel.getMemoryContextForAI(5)
      assertTrue(memoryContext.contains("User: What is the status of project Stark?"))
      assertTrue(memoryContext.contains("JARVIS: All systems operational, Sir. Reactor output at 100%."))

      // Test repository search / query
      val recentMessages = viewModel.retrieveRecentHistory(1)
      assertEquals(1, recentMessages.size)
      assertEquals(MessageSender.JARVIS, recentMessages[0].sender)

      // Test clear
      repository.clearHistory()
      val clearedHistory = viewModel.retrieveHistory()
      assertTrue(clearedHistory.isEmpty())
    } finally {
      db.close()
    }
  }
}
