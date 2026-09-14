package com.example.data.room

import androidx.room.TypeConverter
import com.example.model.AccountType
import com.example.model.CaptureSource
import com.example.model.PaymentMethod
import com.example.model.TransactionStatus
import com.example.model.TransactionType

class Converters {
  @TypeConverter
  fun fromTransactionType(value: TransactionType): String = value.name

  @TypeConverter
  fun toTransactionType(value: String): TransactionType = try {
    TransactionType.valueOf(value)
  } catch (e: Exception) {
    TransactionType.EXPENSE
  }

  @TypeConverter
  fun fromTransactionStatus(value: TransactionStatus): String = value.name

  @TypeConverter
  fun toTransactionStatus(value: String): TransactionStatus = try {
    TransactionStatus.valueOf(value)
  } catch (e: Exception) {
    TransactionStatus.CONFIRMED
  }

  @TypeConverter
  fun fromCaptureSource(value: CaptureSource): String = value.name

  @TypeConverter
  fun toCaptureSource(value: String): CaptureSource = try {
    CaptureSource.valueOf(value)
  } catch (e: Exception) {
    CaptureSource.MANUAL
  }

  @TypeConverter
  fun fromPaymentMethod(value: PaymentMethod): String = value.name

  @TypeConverter
  fun toPaymentMethod(value: String): PaymentMethod = try {
    PaymentMethod.valueOf(value)
  } catch (e: Exception) {
    PaymentMethod.UPI
  }

  @TypeConverter
  fun fromAccountType(value: AccountType): String = value.name

  @TypeConverter
  fun toAccountType(value: String): AccountType = try {
    AccountType.valueOf(value)
  } catch (e: Exception) {
    AccountType.SAVINGS
  }
}
