package com.example.capture.parsing.normalizer

object MerchantNormalizer {
  private val KNOWN_MERCHANT_MAP = mapOf(
    "swiggy" to "Swiggy",
    "zomato" to "Zomato",
    "blinkit" to "Blinkit",
    "zepto" to "Zepto",
    "instamart" to "Instamart",
    "bigbasket" to "BigBasket",
    "amazon" to "Amazon",
    "amzn" to "Amazon",
    "flipkart" to "Flipkart",
    "uber" to "Uber",
    "ola" to "Ola",
    "rapido" to "Rapido",
    "netflix" to "Netflix",
    "spotify" to "Spotify",
    "youtube" to "YouTube",
    "starbucks" to "Starbucks",
    "mcdonalds" to "McDonald's",
    "kfc" to "KFC",
    "dominos" to "Domino's",
    "shell" to "Shell",
    "dmart" to "DMart",
    "myntra" to "Myntra",
    "nykaa" to "Nykaa",
    "croma" to "Croma",
    "reliance" to "Reliance Retail",
    "airtel" to "Airtel",
    "jio" to "Jio",
    "bescom" to "Electricity (BESCOM)",
    "apollo" to "Apollo Pharmacy",
    "pharmeasy" to "PharmEasy",
    "makemytrip" to "MakeMyTrip",
    "bookmyshow" to "BookMyShow",
    "cred" to "CRED"
  )

  fun normalize(rawMerchant: String): String {
    val clean = rawMerchant
      .replace(Regex("(?i)\\b(vpa|upi|pvt|ltd|limited|india|pay\\s*to|at|info:?|inf\\*)\\b"), "")
      .replace(Regex("[^a-zA-Z0-9\\s&.'-]"), " ")
      .trim()

    if (clean.isBlank()) return "Unknown Merchant"

    val lower = clean.lowercase()
    for ((key, normal) in KNOWN_MERCHANT_MAP) {
      if (lower.contains(key)) {
        return normal
      }
    }

    // Convert to Title Case
    return clean.split(" ")
      .filter { it.isNotBlank() }
      .take(3)
      .joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
      }
      .ifBlank { "Unknown Merchant" }
  }

  fun guessCategory(normalizedMerchant: String): String {
    val lower = normalizedMerchant.lowercase()
    return when {
      lower.contains("swiggy") || lower.contains("zomato") || lower.contains("mcdonald") || lower.contains("domino") || lower.contains("kfc") -> "Food"
      lower.contains("starbucks") || lower.contains("cafe") || lower.contains("restaurant") -> "Dining Out"
      lower.contains("blinkit") || lower.contains("zepto") || lower.contains("instamart") || lower.contains("bigbasket") || lower.contains("dmart") || lower.contains("grocer") -> "Groceries & Household"
      lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") || lower.contains("nykaa") || lower.contains("croma") || lower.contains("retail") -> "Shopping"
      lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") || lower.contains("shell") || lower.contains("petrol") || lower.contains("fuel") || lower.contains("metro") -> "Transport"
      lower.contains("netflix") || lower.contains("spotify") || lower.contains("youtube") || lower.contains("prime") || lower.contains("hotstar") -> "Subscriptions"
      lower.contains("airtel") || lower.contains("jio") || lower.contains("electricity") || lower.contains("bescom") || lower.contains("gas") || lower.contains("water") || lower.contains("bill") -> "Bills & Utilities"
      lower.contains("apollo") || lower.contains("pharmeasy") || lower.contains("med") || lower.contains("hospital") || lower.contains("clinic") -> "Health"
      lower.contains("bookmyshow") || lower.contains("cinema") || lower.contains("movie") || lower.contains("game") -> "Entertainment"
      lower.contains("makemytrip") || lower.contains("flight") || lower.contains("hotel") || lower.contains("irctc") || lower.contains("rail") -> "Travel"
      else -> "Other"
    }
  }
}
