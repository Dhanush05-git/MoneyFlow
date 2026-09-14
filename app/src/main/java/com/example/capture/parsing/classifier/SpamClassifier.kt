package com.example.capture.parsing.classifier

import android.content.Context

object SpamClassifier {
  private val OTP_PATTERNS = listOf(
    Regex("(?i)\\b(otp|one\\s*time\\s*password|verification\\s*code|security\\s*code|auth\\s*code)\\b"),
    Regex("(?i)\\b(do\\s*not\\s*share|never\\s*share|valid\\s*for\\s*\\d+\\s*(mins|minutes)|secret\\s*code)\\b"),
    Regex("(?i)\\blogin\\s*to\\s*your\\s*account\\s*using\\b"),
    Regex("(?i)\\buse\\s*code\\s*\\d{4,8}\\b")
  )

  private val MARKETING_AND_LOAN_PATTERNS = listOf(
    // Telecom recharge advertisements, mobile packs & validity offers (frequent false positives)
    Regex("(?i)\\b(recharge\\s*(with|for|now|pack|plan|offer)?\\s*(rs|₹)?\\s*\\d+.*validity)\\b"),
    Regex("(?i)\\b(validity\\s*of\\s*\\d+\\s*days|\\d+\\s*gb/day|unlimited\\s*(5g|calls|data)|talktime\\s*of)\\b"),
    Regex("(?i)\\b(best\\s*recharge\\s*pack|recommended\\s*plan|special\\s*recharge\\s*offer|pack\\s*worth)\\b"),
    Regex("(?i)\\b(recharge\\s*your\\s*(airtel|jio|vi|vodafone|bsnl)\\s*(mobile|number)?)\\b"),
    Regex("(?i)\\b(data\\s*balance\\s*is\\s*exhausted|daily\\s*limit\\s*of\\s*data|pack\\s*expir(es|ed|ing))\\b"),

    // Coupon codes, promo codes & discounts
    Regex("(?i)\\b(use\\s*code\\s*[a-z0-9_]{3,15}|apply\\s*(coupon|promo)\\s*code|valid\\s*coupon)\\b"),
    Regex("(?i)\\b(flat\\s*(rs\\.?|inr|₹)?\\s*\\d+\\s*(%|percent)?\\s*(off|discount|cashback)|save\\s*(rs\\.?|inr|₹)?\\s*\\d+)\\b"),
    Regex("(?i)\\b(deals?\\s*of\\s*the\\s*day|buy\\s*\\d+\\s*get\\s*\\d+|flash\\s*sale|megasale|clearance\\s*sale)\\b"),
    Regex("(?i)\\b(order\\s*(starts|starting)\\s*at\\s*(rs\\.?|inr|₹)?\\s*\\d+|cravings?\\s*(sorted|calling))\\b"),
    Regex("(?i)\\b(free\\s*delivery\\s*on\\s*orders|minimum\\s*order\\s*value|tap\\s*to\\s*order|order\\s*now\\s*on)\\b"),

    // Loan & Credit line spam
    Regex("(?i)\\b(eligib(le|ility)\\s*(for|to|of)?\\s*(a\\s*)?(personal\\s*|instant\\s*|cash\\s*)?loan)\\b"),
    Regex("(?i)\\b(pre-?approved\\s*(instant\\s*)?(personal\\s*)?(loan|credit\\s*card|limit|offer))\\b"),
    Regex("(?i)\\b(instant\\s*loan|apply\\s*for\\s*loan|get\\s*(instant\\s*)?loan|avail\\s*(a\\s*)?loan)\\b"),
    Regex("(?i)\\b(loan\\s*(sanctioned|offer|disbursal|approved|upto|at\\s*zero))\\b"),
    Regex("(?i)\\b(zero\\s*interest|interest\\s*free\\s*loan|zero\\s*processing\\s*fee)\\b"),
    Regex("(?i)\\b(credit\\s*score|free\\s*credit\\s*report|check\\s*your\\s*cibil)\\b"),
    Regex("(?i)\\b(increase\\s*(your\\s*)?credit\\s*limit|limit\\s*enhancement|card\\s*upgrade\\s*offer)\\b"),
    Regex("(?i)\\b(apply\\s*now|apply\\s*for\\s*(a\\s*)?credit\\s*card|lifetime\\s*free\\s*card)\\b"),

    // Promotional rewards, lotteries, scratch cards & vouchers
    Regex("(?i)\\b(congratulations|you\\s*have\\s*won|claim\\s*(your\\s*)?(reward|cashback|voucher|gift\\s*card))\\b"),
    Regex("(?i)\\b(exclusive\\s*offer|cashback\\s*upto\\s*(rs|₹)?\\s*\\d+|limited\\s*period\\s*offer)\\b"),
    Regex("(?i)\\b(scratch\\s*card|spin\\s*and\\s*win|lucky\\s*draw|win\\s*exciting\\s*rewards|jackpot)\\b"),
    Regex("(?i)\\b(refer\\s*and\\s*earn|invite\\s*friends\\s*and\\s*get)\\b"),
    Regex("(?i)\\b(reward\\s*points\\s*expir(ing|ed)|redeem\\s*your\\s*(points|coins))\\b"),

    // Upcoming Bill due reminders (not paid yet)
    Regex("(?i)\\b(reminder:\\s*your\\s*bill|bill\\s*is\\s*due|payment\\s*due\\s*date|minimum\\s*amount\\s*due|avoid\\s*late\\s*fee|bill\\s*generated)\\b"),
    Regex("(?i)\\b(e-?statement\\s*for.*is\\s*ready|statement\\s*generated|pay\\s*bill\\s*before)\\b"),

    // Phishing / KYC updates
    Regex("(?i)\\b(kyc\\s*(pending|suspended|expired|update)|pan\\s*(card\\s*)?update|account\\s*will\\s*be\\s*(suspended|blocked))\\b"),

    // Investment / Stock advisory spam
    Regex("(?i)\\b(stock\\s*tips|trading\\s*profit|jackpot\\s*calls|guaranteed\\s*returns|crypto\\s*bonus)\\b")
  )

