package com.example.capture.sms

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.example.capture.TransactionCapturePipeline
import com.example.data.room.AppDatabase
import com.example.model.CaptureSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScanResult(
  val totalScanned: Int,
  val candidatesFound: Int,
  val autoSaved: Int,
  val queuedForReview: Int,
  val duplicatesSkipped: Int
)

object SmsHistoricalScanner {

  suspend fun scanRecentSms(
    context: Context,
    daysBack: Int = 30,
    onProgress: (Int, Int) -> Unit = { _, _ -> }
  ): ScanResult = withContext(Dispatchers.IO) {
    val db = AppDatabase.getDatabase(context)
    val pipeline = TransactionCapturePipeline(db)

    val contentResolver = context.contentResolver
    val uri = Uri.parse("content://sms/inbox")
    val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
    val cutoffTime = System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000L)
    val selection = "${Telephony.Sms.DATE} >= ?"
    val selectionArgs = arrayOf(cutoffTime.toString())

    var totalScanned = 0
    var candidatesFound = 0
    var autoSaved = 0
    var queuedForReview = 0
    var duplicatesSkipped = 0

    try {
      val cursor = contentResolver.query(
        uri,
        projection,
        selection,
        selectionArgs,
        "${Telephony.Sms.DATE} DESC LIMIT 300"
      )

      cursor?.use {
        val total = it.count
        val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
        val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
        val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

        while (it.moveToNext()) {
          totalScanned++
          val sender = it.getString(addressIdx) ?: "SMS"
          val body = it.getString(bodyIdx) ?: ""
          val date = it.getLong(dateIdx)

          val createdTx = pipeline.processIncomingMessage(
            rawText = body,
            source = CaptureSource.SMS,
            sourceAppOrSender = sender,
            occurredAt = date
          )

          if (createdTx != null) {
            candidatesFound++
            if (createdTx.status == com.example.model.TransactionStatus.CONFIRMED) {
              autoSaved++
            } else {
              queuedForReview++
            }
          }
          onProgress(totalScanned, total)
        }
      }
    } catch (e: Exception) {
      // SecurityException if permission not granted
    }

    ScanResult(
      totalScanned = totalScanned,
      candidatesFound = candidatesFound,
      autoSaved = autoSaved,
      queuedForReview = queuedForReview,
      duplicatesSkipped = duplicatesSkipped
    )
  }
}
