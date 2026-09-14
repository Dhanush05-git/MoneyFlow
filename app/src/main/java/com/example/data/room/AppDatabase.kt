package com.example.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.AccountEntity
import com.example.model.AccountType
import com.example.model.BudgetEntity
import com.example.model.CategoryEntity
import com.example.model.DiagnosticEventEntity
import com.example.model.MerchantRuleEntity
import com.example.model.PaymentMethod
import com.example.model.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
  entities = [
    TransactionEntity::class,
    AccountEntity::class,
    CategoryEntity::class,
    MerchantRuleEntity::class,
    BudgetEntity::class,
    DiagnosticEventEntity::class
  ],
  version = 1,
  exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
  abstract fun transactionDao(): TransactionDao
  abstract fun accountDao(): AccountDao
  abstract fun categoryDao(): CategoryDao
  abstract fun merchantRuleDao(): MerchantRuleDao
  abstract fun budgetDao(): BudgetDao
  abstract fun diagnosticDao(): DiagnosticDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "moneyflow_ledger_db"
        ).addCallback(DatabaseCallback())
         .fallbackToDestructiveMigration()
         .build()
        INSTANCE = instance
        instance
      }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        // Seed default categories, accounts, and merchant rules on first run
        CoroutineScope(Dispatchers.IO).launch {
          INSTANCE?.let { database ->
            seedDefaults(database)
          }
        }
      }
    }

    suspend fun seedDefaults(database: AppDatabase) {
      val defaultCategories = listOf(
        CategoryEntity(name = "Food", iconName = "restaurant", colorHex = "#F97316", isEssential = true),
        CategoryEntity(name = "Groceries & Household", iconName = "shopping_cart", colorHex = "#10B981", isEssential = true),
        CategoryEntity(name = "Housing", iconName = "home", colorHex = "#6366F1", isEssential = true),
        CategoryEntity(name = "Bills & Utilities", iconName = "receipt_long", colorHex = "#EF4444", isEssential = true),
        CategoryEntity(name = "Transport", iconName = "directions_car", colorHex = "#0EA5E9", isEssential = true),
        CategoryEntity(name = "Health", iconName = "local_hospital", colorHex = "#EC4899", isEssential = true),
        CategoryEntity(name = "Personal Care", iconName = "spa", colorHex = "#8B5CF6", isEssential = true),
        CategoryEntity(name = "Shopping", iconName = "shopping_bag", colorHex = "#F59E0B", isEssential = false),
        CategoryEntity(name = "Entertainment", iconName = "movie", colorHex = "#D946EF", isEssential = false),
        CategoryEntity(name = "Travel", iconName = "flight", colorHex = "#14B8A6", isEssential = false),
        CategoryEntity(name = "Subscriptions", iconName = "subscriptions", colorHex = "#84CC16", isEssential = false),
        CategoryEntity(name = "Dining Out", iconName = "local_cafe", colorHex = "#FB923C", isEssential = false),
        CategoryEntity(name = "Education", iconName = "school", colorHex = "#3B82F6", isEssential = false),
        CategoryEntity(name = "Family", iconName = "people", colorHex = "#A855F7", isEssential = false),
        CategoryEntity(name = "Gifts", iconName = "redeem", colorHex = "#F43F5E", isEssential = false),
        CategoryEntity(name = "Work", iconName = "work", colorHex = "#64748B", isEssential = false),
        CategoryEntity(name = "Finance & Fees", iconName = "account_balance", colorHex = "#78716C", isEssential = false),
        CategoryEntity(name = "Other", iconName = "more_horiz", colorHex = "#94A3B8", isEssential = false)
      )
      database.categoryDao().insertAll(defaultCategories)

      val defaultAccounts = listOf(
        AccountEntity(name = "Primary Bank", type = AccountType.SAVINGS, mask = null, balancePaise = 0L, colorHex = "#004B87"),
        AccountEntity(name = "Cash Wallet", type = AccountType.CASH, mask = null, balancePaise = 0L, colorHex = "#059669")
      )
      database.accountDao().insertAll(defaultAccounts)

      // Seed core merchant memory rules for Indian merchants
      val defaultMerchantRules = listOf(
        MerchantRuleEntity(normalizedMerchant = "Swiggy", defaultCategory = "Food", defaultPaymentMethod = PaymentMethod.UPI),
        MerchantRuleEntity(normalizedMerchant = "Zomato", defaultCategory = "Food", defaultPaymentMethod = PaymentMethod.UPI),
        MerchantRuleEntity(normalizedMerchant = "Blinkit", defaultCategory = "Groceries & Household", defaultPaymentMethod = PaymentMethod.UPI),
        MerchantRuleEntity(normalizedMerchant = "Zepto", defaultCategory = "Groceries & Household", defaultPaymentMethod = PaymentMethod.UPI),
        MerchantRuleEntity(normalizedMerchant = "Instamart", defaultCategory = "Groceries & Household", defaultPaymentMethod = PaymentMethod.UPI),
        MerchantRuleEntity(normalizedMerchant = "Uber", defaultCategory = "Transport", defaultPaymentMethod = PaymentMethod.UPI),
        MerchantRuleEntity(normalizedMerchant = "Ola", defaultCategory = "Transport", defaultPaymentMethod = PaymentMethod.UPI),
        MerchantRuleEntity(normalizedMerchant = "Amazon", defaultCategory = "Shopping", defaultPaymentMethod = PaymentMethod.CREDIT_CARD),
        MerchantRuleEntity(normalizedMerchant = "Flipkart", defaultCategory = "Shopping", defaultPaymentMethod = PaymentMethod.CREDIT_CARD),
        MerchantRuleEntity(normalizedMerchant = "Netflix", defaultCategory = "Subscriptions", defaultPaymentMethod = PaymentMethod.CREDIT_CARD),
        MerchantRuleEntity(normalizedMerchant = "Spotify", defaultCategory = "Subscriptions", defaultPaymentMethod = PaymentMethod.CREDIT_CARD),
        MerchantRuleEntity(normalizedMerchant = "Starbucks", defaultCategory = "Dining Out", defaultPaymentMethod = PaymentMethod.UPI),
        MerchantRuleEntity(normalizedMerchant = "Shell", defaultCategory = "Transport", defaultPaymentMethod = PaymentMethod.DEBIT_CARD),
        MerchantRuleEntity(normalizedMerchant = "DMart", defaultCategory = "Groceries & Household", defaultPaymentMethod = PaymentMethod.UPI)
      )
      for (rule in defaultMerchantRules) {
        database.merchantRuleDao().insertOrUpdate(rule)
      }
    }
  }
}