  private val BALANCE_INQUIRY_ONLY = Regex("(?i)^.*(avail(able)?\\s*bal(ance)?|clear\\s*bal(ance)?)\\s*(is|:).*$")
  private val TRANSACTION_SIGNAL = Regex("(?i)\\b(debited|credited|paid|spent|transferred|withdrawn|charged|purchase|refund|reversed|received|sent|deposited)\\b")

  /**
   * Evaluates whether a message is promotional spam, OTP, or non-transactional content.
   * If a Context is provided, also checks the on-device Adaptive Machine Learning model.
   */
  fun isSpamOrIgnored(
    message: String,
    sender: String? = null,
    context: Context? = null
  ): Pair<Boolean, String?> {
    val clean = message.trim()

    // 1. Check for OTP / Security verification codes
    for (pat in OTP_PATTERNS) {
      if (pat.containsMatchIn(clean)) {
        return Pair(true, "Security OTP or Verification Code (Ignored for Privacy)")
      }
    }

    // 2. Check for Promotional / Telecom / Loan / Marketing spam
    for (pat in MARKETING_AND_LOAN_PATTERNS) {
      if (pat.containsMatchIn(clean)) {
        return Pair(true, "Promotional, Recharge or Marketing Offer")
      }
    }

    // 3. Balance inquiry with no transaction verb
    if (BALANCE_INQUIRY_ONLY.matches(clean) && !TRANSACTION_SIGNAL.containsMatchIn(clean)) {
      return Pair(true, "Informational Balance Inquiry (No Transaction)")
    }

    // 4. On-Device Adaptive Machine Learning model check
    if (context != null) {
      val spamScore = AdaptiveSpamLearner.predictSpamScore(context, clean, sender)
      if (spamScore >= 0.65f) {
        val pct = (spamScore * 100).toInt()
        return Pair(true, "Filtered by Adaptive Machine Learning AI model ($pct% spam probability based on your ignored reviews)")
      }
    }

    return Pair(false, null)
  }
}
