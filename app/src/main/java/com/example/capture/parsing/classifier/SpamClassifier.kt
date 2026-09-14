package com.example.capture.parsing.classifier

object SpamClassifier {
  private val OTP_PATTERNS = listOf(
    Regex("(?i)\\b(otp|one\\s*time\\s*password|verification\\s*code|security\\s*code|auth\\s*code)\\b"),
    Regex("(?i)\\b(do\\s*not\\s*share|never\\s*share|valid\\s*for\\s*\\d+\\s*(mins|minutes)|secret\\s*code)\\b"),
    Regex("(?i)\\blogin\\s*to\\s*your\\s*account\\s*using\\b"),
    Regex("(?i)\\buse\\s*code\\s*\\d{4,8}\\b")
  )

  private val MARKETING_AND_LOAN_PATTERNS = listOf(
    // Loan & Credit line spam (e.g. "you are eligible for loan", "pre-approved instant personal loan")
    Regex("(?i)\\b(eligib(le|ility)\\s*(for|to|of)?\\s*(a\\s*)?(personal\\s*|instant\\s*|cash\\s*)?loan)\\b"),
    Regex("(?i)\\b(pre-?approved\\s*(instant\\s*)?(personal\\s*)?(loan|credit\\s*card|limit|offer))\\b"),
    Regex("(?i)\\b(instant\\s*loan|apply\\s*for\\s*loan|get\\s*(instant\\s*)?loan|avail\\s*(a\\s*)?loan)\\b"),
    Regex("(?i)\\b(loan\\s*(sanctioned|offer|disbursal|approved|upto|at\\s*zero))\\b"),
    Regex("(?i)\\b(zero\\s*interest|interest\\s*free\\s*loan|zero\\s*processing\\s*fee)\\b"),
    Regex("(?i)\\b(credit\\s*score|free\\s*credit\\s*report|check\\s*your\\s*cibil)\\b"),
    Regex("(?i)\\b(increase\\s*(your\\s*)?credit\\s*limit|limit\\s*enhancement|card\\s*upgrade\\s*offer)\\b"),
    Regex("(?i)\\b(apply\\s*now|apply\\s*for\\s*(a\\s*)?credit\\s*card|lifetime\\s*free\\s*card)\\b"),

    // Promotional rewards, discounts, lotteries & scratch cards
    Regex("(?i)\\b(congratulations|you\\s*have\\s*won|claim\\s*(your\\s*)?reward|special\\s*discount)\\b"),
    Regex("(?i)\\b(exclusive\\s*offer|flat\\s*\\d+%\\s*off|cashback\\s*upto\\s*rs|limited\\s*period\\s*offer)\\b"),
    Regex("(?i)\\b(scratch\\s*card|spin\\s*and\\s*win|lucky\\s*draw|win\\s*exciting\\s*rewards)\\b"),
    Regex("(?i)\\b(refer\\s*and\\s*earn|invite\\s*friends\\s*and\\s*get)\\b"),

    // Bill due reminders (not yet paid)
    Regex("(?i)\\b(reminder:\\s*your\\s*bill|bill\\s*is\\s*due|payment\\s*due\\s*date|minimum\\s*amount\\s*due|avoid\\s*late\\s*fee)\\b"),

    // Phishing / KYC updates
    Regex("(?i)\\b(kyc\\s*(pending|suspended|expired|update)|pan\\s*(card\\s*)?update|account\\s*will\\s*be\\s*(suspended|blocked))\\b"),

    // Investment / Stock advisory spam
    Regex("(?i)\\b(stock\\s*tips|trading\\s*profit|jackpot\\s*calls|guaranteed\\s*returns)\\b")
  )

  private val BALANCE_INQUIRY_ONLY = Regex("(?i)^.*(avail(able)?\\s*bal(ance)?|clear\\s*bal(ance)?)\\s*(is|:).*$")
  private val TRANSACTION_SIGNAL = Regex("(?i)\\b(debited|credited|paid|spent|transferred|withdrawn|charged|purchase|refund|reversed|received|sent)\\b")

  fun isSpamOrIgnored(message: String): Pair<Boolean, String?> {
    val clean = message.trim()

    // 1. Check for OTP / Security verification codes
    for (pat in OTP_PATTERNS) {
      if (pat.containsMatchIn(clean)) {
        return Pair(true, "Security OTP or Verification Code (Ignored for Privacy)")
      }
    }

    // 2. Check for Promotional / Loan / Marketing spam
    for (pat in MARKETING_AND_LOAN_PATTERNS) {
      if (pat.containsMatchIn(clean)) {
        return Pair(true, "Promotional or Loan Spam Offer")
      }
    }

    // 3. Balance inquiry with no transaction verb
    if (BALANCE_INQUIRY_ONLY.matches(clean) && !TRANSACTION_SIGNAL.containsMatchIn(clean)) {
      return Pair(true, "Informational Balance Inquiry (No Transaction)")
    }

    return Pair(false, null)
  }
}
