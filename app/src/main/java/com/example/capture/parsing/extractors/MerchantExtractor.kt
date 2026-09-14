package com.example.capture.parsing.extractors

import com.example.capture.parsing.normalizer.MerchantNormalizer

object MerchantExtractor {
  private val MERCHANT_PATTERNS = listOf(
    // e.g. "paid to Swiggy", "debited for VPA swiggy@icici"
    Regex("(?i)(?:paid\\s*to|sent\\s*to|transferred\\s*to|to\\s*vpa|to)\\s+([a-zA-Z0-9&._@\\s-]+?)(?:\\s+(?:on|via|ref|upi|using|avl|bal|dated|from|\\.)|$)"),
    // e.g. "at Starbucks", "spent at Amazon India"
    Regex("(?i)(?:at|purchase\\s*at)\\s+([a-zA-Z0-9&._'\\s-]+?)(?:\\s+(?:on|via|ref|using|card|avl|bal|\\.)|$)"),
    // e.g. "towards Electricity Bill"
    Regex("(?i)(?:towards|for)\\s+([a-zA-Z0-9&._'\\s-]+?)(?:\\s+(?:on|via|ref|using|avl|bal|\\.)|$)"),
    // e.g. "Info: INF*SWIGGY*1234"
    Regex("(?i)(?:info[:*]|txn\\s*info[:*])\\s*([a-zA-Z0-9&._*\\s-]+?)(?:\\s+(?:avl|bal|ref|\\.)|$)")
  )

  private val KNOWN_KEYWORDS = listOf(
    "Swiggy", "Zomato", "Blinkit", "Zepto", "Instamart", "BigBasket",
    "Amazon", "Flipkart", "Uber", "Ola", "Rapido", "Netflix", "Spotify",
    "Starbucks", "McDonald", "KFC", "Domino", "Shell", "DMart", "Myntra",
    "Nykaa", "Croma", "Airtel", "Jio", "Apollo Pharmacy", "MakeMyTrip", "BookMyShow", "CRED"
  )

  fun extractMerchant(message: String): String {
    // 1. First check if any famous known merchant is directly present in text
    for (known in KNOWN_KEYWORDS) {
      if (message.contains(known, ignoreCase = true)) {
        return MerchantNormalizer.normalize(known)
      }
    }

    // 2. Try regex extraction
    for (regex in MERCHANT_PATTERNS) {
      val match = regex.find(message)
      if (match != null) {
        val candidate = match.groupValues[1].trim()
        if (candidate.length in 3..40 && !candidate.contains("A/c", ignoreCase = true)) {
          return MerchantNormalizer.normalize(candidate)
        }
      }
    }

    return "Unknown Merchant"
  }
}
