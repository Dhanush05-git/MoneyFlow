package com.example.capture.parsing.extractors

import com.example.model.PaymentMethod
import com.example.model.TransactionType

object DebitCreditClassifier {
  private val REFUND_REGEX = Regex("(?i)\\b(refund(ed)?|revers(ed|al)|cashback\\s*(credited|received)?)\\b")
  private val ATM_REGEX = Regex("(?i)\\b(atm\\s*w(ith)?d(rawa)?l|withdrawn\\s*from\\s*atm|cash\\s*withdrawal)\\b")
  private val CARD_PAYMENT_REGEX = Regex("(?i)\\b(payment\\s*(received\\s*)?towards\\s*(your\\s*)?credit\\s*card|credit\\s*card\\s*bill\\s*payment)\\b")
  private val TRANSFER_REGEX = Regex("(?i)\\b(transferred\\s*to\\s*(own|self)|self\\s*transfer|neft\\s*transfer\\s*to\\s*self)\\b")
  private val INCOME_REGEX = Regex("(?i)\\b(credited\\s*(to|in)?|received\\s*(from|rs|inr|₹)?|deposited\\s*(to|in)?|salary\\s*credited|money\\s*received|payment\\s*received|added\\s*to\\s*(your\\s*)?(wallet|account)|you('ve|\\s*have)?\\s*received|inward\\s*(remittance|neft|imps|upi))\\b")
  private val EXPENSE_REGEX = Regex("(?i)\\b(debited\\s*(from|by)?|paid\\s*(to|for)?|spent\\s*(on|at)?|charged\\s*(to|on)?|purchase\\s*(at|of)?|sent\\s*(to|rs|inr|₹)?|transferred\\s*(to|rs|inr|₹)?|payment\\s*(of|to|for)|you('ve|\\s*have)?\\s*paid|txn\\s*of|withdrawn|money\\s*debited)\\b")

  // Checks for banking instruments, masks, UPI references or TRAI banking headers
  private val ACCOUNT_MASK_REGEX = Regex("(?i)\\b(a/c\\s*(no\\.?)?|acct|account\\s*(no\\.?)?|card\\s*(ending)?|ending\\s*(in|with)?)\\s*([x\\*]*\\d{3,6})\\b")
  private val UPI_REF_REGEX = Regex("(?i)\\b(upi\\s*ref(erence)?(\\s*no\\.?)?|rrn|utr|ref\\s*(no\\.?)?|txn\\s*(id|no\\.?)?)\\s*[:#-]?\\s*([a-z0-9]{6,16})\\b")
  private val VPA_REGEX = Regex("(?i)\\b([a-zA-Z0-9.\\-_]+@[a-zA-Z]+)\\b")

  private val BANK_SENDER_PATTERNS = listOf(
    "HDFC", "SBI", "ICICI", "AXIS", "KOTAK", "PNB", "BOI", "CANARA", "CANBNK",
    "INDUS", "YESBNK", "IDFC", "UNIONB", "BARODA", "PAYTM", "GPAY", "BHIM",
    "FEDRL", "RBL", "SCB", "CITI", "JUPITR", "EPFO", "CRED"
  )

  fun hasExplicitFinancialVerb(message: String): Boolean {
    val clean = message.trim()
    return REFUND_REGEX.containsMatchIn(clean) ||
           CARD_PAYMENT_REGEX.containsMatchIn(clean) ||
           ATM_REGEX.containsMatchIn(clean) ||
           TRANSFER_REGEX.containsMatchIn(clean) ||
           INCOME_REGEX.containsMatchIn(clean) ||
           EXPENSE_REGEX.containsMatchIn(clean)
  }

  fun hasFinancialProof(message: String, sender: String?): Boolean {
    val clean = message.trim()
    if (ACCOUNT_MASK_REGEX.containsMatchIn(clean)) return true
    if (UPI_REF_REGEX.containsMatchIn(clean)) return true
    if (VPA_REGEX.containsMatchIn(clean)) return true

    if (!sender.isNullOrBlank()) {
      val upper = sender.uppercase()
      if (BANK_SENDER_PATTERNS.any { upper.contains(it) }) return true
    }

    return false
  }

  fun classifyType(message: String): Pair<TransactionType?, String> {
    val clean = message.trim()

    if (REFUND_REGEX.containsMatchIn(clean)) {
      return Pair(TransactionType.REFUND, "Identified refund / reversal keyword")
    }

    if (CARD_PAYMENT_REGEX.containsMatchIn(clean)) {
      return Pair(TransactionType.CARD_PAYMENT, "Identified credit card bill settlement")
    }

    if (ATM_REGEX.containsMatchIn(clean)) {
      return Pair(TransactionType.WITHDRAWAL, "Identified ATM cash withdrawal")
    }

    if (TRANSFER_REGEX.containsMatchIn(clean)) {
      return Pair(TransactionType.TRANSFER, "Identified self account transfer")
    }

    if (INCOME_REGEX.containsMatchIn(clean)) {
      return Pair(TransactionType.INCOME, "Identified credit/deposit keyword")
    }

    if (EXPENSE_REGEX.containsMatchIn(clean)) {
      return Pair(TransactionType.EXPENSE, "Identified debit/spending keyword")
    }

    return Pair(null, "No explicit financial debit/credit verb found")
  }

  fun detectPaymentMethod(message: String): PaymentMethod {
    val clean = message.lowercase()
    return when {
      clean.contains("upi") || clean.contains("vpa") || clean.contains("@") -> PaymentMethod.UPI
      clean.contains("credit card") || clean.contains("card ending") || clean.contains("spent on card") -> PaymentMethod.CREDIT_CARD
      clean.contains("debit card") -> PaymentMethod.DEBIT_CARD
      clean.contains("netbanking") || clean.contains("net banking") || clean.contains("imps") || clean.contains("neft") || clean.contains("rtgs") -> PaymentMethod.NET_BANKING
      clean.contains("atm") || clean.contains("cash wdl") -> PaymentMethod.ATM_CASH
      else -> PaymentMethod.UPI
    }
  }
}
