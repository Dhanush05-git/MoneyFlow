package com.example.capture

import com.example.model.CaptureSource
import com.example.model.PaymentMethod
import com.example.model.TransactionEntity
import com.example.model.TransactionStatus
import com.example.model.TransactionType
import java.security.MessageDigest

data class TransactionCandidate(
  val amountPaise: Long,
  val currency: String = "INR",
  val type: TransactionType = TransactionType.EXPENSE,
  val occurredAt: Long = System.currentTimeMillis(),
  val merchantRaw: String = "",
  val merchantNormalized: String = "Unknown",
  val categoryCandidate: String = "Other",
  val accountCandidate: String = "Primary Account",
  val paymentMethod: PaymentMethod = PaymentMethod.UPI,
  val source: CaptureSource = CaptureSource.SMS,
  val sourceApp: String? = null,
  val reference: String? = null,
  val rawMessageSnippet: String = "",
  val confidenceScore: Int = 90,
  val confidenceReasons: List<String> = emptyList(),
  val isPotentialDuplicate: Boolean = false,
  val duplicateReason: String? = null
) {
  val fingerprint: String
    get() {
      val raw = "${amountPaise}_${merchantNormalized.lowercase().trim()}_${reference ?: ""}_${occurredAt / (1000 * 60 * 15)}"
      val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
      return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }

  fun toEntity(autoApproveThreshold: Int = 95): TransactionEntity {
    val isConfident = confidenceScore >= autoApproveThreshold && !isPotentialDuplicate
    val status = if (isConfident) TransactionStatus.CONFIRMED else TransactionStatus.NEEDS_REVIEW
    return TransactionEntity(
      amountPaise = amountPaise,
      currency = currency,
      type = type,
      status = status,
      occurredAt = occurredAt,
      merchantName = merchantNormalized,
      categoryName = categoryCandidate,
      accountName = accountCandidate,
      paymentMethod = paymentMethod,
      source = source,
      sourceApp = sourceApp,
      reference = reference,
      rawSnippet = rawMessageSnippet.take(160),
      confidenceScore = confidenceScore,
      confidenceReason = confidenceReasons.joinToString(" • "),
      fingerprint = fingerprint
    )
  }
}

sealed class ParseOutcome {
  data class Success(val candidate: TransactionCandidate) : ParseOutcome()
  data class Rejected(val reason: String, val rawSnippet: String) : ParseOutcome()
}
