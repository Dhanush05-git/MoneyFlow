package com.example.capture.parsing.classifier

import android.content.Context
import org.json.JSONObject
import java.util.Locale
import kotlin.math.ln
import kotlin.math.min

data class AdaptiveLearnerStats(
  val totalSpamExamples: Int,
  val totalHamExamples: Int,
  val uniqueSpamTokens: Int,
  val topLearnedSpamTerms: List<String>
)

/**
 * On-Device Online Machine Learning Spam Classifier.
 * Adapts in real-time when the user confirms or ignores/rejects transactions from the review queue.
 * Requires zero cloud calls, zero external libraries, takes < 50KB, and evaluates in < 1ms.
 */
object AdaptiveSpamLearner {
  private const val PREFS_NAME = "adaptive_spam_learner_model"
  private const val KEY_SPAM_COUNT = "total_spam_count"
  private const val KEY_HAM_COUNT = "total_ham_count"
  private const val KEY_SPAM_TOKENS = "spam_tokens_json"
  private const val KEY_HAM_TOKENS = "ham_tokens_json"
  private const val KEY_SENDER_SPAM = "sender_spam_json"

  private val lock = Any()
  @Volatile private var isLoaded = false

  private var totalSpamSamples = 0
  private var totalHamSamples = 0
  private val tokenSpamCounts = HashMap<String, Int>()
  private val tokenHamCounts = HashMap<String, Int>()
  private val senderSpamCounts = HashMap<String, Int>()

  private val STOP_WORDS = setOf(
    "the", "is", "at", "which", "on", "a", "an", "and", "or", "in", "to", "for", "of", "with",
    "by", "your", "my", "our", "this", "that", "it", "from", "as", "be", "has", "have", "had",
    "will", "shall", "can", "may", "please", "dear", "sir", "madam", "customer", "user"
  )

  fun init(context: Context) {
    if (isLoaded) return
    synchronized(lock) {
      if (isLoaded) return
      loadFromStorage(context)
      isLoaded = true
    }
  }

  private fun loadFromStorage(context: Context) {
    try {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      totalSpamSamples = prefs.getInt(KEY_SPAM_COUNT, 0)
      totalHamSamples = prefs.getInt(KEY_HAM_COUNT, 0)

      val spamTokensStr = prefs.getString(KEY_SPAM_TOKENS, "{}") ?: "{}"
      val spamJson = JSONObject(spamTokensStr)
      tokenSpamCounts.clear()
      for (key in spamJson.keys()) {
        tokenSpamCounts[key] = spamJson.getInt(key)
      }

      val hamTokensStr = prefs.getString(KEY_HAM_TOKENS, "{}") ?: "{}"
      val hamJson = JSONObject(hamTokensStr)
      tokenHamCounts.clear()
      for (key in hamJson.keys()) {
        tokenHamCounts[key] = hamJson.getInt(key)
      }

      val senderStr = prefs.getString(KEY_SENDER_SPAM, "{}") ?: "{}"
      val senderJson = JSONObject(senderStr)
      senderSpamCounts.clear()
      for (key in senderJson.keys()) {
        senderSpamCounts[key] = senderJson.getInt(key)
      }
    } catch (_: Exception) {
      // Fallback gracefully on empty state
    }
  }

  private fun saveToStorage(context: Context) {
    try {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val spamJson = JSONObject()
      tokenSpamCounts.forEach { (k, v) -> spamJson.put(k, v) }

      val hamJson = JSONObject()
      tokenHamCounts.forEach { (k, v) -> hamJson.put(k, v) }

      val senderJson = JSONObject()
      senderSpamCounts.forEach { (k, v) -> senderJson.put(k, v) }

      prefs.edit()
        .putInt(KEY_SPAM_COUNT, totalSpamSamples)
        .putInt(KEY_HAM_COUNT, totalHamSamples)
        .putString(KEY_SPAM_TOKENS, spamJson.toString())
        .putString(KEY_HAM_TOKENS, hamJson.toString())
        .putString(KEY_SENDER_SPAM, senderJson.toString())
        .apply()
    } catch (_: Exception) {
      // Best-effort persist
    }
  }

