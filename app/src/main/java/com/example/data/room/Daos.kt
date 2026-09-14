package com.example.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.AccountEntity
import com.example.model.BudgetEntity
import com.example.model.CategoryEntity
import com.example.model.DiagnosticEventEntity
import com.example.model.MerchantRuleEntity
import com.example.model.TransactionEntity
import com.example.model.TransactionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
  @Query("SELECT * FROM transactions WHERE status != 'IGNORED' ORDER BY occurredAt DESC")
  fun getAllActiveTransactions(): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM transactions WHERE status = 'NEEDS_REVIEW' ORDER BY occurredAt DESC")
  fun getTransactionsNeedingReview(): Flow<List<TransactionEntity>>

  @Query("SELECT COUNT(*) FROM transactions WHERE status = 'NEEDS_REVIEW'")
  fun getReviewCount(): Flow<Int>

  @Query("SELECT * FROM transactions WHERE id = :id")
  suspend fun getById(id: Long): TransactionEntity?

  @Query("SELECT * FROM transactions WHERE reference = :reference LIMIT 1")
  suspend fun getByReference(reference: String): TransactionEntity?

  @Query("SELECT * FROM transactions WHERE fingerprint = :fingerprint LIMIT 1")
  suspend fun getByFingerprint(fingerprint: String): TransactionEntity?

  @Query("""
    SELECT * FROM transactions 
    WHERE amountPaise = :amountPaise 
      AND occurredAt BETWEEN :startTime AND :endTime
      AND status != 'IGNORED'
    LIMIT 10
  """)
  suspend fun findSimilarTransactions(
    amountPaise: Long,
    startTime: Long,
    endTime: Long
  ): List<TransactionEntity>

  @Query("""
    SELECT * FROM transactions 
    WHERE occurredAt BETWEEN :startOfDay AND :endOfDay 
      AND status != 'IGNORED'
    ORDER BY occurredAt DESC
  """)
  fun getTransactionsForDateRange(startOfDay: Long, endOfDay: Long): Flow<List<TransactionEntity>>

  @Query("""
    SELECT * FROM transactions 
    WHERE occurredAt BETWEEN :startOfDay AND :endOfDay 
      AND status != 'IGNORED'
    ORDER BY occurredAt DESC
  """)
  suspend fun getTransactionsForDateRangeSync(startOfDay: Long, endOfDay: Long): List<TransactionEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(transaction: TransactionEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(transactions: List<TransactionEntity>)

  @Update
  suspend fun update(transaction: TransactionEntity)

  @Delete
  suspend fun delete(transaction: TransactionEntity)

  @Query("UPDATE transactions SET status = :newStatus WHERE id = :id")
  suspend fun updateStatus(id: Long, newStatus: TransactionStatus)

  @Query("DELETE FROM transactions")
  suspend fun deleteAllTransactions()
}

@Dao
interface AccountDao {
  @Query("SELECT * FROM accounts ORDER BY name ASC")
  fun getAllAccounts(): Flow<List<AccountEntity>>

  @Query("SELECT * FROM accounts WHERE name = :name LIMIT 1")
  suspend fun getByName(name: String): AccountEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(account: AccountEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(accounts: List<AccountEntity>)

  @Update
  suspend fun update(account: AccountEntity)

  @Delete
  suspend fun delete(account: AccountEntity)
}

@Dao
interface CategoryDao {
  @Query("SELECT * FROM categories ORDER BY isEssential DESC, name ASC")
  fun getAllCategories(): Flow<List<CategoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(categories: List<CategoryEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(category: CategoryEntity): Long
}

@Dao
interface MerchantRuleDao {
  @Query("SELECT * FROM merchant_rules WHERE normalizedMerchant = :normalizedMerchant LIMIT 1")
  suspend fun getRuleForMerchant(normalizedMerchant: String): MerchantRuleEntity?

  @Query("SELECT * FROM merchant_rules ORDER BY userTrainedCount DESC, normalizedMerchant ASC")
  fun getAllRules(): Flow<List<MerchantRuleEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(rule: MerchantRuleEntity)
}

@Dao
interface BudgetDao {
  @Query("SELECT * FROM budgets WHERE monthYear = :monthYear")
  fun getBudgetsForMonth(monthYear: String): Flow<List<BudgetEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(budget: BudgetEntity): Long
}

@Dao
interface DiagnosticDao {
  @Query("SELECT * FROM diagnostic_events ORDER BY timestamp DESC LIMIT 100")
  fun getRecentEvents(): Flow<List<DiagnosticEventEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(event: DiagnosticEventEntity): Long

  @Query("DELETE FROM diagnostic_events WHERE id NOT IN (SELECT id FROM diagnostic_events ORDER BY timestamp DESC LIMIT 200)")
  suspend fun pruneOldEvents()
}
