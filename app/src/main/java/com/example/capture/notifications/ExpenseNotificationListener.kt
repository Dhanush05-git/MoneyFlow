package com.example.capture.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.capture.TransactionCapturePipeline
import com.example.data.room.AppDatabase
import com.example.model.CaptureSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExpenseNotificationListener : NotificationListenerService() {

  private val serviceScope = CoroutineScope(Dispatchers.IO)

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    if (sbn == null) return
    val packageName = sbn.packageName ?: return

    val extras = sbn.notification?.extras ?: return
    val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
    val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

    val fullContent = listOf(title, text, bigText)
      .filter { it.isNotBlank() }
      .distinct()
      .joinToString(" ")

    if (fullContent.isBlank()) return

    // Filter to any UPI, banking app, or clear monetary transaction on device
    if (!SupportedPaymentApps.isFinancialNotification(packageName, fullContent)) {
      return
    }

    val appName = SupportedPaymentApps.getAppName(this, packageName)
    val postTime = sbn.postTime.takeIf { it > 0 } ?: System.currentTimeMillis()

    serviceScope.launch {
      try {
        val db = AppDatabase.getDatabase(applicationContext)
        val pipeline = TransactionCapturePipeline(db, applicationContext)
        pipeline.processIncomingMessage(
          rawText = fullContent,
          source = CaptureSource.NOTIFICATION,
          sourceAppOrSender = appName,
          occurredAt = postTime
        )
      } catch (e: Exception) {
        // Safe fail
      }
    }
  }
}
