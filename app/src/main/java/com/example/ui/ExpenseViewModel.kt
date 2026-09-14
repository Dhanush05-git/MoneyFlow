package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.capture.ParseOutcome
import com.example.capture.parsing.TransactionParserEngine
import com.example.capture.sms.ScanResult
import com.example.capture.sms.SmsHistoricalScanner
import com.example.data.repository.TransactionRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class ExpenseUiState(
  val allTransactions: List<TransactionEntity> = emptyList(),
  val filteredTransactions: List<TransactionEntity> = emptyList(),
  val reviewQueue: List<TransactionEntity> = emptyList(),
  val reviewCount: Int = 0,
  val categories: List<CategoryEntity> = emptyList(),
  val accounts: List<AccountEntity> = emptyList(),
  val merchantRules: List<MerchantRuleEntity> = emptyList(),
  val diagnostics: List<DiagnosticEventEntity> = emptyList(),
  val selectedDateMillis: Long? = null,
  val searchQuery: String = "",
  val typeFilter: TransactionType? = null,
  val sourceFilter: CaptureSource? = null,
  val categoryFilter: String? = null,
  val autoApproveThreshold: Int = 95,
  val isScanning: Boolean = false,
  val scanProgress: Pair<Int, Int> = Pair(0, 0),
  val lastScanResult: ScanResult? = null,
  val learnedSpamStats: com.example.capture.parsing.classifier.AdaptiveLearnerStats? = null
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

  private val database = AppDatabase.getDatabase(application)
  private val repository = TransactionRepository(database, application)
  private val parserEngine = TransactionParserEngine(database.merchantRuleDao(), application)

  private val _searchQuery = MutableStateFlow("")
  private val _typeFilter = MutableStateFlow<TransactionType?>(null)
  private val _sourceFilter = MutableStateFlow<CaptureSource?>(null)
  private val _categoryFilter = MutableStateFlow<String?>(null)
  private val _selectedDateMillis = MutableStateFlow<Long?>(null)
  private val _autoApproveThreshold = MutableStateFlow(95)
  private val _isScanning = MutableStateFlow(false)
  private val _scanProgress = MutableStateFlow(Pair(0, 0))
  private val _lastScanResult = MutableStateFlow<ScanResult?>(null)
  private val _learnedStatsTick = MutableStateFlow(0L)

  init {
    // Clean production start: no demo transactions seeded
  }

  val uiState: StateFlow<ExpenseUiState> = combine(
    repository.activeTransactions,
    repository.reviewQueue,
    repository.reviewCount,
    repository.categories,
    repository.accounts,
    repository.merchantRules,
    repository.recentDiagnostics,
    _searchQuery,
    _typeFilter,
    _sourceFilter,
    _categoryFilter,
    _selectedDateMillis,
    _isScanning,
    _scanProgress,
    _lastScanResult,
    _learnedStatsTick
  ) { args: Array<Any?> ->
    @Suppress("UNCHECKED_CAST")
    val txs = args[0] as List<TransactionEntity>
    @Suppress("UNCHECKED_CAST")
    val reviews = args[1] as List<TransactionEntity>
    val reviewCount = args[2] as Int
    @Suppress("UNCHECKED_CAST")
    val categories = args[3] as List<CategoryEntity>
    @Suppress("UNCHECKED_CAST")
    val accounts = args[4] as List<AccountEntity>
    @Suppress("UNCHECKED_CAST")
    val rules = args[5] as List<MerchantRuleEntity>
    @Suppress("UNCHECKED_CAST")
    val diags = args[6] as List<DiagnosticEventEntity>
    val query = args[7] as String
    val typeFilter = args[8] as TransactionType?
    val sourceFilter = args[9] as CaptureSource?
    val catFilter = args[10] as String?
    val selectedDate = args[11] as Long?
    val isScanning = args[12] as Boolean
    @Suppress("UNCHECKED_CAST")
    val scanProg = args[13] as Pair<Int, Int>
    val lastScan = args[14] as ScanResult?

    val filtered = txs.filter { tx ->
      val matchesQuery = if (query.isBlank()) true else {
        val q = query.trim().lowercase()
        // Support search by condition e.g. "> 500" or merchant/category name
        if (q.startsWith(">") || q.startsWith("<")) {
          val op = q.first()
          val numStr = q.drop(1).trim().replace(",", "")
          val targetRs = numStr.toDoubleOrNull()
          if (targetRs != null) {
            val amountRs = tx.amountRupees
            if (op == '>') amountRs >= targetRs else amountRs <= targetRs
          } else true
        } else {
          tx.merchantName.lowercase().contains(q) ||
          tx.categoryName.lowercase().contains(q) ||
          (tx.reference?.lowercase()?.contains(q) == true) ||
          tx.accountName.lowercase().contains(q)
        }
      }

      val matchesType = typeFilter == null || tx.type == typeFilter
      val matchesSource = sourceFilter == null || tx.source == sourceFilter
      val matchesCategory = catFilter == null || tx.categoryName.equals(catFilter, ignoreCase = true)

      matchesQuery && matchesType && matchesSource && matchesCategory
    }

    ExpenseUiState(
      allTransactions = txs,
      filteredTransactions = filtered,
      reviewQueue = reviews,
      reviewCount = reviewCount,
      categories = categories,
      accounts = accounts,
      merchantRules = rules,
      diagnostics = diags,
      selectedDateMillis = selectedDate,
      searchQuery = query,
      typeFilter = typeFilter,
      sourceFilter = sourceFilter,
      categoryFilter = catFilter,
      autoApproveThreshold = _autoApproveThreshold.value,
      isScanning = isScanning,
      scanProgress = scanProg,
      lastScanResult = lastScan,
      learnedSpamStats = repository.getAdaptiveSpamStats()
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = ExpenseUiState()
  )

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setTypeFilter(type: TransactionType?) {
    _typeFilter.value = type
  }

  fun setSourceFilter(source: CaptureSource?) {
    _sourceFilter.value = source
  }

  fun setCategoryFilter(category: String?) {
    _categoryFilter.value = category
  }

  fun selectCalendarDate(millis: Long?) {
    _selectedDateMillis.value = millis
  }

  fun setAutoApproveThreshold(threshold: Int) {
    _autoApproveThreshold.value = threshold
  }

  fun addManualExpense(
    amountRupees: Double,
    merchant: String,
    category: String,
    account: String,
    paymentMethod: PaymentMethod,
    type: TransactionType,
    dateMillis: Long,
    notes: String?
  ) {
    viewModelScope.launch {
      val paise = (amountRupees * 100).toLong()
      val entity = TransactionEntity(
        amountPaise = paise,
        currency = "INR",
        type = type,
        status = TransactionStatus.CONFIRMED,
        occurredAt = dateMillis,
        merchantName = merchant.ifBlank { "Manual Entry" },
        categoryName = category,
        accountName = account,
        paymentMethod = paymentMethod,
        source = CaptureSource.MANUAL,
        confidenceScore = 100,
        confidenceReason = "Manually verified by user",
        notes = notes
      )
      repository.insertTransaction(entity)
      repository.teachMerchantCategory(merchant, category, paymentMethod)
    }
  }

  fun updateTransaction(transaction: TransactionEntity) {
    viewModelScope.launch {
      repository.updateTransaction(transaction)
    }
  }

  fun deleteTransaction(transaction: TransactionEntity) {
    viewModelScope.launch {
      repository.deleteTransaction(transaction)
    }
  }

  fun confirmReview(id: Long) {
    viewModelScope.launch {
      repository.confirmReview(id)
      _learnedStatsTick.value = System.currentTimeMillis()
    }
  }

  fun ignoreReview(id: Long) {
    viewModelScope.launch {
      repository.ignoreReview(id)
      _learnedStatsTick.value = System.currentTimeMillis()
    }
  }

  fun resetAdaptiveSpamFilter() {
    viewModelScope.launch {
      repository.resetAdaptiveSpamModel()
      _learnedStatsTick.value = System.currentTimeMillis()
    }
  }

  suspend fun testParse(rawText: String, source: CaptureSource = CaptureSource.SMS, sender: String? = "HDFCBK"): ParseOutcome {
    return parserEngine.parse(rawText, source, sender)
  }

  fun runHistoricalScan(daysBack: Int = 60) {
    viewModelScope.launch {
      _isScanning.value = true
      try {
        val result = SmsHistoricalScanner.scanRecentSms(
          context = getApplication(),
          daysBack = daysBack,
          onProgress = { current, total ->
            _scanProgress.value = Pair(current, total)
          }
        )
        _lastScanResult.value = result
      } finally {
        _isScanning.value = false
      }
    }
  }

  fun teachRule(merchant: String, category: String, method: PaymentMethod?) {
    viewModelScope.launch {
      repository.teachMerchantCategory(merchant, category, method)
    }
  }

  fun addAccount(name: String, type: AccountType, mask: String?, balanceRupees: Double) {
    viewModelScope.launch {
      val acc = AccountEntity(
        name = name,
        type = type,
        mask = mask,
        balancePaise = (balanceRupees * 100).toLong()
      )
      repository.insertAccount(acc)
    }
  }

  fun clearAllTransactions() {
    viewModelScope.launch {
      repository.clearAllTransactions()
    }
  }
}
