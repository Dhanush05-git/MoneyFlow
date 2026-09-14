package com.example.data.repository

import android.content.Context
import com.example.capture.TransactionCapturePipeline
import com.example.capture.parsing.classifier.AdaptiveLearnerStats
import com.example.capture.parsing.classifier.AdaptiveSpamLearner
import com.example.data.room.AppDatabase
import com.example.model.AccountEntity
import com.example.model.AccountType
import com.example.model.CaptureSource
import com.example.model.CategoryEntity
import com.example.model.DiagnosticEventEntity
import com.example.model.MerchantRuleEntity
import com.example.model.PaymentMethod
import com.example.model.TransactionEntity
import com.example.model.TransactionStatus
import com.example.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TransactionRepository(
  private val database: AppDatabase,
  private val context: Context? = null
) {

  val activeTransactions: Flow<List<TransactionEntity>> = database.transactionDao().getAllActiveTransactions()
  val reviewQueue: Flow<List<TransactionEntity>> = database.transactionDao().getTransactionsNeedingReview()
  val reviewCount: Flow<Int> = database.transactionDao().getReviewCount()
  val accounts: Flow<List<AccountEntity>> = database.accountDao().getAllAccounts()
  val categories: Flow<List<CategoryEntity>> = database.categoryDao().getAllCategories()
  val merchantRules: Flow<List<MerchantRuleEntity>> = database.merchantRuleDao().getAllRules()
  val recentDiagnostics: Flow<List<DiagnosticEventEntity>> = database.diagnosticDao().getRecentEvents()

  private val capturePipeline = TransactionCapturePipeline(database, context)

  suspend fun insertTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
    database.transactionDao().insert(transaction)
  }

  suspend fun updateTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
    database.transactionDao().update(transaction)
    // Update merchant memory rule if changed
    teachMerchantCategory(transaction.merchantName, transaction.categoryName, transaction.paymentMethod)
  }

  suspend fun deleteTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
    database.transactionDao().delete(transaction)
  }

  suspend fun confirmReview(id: Long) = withContext(Dispatchers.IO) {
    val tx = database.transactionDao().getById(id)
    if (tx != null) {
      database.transactionDao().updateStatus(id, TransactionStatus.CONFIRMED)
      teachMerchantCategory(tx.merchantName, tx.categoryName, tx.paymentMethod)

      // Train on-device adaptive ML that this is a valid transaction
      if (context != null) {
        val snippet = tx.rawSnippet ?: tx.merchantName
        AdaptiveSpamLearner.recordHam(context, snippet, tx.sourceApp)
      }
    }
  }

  suspend fun ignoreReview(id: Long) = withContext(Dispatchers.IO) {
    val tx = database.transactionDao().getById(id)
    if (tx != null) {
      database.transactionDao().updateStatus(id, TransactionStatus.IGNORED)

      // Train on-device adaptive ML that this text/pattern is unwanted promo/spam
      if (context != null) {
        val snippet = tx.rawSnippet ?: tx.merchantName
        AdaptiveSpamLearner.recordSpam(context, snippet, tx.sourceApp)
      }
    }
  }

  fun getAdaptiveSpamStats(): AdaptiveLearnerStats? {
    return context?.let { AdaptiveSpamLearner.getLearnedStats(it) }
  }

  fun resetAdaptiveSpamModel() {
    context?.let { AdaptiveSpamLearner.resetModel(it) }
  }

  suspend fun teachMerchantCategory(
    merchant: String,
    category: String,
    method: PaymentMethod?
  ) = withContext(Dispatchers.IO) {
    if (merchant.isBlank() || merchant == "Unknown Merchant") return@withContext
    val existing = database.merchantRuleDao().getRuleForMerchant(merchant)
    val rule = existing?.copy(
      defaultCategory = category,
      defaultPaymentMethod = method ?: existing.defaultPaymentMethod,
      userTrainedCount = existing.userTrainedCount + 1,
      lastUpdated = System.currentTimeMillis()
    ) ?: MerchantRuleEntity(
      normalizedMerchant = merchant,
      defaultCategory = category,
      defaultPaymentMethod = method,
      userTrainedCount = 1,
      lastUpdated = System.currentTimeMillis()
    )
    database.merchantRuleDao().insertOrUpdate(rule)
  }

  suspend fun processIncomingMessage(
    rawText: String,
    source: CaptureSource,
    senderOrApp: String?,
    autoApproveThreshold: Int = 95
  ): TransactionEntity? {
    return capturePipeline.processIncomingMessage(
      rawText = rawText,
      source = source,
      sourceAppOrSender = senderOrApp,
      autoApproveThreshold = autoApproveThreshold
    )
  }

  suspend fun insertAccount(account: AccountEntity): Long = withContext(Dispatchers.IO) {
    database.accountDao().insert(account)
  }

  suspend fun updateAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
    database.accountDao().update(account)
  }

  suspend fun insertCategory(category: CategoryEntity): Long = withContext(Dispatchers.IO) {
    database.categoryDao().insert(category)
  }

  suspend fun clearAllTransactions() = withContext(Dispatchers.IO) {
    database.transactionDao().deleteAllTransactions()
  }
}
