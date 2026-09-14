package com.example.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "transactions",
  indices = [
    Index(value = ["fingerprint"]),
    Index(value = ["reference"]),
    Index(value = ["occurredAt"]),
    Index(value = ["categoryName"]),
    Index(value = ["merchantName"]),
    Index(value = ["status"])
  ]
)
data class TransactionEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  val amountPaise: Long, // 100 paise = 1 INR. Always integer.
  val currency: String = "INR",
  val type: TransactionType = TransactionType.EXPENSE,
  val status: TransactionStatus = TransactionStatus.CONFIRMED,
  val occurredAt: Long = System.currentTimeMillis(),
  val merchantName: String = "Unknown",
  val categoryName: String = "Other",
  val accountName: String = "Primary Account",
  val paymentMethod: PaymentMethod = PaymentMethod.UPI,
  val source: CaptureSource = CaptureSource.MANUAL,
  val sourceApp: String? = null,
  val reference: String? = null,
  val rawSnippet: String? = null,
  val confidenceScore: Int = 100, // 0 to 100
  val confidenceReason: String? = null,
  val fingerprint: String = "",
  val linkedTransactionId: Long? = null,
  val notes: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
) {
  val amountRupees: Double
    get() = amountPaise / 100.0

  fun formatRupees(): String {
    val rs = amountPaise / 100
    val paise = Math.abs(amountPaise % 100)
    return if (paise == 0L) {
      "₹$rs"
    } else {
      String.format("₹%d.%02d", rs, paise)
    }
  }
}

@Entity(tableName = "accounts")
data class AccountEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  val name: String,
  val type: AccountType,
  val mask: String? = null, // e.g. "XX1234"
  val balancePaise: Long = 0L,
  val colorHex: String = "#10B981"
)

@Entity(tableName = "categories")
data class CategoryEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  val name: String,
  val iconName: String,
  val colorHex: String,
  val isEssential: Boolean = true
)

@Entity(
  tableName = "merchant_rules",
  indices = [Index(value = ["normalizedMerchant"], unique = true)]
)
data class MerchantRuleEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  val normalizedMerchant: String,
  val defaultCategory: String,
  val defaultPaymentMethod: PaymentMethod? = null,
  val userTrainedCount: Int = 1,
  val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "budgets")
data class BudgetEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  val categoryName: String?, // null means overall total budget
  val monthlyLimitPaise: Long,
  val monthYear: String // e.g. "2026-09"
)

@Entity(
  tableName = "diagnostic_events",
  indices = [Index(value = ["timestamp"])]
)
data class DiagnosticEventEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0L,
  val timestamp: Long = System.currentTimeMillis(),
  val source: CaptureSource,
  val senderOrApp: String,
  val eventType: String, // e.g. "AUTO_SAVED", "SENT_TO_REVIEW", "REJECTED_OTP", "REJECTED_PROMOTION", "DUPLICATE_SKIPPED"
  val summary: String,
  val rawSnippet: String? = null
)
