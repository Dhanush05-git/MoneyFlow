package com.example.capture.parsing

import com.example.capture.ParseOutcome
import com.example.capture.TransactionCandidate
import com.example.capture.parsing.classifier.SpamClassifier
import com.example.capture.parsing.confidence.ConfidenceScorer
import com.example.capture.parsing.extractors.AccountExtractor
import com.example.capture.parsing.extractors.AmountExtractor
import com.example.capture.parsing.extractors.DebitCreditClassifier
import com.example.capture.parsing.extractors.MerchantExtractor
import com.example.capture.parsing.extractors.ReferenceExtractor
import com.example.capture.parsing.normalizer.MerchantNormalizer
import com.example.data.room.MerchantRuleDao
import com.example.model.CaptureSource

class TransactionParserEngine(private val merchantRuleDao: MerchantRuleDao? = null) {

  suspend fun parse(
    rawText: String,
    source: CaptureSource,
    sourceAppOrSender: String? = null,
    occurredAt: Long = System.currentTimeMillis()
  ): ParseOutcome {
    val clean = rawText.trim()
    if (clean.isBlank()) {
      return ParseOutcome.Rejected("Empty message", "")
    }

    // Layer 1 & 2: Spam / OTP / Marketing rejection
    val (isSpam, spamReason) = SpamClassifier.isSpamOrIgnored(clean)
    if (isSpam) {
      return ParseOutcome.Rejected(spamReason ?: "Filtered by spam/privacy engine", clean.take(120))
    }

    // Layer 3: Amount extraction
    val amountPaise = AmountExtractor.extractAmountPaise(clean)
    if (amountPaise == null || amountPaise <= 0L) {
      return ParseOutcome.Rejected("No valid monetary amount detected", clean.take(120))
    }

    // Layer 4: Debit vs Credit & Payment method
    val (txType, _) = DebitCreditClassifier.classifyType(clean)
    val paymentMethod = DebitCreditClassifier.detectPaymentMethod(clean)

    // Layer 5: Merchant extraction & normalization
    val rawMerchant = MerchantExtractor.extractMerchant(clean)
    val normalizedMerchant = MerchantNormalizer.normalize(rawMerchant)

    // Layer 6: Merchant category memory lookup
    var userTrainedRule = false
    var category = MerchantNormalizer.guessCategory(normalizedMerchant)
    if (merchantRuleDao != null) {
      val rule = merchantRuleDao.getRuleForMerchant(normalizedMerchant)
      if (rule != null) {
        category = rule.defaultCategory
        userTrainedRule = true
      }
    }

    // Layer 7: Reference & Account extraction
    val reference = ReferenceExtractor.extractReference(clean)
    val account = AccountExtractor.extractAccount(clean, sourceAppOrSender)

    // Layer 8: Confidence scoring
    val (confidence, reasons) = ConfidenceScorer.scoreConfidence(
      amountPaise = amountPaise,
      type = txType,
      merchant = normalizedMerchant,
      reference = reference,
      account = account,
      paymentMethod = paymentMethod,
      hasUserMerchantRule = userTrainedRule
    )

    val candidate = TransactionCandidate(
      amountPaise = amountPaise,
      currency = "INR",
      type = txType,
      occurredAt = occurredAt,
      merchantRaw = rawMerchant,
      merchantNormalized = normalizedMerchant,
      categoryCandidate = category,
      accountCandidate = account,
      paymentMethod = paymentMethod,
      source = source,
      sourceApp = sourceAppOrSender,
      reference = reference,
      rawMessageSnippet = clean.take(180),
      confidenceScore = confidence,
      confidenceReasons = reasons
    )

    return ParseOutcome.Success(candidate)
  }
}