  /**
   * Tokenizes text into meaningful semantic tokens (lowercase, alphanumeric, stopwords stripped)
   */
  fun tokenize(text: String): List<String> {
    return text.lowercase(Locale.ROOT)
      .replace(Regex("[^a-z0-9\\s]"), " ")
      .split(Regex("\\s+"))
      .filter { it.length in 3..25 && !STOP_WORDS.contains(it) && !it.all { ch -> ch.isDigit() } }
  }

  /**
   * Called when a user IGNORES or reports a transaction from the review queue.
   * Trains the model that this message pattern represents unwanted spam/promotions.
   */
  fun recordSpam(context: Context, rawText: String, sender: String? = null) {
    synchronized(lock) {
      init(context)
      totalSpamSamples++

      val tokens = tokenize(rawText)
      for (token in tokens.toSet()) { // Unique tokens per message
        val current = tokenSpamCounts[token] ?: 0
        tokenSpamCounts[token] = current + 1
      }

      if (!sender.isNullOrBlank()) {
        val normSender = sender.trim().uppercase(Locale.ROOT)
        senderSpamCounts[normSender] = (senderSpamCounts[normSender] ?: 0) + 1
      }

      saveToStorage(context)
    }
  }

  /**
   * Called when a user CONFIRMS a transaction from the review queue.
   * Trains the model that this message pattern represents a legitimate financial transaction.
   */
  fun recordHam(context: Context, rawText: String, sender: String? = null) {
    synchronized(lock) {
      init(context)
      totalHamSamples++

      val tokens = tokenize(rawText)
      for (token in tokens.toSet()) {
        val current = tokenHamCounts[token] ?: 0
        tokenHamCounts[token] = current + 1
      }

      saveToStorage(context)
    }
  }

  /**
   * Calculates the learned spam probability score (0.0 to 1.0).
   * Uses Naive Bayes log-odds with Laplace smoothing.
   */
  fun predictSpamScore(context: Context?, rawText: String, sender: String? = null): Float {
    if (context != null) init(context)

    // If no user training has happened yet, return neutral 0.0
    if (totalSpamSamples == 0) return 0f

    val tokens = tokenize(rawText)
    if (tokens.isEmpty()) return 0f

    var logOdds = 0.0
    val totalSpamTokens = tokenSpamCounts.values.sum().coerceAtLeast(1)
    val totalHamTokens = tokenHamCounts.values.sum().coerceAtLeast(1)
    val vocabSize = (tokenSpamCounts.keys + tokenHamCounts.keys).size.coerceAtLeast(10)

    var informativeTokens = 0

    for (token in tokens) {
      val spamCount = tokenSpamCounts[token] ?: 0
      val hamCount = tokenHamCounts[token] ?: 0

      // Only consider tokens that have been seen in training
      if (spamCount > 0 || hamCount > 0) {
        val pTokenSpam = (spamCount + 1.0) / (totalSpamTokens + vocabSize)
        val pTokenHam = (hamCount + 1.0) / (totalHamTokens + vocabSize)

        logOdds += ln(pTokenSpam / pTokenHam)
        informativeTokens++
      }
    }

    // Sender reputation penalty
    if (!sender.isNullOrBlank()) {
      val normSender = sender.trim().uppercase(Locale.ROOT)
      val spamCount = senderSpamCounts[normSender] ?: 0
      if (spamCount >= 2) {
        logOdds += 2.5 * min(spamCount, 5)
        informativeTokens++
      }
    }

    if (informativeTokens == 0) return 0f

    // Sigmoid conversion from log-odds to probability (0.0 to 1.0)
    val probability = (1.0 / (1.0 + Math.exp(-logOdds))).toFloat()
    return probability.coerceIn(0f, 1f)
  }

  fun getLearnedStats(context: Context): AdaptiveLearnerStats {
    init(context)
    synchronized(lock) {
      val topTerms = tokenSpamCounts.entries
        .sortedByDescending { it.value }
        .take(6)
        .map { "${it.key} (${it.value})" }

      return AdaptiveLearnerStats(
        totalSpamExamples = totalSpamSamples,
        totalHamExamples = totalHamSamples,
        uniqueSpamTokens = tokenSpamCounts.size,
        topLearnedSpamTerms = topTerms
      )
    }
  }

  fun resetModel(context: Context) {
    synchronized(lock) {
      totalSpamSamples = 0
      totalHamSamples = 0
      tokenSpamCounts.clear()
      tokenHamCounts.clear()
      senderSpamCounts.clear()
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      prefs.edit().clear().apply()
    }
  }
}
