package com.example.model

enum class TransactionType(val label: String) {
  EXPENSE("Expense"),
  INCOME("Income"),
  TRANSFER("Transfer"),
  REFUND("Refund"),
  WITHDRAWAL("ATM Withdrawal"),
  CARD_PAYMENT("Card Bill Payment")
}

enum class CaptureSource(val label: String) {
  SMS("SMS"),
  NOTIFICATION("Notification"),
  IMPORT("Import"),
  MANUAL("Manual")
}

enum class PaymentMethod(val label: String) {
  UPI("UPI"),
  CREDIT_CARD("Credit Card"),
  DEBIT_CARD("Debit Card"),
  NET_BANKING("Net Banking"),
  ATM_CASH("ATM / Cash"),
  CASH("Cash"),
  OTHER("Other")
}

enum class TransactionStatus {
  CONFIRMED,
  NEEDS_REVIEW,
  IGNORED
}

enum class AccountType(val label: String) {
  SAVINGS("Savings Account"),
  CREDIT_CARD("Credit Card"),
  WALLET("Wallet"),
  CASH("Cash")
}
