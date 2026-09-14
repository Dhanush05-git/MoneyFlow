package com.example.capture.parsing.extractors

import com.example.model.PaymentMethod
import com.example.model.TransactionType

object DebitCreditClassifier {
  private val REFUND_REGEX = Regex("(?i)\\b(refund(ed)?|revers(ed|al)|cashback\\s*(credited|received)?)\\b")
  private val ATM_REGEX = Regex("(?i)\\b(atm\\s*w(ith)?d(rawa)?l|withdrawn\\s*from\\s*atm|cash\\s*withdrawal)\\b")
  private val CARD_PAYMENT_REGEX = Regex("(?i)\\b(payment\\s*(received\\s*)?towards\\s*(your\\s*)?credit\\s*card|credit\\s*card\\s*bill\\s*payment)\\b")
  private val TRANSFER_REGEX = Regex("(?i)\\b(transferred\\s*to\\s*(own|self)|self\\s*transfer|neft\\s*transfer\\s*to\\s*self)\\b")
  private val INCOME_REGEX = Regex("(?i)\\b(credited\\s*(to|in)?|received\\s*(from)?|deposited\\s*(to|in)?|salary\\s*credited|money\\s*received|payment\\s*received|added\\s*to\\s*(your\\s*)?(wallet|account)|you('ve|\\s*have)?\\s*received)\\b")
  private val EXPENSE_REGEX = Regex("(?i)\\b(debited\\s*(from|by)?|paid\\s*(to)?|spent\\s*(on|at)?|charged\\s*(on)?|purchase\\s*(at)?|sent\\s*(to)?|transferred\\s*(to)?|payment\\s*to|you('ve|\\s*have)?\\s*paid|txn\\s*of)\\b")

  fun classifyType(message: String): Pair<TransactionType, String> {
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

    // Default to EXPENSE if financial transaction was detected
    return Pair(TransactionType.EXPENSE, "Defaulted to expense from financial context")
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
