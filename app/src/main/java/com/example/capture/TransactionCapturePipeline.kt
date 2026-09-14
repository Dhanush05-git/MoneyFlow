package com.example.capture

import com.example.capture.dedupe.DuplicateCheckResult
import com.example.capture.dedupe.DuplicateMatcher
import com.example.capture.parsing.TransactionParserEngine
import com.example.data.room.AppDatabase
import com.example.model.CaptureSource
import com.example.model.DiagnosticEventEntity
import com.example.model.TransactionEntity
import com.example.model.TransactionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TransactionCapturePipeline(
  private val database: AppDatabase,
  private val context: android.content.Context? = null
) {

  private val parserEngine = TransactionParserEngine(database.merchantRuleDao(), context)
  private val duplicateMatcher = DuplicateMatcher(database.transactionDao())

  suspend fun processIncomingMessage(
    rawText: String,
    source: CaptureSource,
    sourceAppOrSender: String?,
    occurredAt: Long = System.currentTimeMillis(),
    autoApproveThreshold: Int = 95
  ): TransactionEntity? = withContext(Dispatchers.IO) {
    val outcome = parserEngine.parse(rawText, source, sourceAppOrSender, occurredAt)

    when (outcome) {
      is ParseOutcome.Rejected -> {
        val event = DiagnosticEventEntity(
          timestamp = System.currentTimeMillis(),
          source = source,
          senderOrApp = sourceAppOrSender ?: "Unknown",
          eventType = "REJECTED",
          summary = outcome.reason,
          rawSnippet = outcome.rawSnippet
        )
        database.diagnosticDao().insert(event)
        null
      }

      is ParseOutcome.Success -> {
        var candidate = outcome.candidate

        // Deduplication Check
        val dupResult = duplicateMatcher.checkCandidate(candidate)
        when (dupResult) {
          is DuplicateCheckResult.ExactReferenceMatch -> {
            val event = DiagnosticEventEntity(
              timestamp = System.currentTimeMillis(),
              source = source,
              senderOrApp = sourceAppOrSender ?: "Unknown",
              eventType = "DUPLICATE_PREVENTED",
              summary = "Ignored duplicate transaction #${dupResult.existing.id} (Matched reference/fingerprint)",
              rawSnippet = candidate.rawMessageSnippet
            )
            database.diagnosticDao().insert(event)
            return@withContext null
          }

          is DuplicateCheckResult.TimeWindowMatch -> {
            val event = DiagnosticEventEntity(
              timestamp = System.currentTimeMillis(),
              source = source,
              senderOrApp = sourceAppOrSender ?: "Unknown",
              eventType = "DUPLICATE_PREVENTED",
              summary = "Ignored duplicate within ${dupResult.minutesDiff}m of existing txn #${dupResult.existing.id}",
              rawSnippet = candidate.rawMessageSnippet
            )
            database.diagnosticDao().insert(event)
            return@withContext null
          }

          is DuplicateCheckResult.SameDaySameAmountMatch -> {
            candidate = candidate.copy(
              isPotentialDuplicate = true,
              duplicateReason = "Possible duplicate of transaction #${dupResult.existing.id} from today"
            )
          }

          DuplicateCheckResult.NoDuplicate -> {
            // All clear
          }
        }

        val entity = candidate.toEntity(autoApproveThreshold)
        val insertedId = database.transactionDao().insert(entity)
        val finalEntity = entity.copy(id = insertedId)

        val eventType = if (finalEntity.status == TransactionStatus.CONFIRMED) "AUTO_CAPTURED" else "QUEUED_FOR_REVIEW"
        val diag = DiagnosticEventEntity(
          timestamp = System.currentTimeMillis(),
          source = source,
          senderOrApp = sourceAppOrSender ?: "Unknown",
          eventType = eventType,
          summary = "${finalEntity.formatRupees()} at ${finalEntity.merchantName} (${finalEntity.confidenceScore}% confidence)",
          rawSnippet = candidate.rawMessageSnippet
        )
        database.diagnosticDao().insert(diag)

        finalEntity
      }
    }
  }
}
