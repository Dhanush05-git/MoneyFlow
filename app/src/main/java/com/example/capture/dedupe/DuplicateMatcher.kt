package com.example.capture.dedupe

import com.example.capture.TransactionCandidate
import com.example.data.room.TransactionDao
import com.example.model.TransactionEntity

sealed class DuplicateCheckResult {
  object NoDuplicate : DuplicateCheckResult()
  data class ExactReferenceMatch(val existing: TransactionEntity) : DuplicateCheckResult()
  data class TimeWindowMatch(val existing: TransactionEntity, val minutesDiff: Long) : DuplicateCheckResult()
  data class SameDaySameAmountMatch(val existing: TransactionEntity) : DuplicateCheckResult()
}

class DuplicateMatcher(private val transactionDao: TransactionDao) {

  suspend fun checkCandidate(candidate: TransactionCandidate): DuplicateCheckResult {
    // Tier 1: Reference match (e.g. UPI Ref / UTR / RRN)
    val ref = candidate.reference
    if (!ref.isNullOrBlank()) {
      val existingByRef = transactionDao.getByReference(ref)
      if (existingByRef != null) {
        return DuplicateCheckResult.ExactReferenceMatch(existingByRef)
      }
    }

    // Check fingerprint
    val existingByFingerprint = transactionDao.getByFingerprint(candidate.fingerprint)
    if (existingByFingerprint != null) {
      return DuplicateCheckResult.ExactReferenceMatch(existingByFingerprint)
    }

    // Tier 2: Check for same amount within +/- 15 minutes (900,000 ms)
    val fifteenMins = 15 * 60 * 1000L
    val nearbyTransactions = transactionDao.findSimilarTransactions(
      amountPaise = candidate.amountPaise,
      startTime = candidate.occurredAt - fifteenMins,
      endTime = candidate.occurredAt + fifteenMins
    )

    for (existing in nearbyTransactions) {
      val sameMerchant = existing.merchantName.equals(candidate.merchantNormalized, ignoreCase = true)
      if (sameMerchant) {
        val diffMins = Math.abs(candidate.occurredAt - existing.occurredAt) / (60 * 1000L)
        return DuplicateCheckResult.TimeWindowMatch(existing, diffMins)
      }
    }

    // Tier 3: Same day + same amount + same merchant (potential duplicate warning)
    val oneDay = 24 * 60 * 60 * 1000L
    val dayTransactions = transactionDao.findSimilarTransactions(
      amountPaise = candidate.amountPaise,
      startTime = candidate.occurredAt - oneDay,
      endTime = candidate.occurredAt + oneDay
    )
    for (existing in dayTransactions) {
      val sameMerchant = existing.merchantName.equals(candidate.merchantNormalized, ignoreCase = true)
      if (sameMerchant && existing.id != 0L) {
        return DuplicateCheckResult.SameDaySameAmountMatch(existing)
      }
    }

    return DuplicateCheckResult.NoDuplicate
  }
}
