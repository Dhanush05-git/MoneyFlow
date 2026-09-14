package com.example.capture.parsing.extractors

import java.text.DecimalFormat

object AmountExtractor {
  // Matches Rs. 1,250.50 | INR 450 | ₹ 3,400.00 | debited by 450
  private val AMOUNT_REGEXES = listOf(
    Regex("(?i)(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
    Regex("(?i)(?:amount|txn\\s*of|for|by|spent)\\s*(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
    Regex("(?i)([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:debited|credited|spent|paid)")
  )

  /**
   * Extracts amount in integer paise (1 INR = 100 paise).
   * Returns null if no valid monetary amount detected.
   */
  fun extractAmountPaise(message: String): Long? {
    for (regex in AMOUNT_REGEXES) {
      val match = regex.find(message)
      if (match != null) {
        val amountStr = match.groupValues[1].replace(",", "").trim()
        val paise = parseToPaise(amountStr)
        if (paise != null && paise > 0L) {
          return paise
        }
      }
    }
    return null
  }

  private fun parseToPaise(raw: String): Long? {
    return try {
      val parts = raw.split(".")
      val rupees = parts[0].toLongOrNull() ?: return null
      val paise = if (parts.size > 1) {
        val pStr = parts[1].padEnd(2, '0').take(2)
        pStr.toLongOrNull() ?: 0L
      } else {
        0L
      }
      (rupees * 100L) + paise
    } catch (e: Exception) {
      null
    }
  }
}
