package com.example.capture.parsing.extractors

object AccountExtractor {
  private val BANK_MAP = listOf(
    Pair(Regex("(?i)\\b(hdfc|hdfcbk)\\b"), "HDFC Bank"),
    Pair(Regex("(?i)\\b(sbi|sbinb|sbiinb)\\b"), "SBI Savings"),
    Pair(Regex("(?i)\\b(icici|icicib)\\b"), "ICICI Bank"),
    Pair(Regex("(?i)\\b(axis|axisbk)\\b"), "Axis Bank"),
    Pair(Regex("(?i)\\b(kotak|ktkbnk)\\b"), "Kotak Bank"),
    Pair(Regex("(?i)\\b(pnb)\\b"), "PNB"),
    Pair(Regex("(?i)\\b(canara|cnrbk)\\b"), "Canara Bank"),
    Pair(Regex("(?i)\\b(bob|barodabk)\\b"), "Bank of Baroda"),
    Pair(Regex("(?i)\\b(idfc|idfcfirst)\\b"), "IDFC First Bank"),
    Pair(Regex("(?i)\\b(federal)\\b"), "Federal Bank")
  )

  private val MASK_REGEX = Regex("(?i)(?:a/c|acct|account|card)\\s*(?:no\\.?)?\\s*(?:ending\\s*(?:with|in)?)?\\s*([xX*]*\\d{3,4})")

  fun extractAccount(message: String, sender: String? = null): String {
    val clean = (sender ?: "") + " " + message

    // 1. Detect Bank
    var detectedBank: String? = null
    for ((regex, bankName) in BANK_MAP) {
      if (regex.containsMatchIn(clean)) {
        detectedBank = bankName
        break
      }
    }

    // 2. Detect Account Mask (e.g. XX4082)
    val maskMatch = MASK_REGEX.find(message)
    val mask = maskMatch?.groupValues?.get(1)?.trim()

    return when {
      detectedBank != null && mask != null -> "$detectedBank ($mask)"
      detectedBank != null -> detectedBank
      mask != null -> "Account $mask"
      clean.contains("credit card", ignoreCase = true) -> "Credit Card"
      else -> "Primary Account"
    }
  }
}
