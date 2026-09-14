package com.example.capture.parsing.confidence

import com.example.model.PaymentMethod
import com.example.model.TransactionType

object ConfidenceScorer {
  fun scoreConfidence(
    amountPaise: Long?,
    type: TransactionType,
    merchant: String,
    reference: String?,
    account: String,
    paymentMethod: PaymentMethod,
    hasUserMerchantRule: Boolean
  ): Pair<Int, List<String>> {
    var score = 0
    val reasons = mutableListOf<String>()

    // 1. Amount verification (up to 30)
    if (amountPaise != null && amountPaise > 0) {
      score += 30
      reasons.add("Valid monetary amount extracted")
    }

    // 2. Transaction verb clarity (up to 25)
    score += 25
    reasons.add("Explicit ${type.label.lowercase()} transaction verb matched")

    // 3. Merchant identification (up to 20)
    if (merchant.isNotBlank() && merchant != "Unknown Merchant") {
      score += 20
      reasons.add("Identified merchant/payee: $merchant")
    } else {
      reasons.add("Merchant unidentified; needs user verification")
    }

    // 4. Financial Reference (UPI / RRN / UTR / Card) (up to 15)
    if (!reference.isNullOrBlank()) {
      score += 15
      reasons.add("Verified reference identifier ($reference)")
    }

    // 5. Account / Bank identification (up to 10)
    if (account.isNotBlank() && account != "Primary Account") {
      score += 10
      reasons.add("Associated bank/account detected ($account)")
    }

    // 6. User personal rule bonus
    if (hasUserMerchantRule) {
      score = Math.min(100, score + 10)
      reasons.add("Matched your custom merchant category memory")
    }

    val finalScore = score.coerceIn(10, 100)
    return Pair(finalScore, reasons)
  }
}
