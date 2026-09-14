package com.example.capture.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.capture.TransactionCapturePipeline
import com.example.data.room.AppDatabase
import com.example.model.CaptureSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context?, intent: Intent?) {
    if (context == null || intent == null) return
    if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
    if (messages.isNullOrEmpty()) return

    val sender = messages[0].originatingAddress ?: "Unknown Sender"
    val timestamp = messages[0].timestampMillis.takeIf { it > 0 } ?: System.currentTimeMillis()

    val fullBody = StringBuilder()
    for (msg in messages) {
      msg.messageBody?.let { fullBody.append(it) }
    }
    val rawText = fullBody.toString()
    if (rawText.isBlank()) return

    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = AppDatabase.getDatabase(context)
        val pipeline = TransactionCapturePipeline(db)
        pipeline.processIncomingMessage(
          rawText = rawText,
          source = CaptureSource.SMS,
          sourceAppOrSender = sender,
          occurredAt = timestamp
        )
      } catch (e: Exception) {
        // Safe fail
      } finally {
        pendingResult.finish()
      }
    }
  }
}
