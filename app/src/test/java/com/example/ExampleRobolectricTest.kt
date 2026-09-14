package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.capture.TransactionCapturePipeline
import com.example.capture.parsing.classifier.SpamClassifier
import com.example.capture.parsing.extractors.AmountExtractor
import com.example.capture.parsing.extractors.MerchantExtractor
import com.example.capture.parsing.extractors.ReferenceExtractor
import com.example.data.room.AppDatabase
import com.example.model.CaptureSource
import com.example.model.PaymentMethod
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun testAppNameResource() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Expense Ledger", appName)
  }

  @Test
  fun testSpamClassifierFiltersOtp() {
    val otpSms = "Your SBI OTP for login is 928104. Do not share with anyone. Valid for 10 minutes."
    assertTrue(SpamClassifier.isSpamOrIgnored(otpSms).first)

    val promoSms = "Congratulations! You are pre-approved for an instant loan of Rs 5,00,000 at zero interest."
    assertTrue(SpamClassifier.isSpamOrIgnored(promoSms).first)

    val loanSpam1 = "Dear Customer, you are eligible for an instant personal loan of Rs 3,00,000 at low interest. Apply now."
    assertTrue(SpamClassifier.isSpamOrIgnored(loanSpam1).first)

    val loanSpam2 = "Special offer! Get pre-approved credit card with limit upto Rs 1,50,000. No paperwork required."
    assertTrue(SpamClassifier.isSpamOrIgnored(loanSpam2).first)

    val billDueSpam = "Reminder: Your credit card bill of Rs 4,500 is due on 20-Sep-26. Pay now to avoid late fee."
    assertTrue(SpamClassifier.isSpamOrIgnored(billDueSpam).first)

    val realTxSms = "Dear Customer, A/c XX4082 debited for Rs. 320.00 on 14-09-26 to VPA swiggy@icici via UPI Ref 425691028371"
    assertFalse(SpamClassifier.isSpamOrIgnored(realTxSms).first)

    val realReceivedSms = "Dear Customer, A/c XX4082 credited for Rs. 1,500.00 on 14-09-26 from rahul@okhdfcbank UPI Ref 583920194821"
    assertFalse(SpamClassifier.isSpamOrIgnored(realReceivedSms).first)
  }

  @Test
  fun testMoneyReceivedCapture() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getDatabase(context)
    val pipeline = TransactionCapturePipeline(db)

    val rawNotification = "You received ₹2,500.00 from Amit Kumar via PhonePe UPI Ref 837492819283"
    val tx = pipeline.processIncomingMessage(
      rawText = rawNotification,
      source = CaptureSource.NOTIFICATION,
      sourceAppOrSender = "PhonePe",
      occurredAt = System.currentTimeMillis()
    )

    assertNotNull(tx)
    assertEquals(250000L, tx!!.amountPaise)
    assertEquals(com.example.model.TransactionType.INCOME, tx.type)
  }

  @Test
  fun testAmountExtraction() {
    val sms = "INR 1,250.00 spent on your HDFC Bank Card ending in 8841 at AMZN MKTP IN on 14-Sep-26"
    val amountPaise = AmountExtractor.extractAmountPaise(sms)
    assertNotNull(amountPaise)
    assertEquals(125000L, amountPaise)
  }

  @Test
  fun testMerchantExtraction() {
    val sms = "Dear Customer, A/c XX4082 debited for Rs. 320.00 on 14-09-26 to VPA swiggy@icici via UPI Ref 425691028371"
    val merchant = MerchantExtractor.extractMerchant(sms)
    assertEquals("Swiggy", merchant)
  }

  @Test
  fun testReferenceExtraction() {
    val sms = "Paid Rs 220.00 to OLA CABS via Google Pay UPI Ref 425182910283"
    val ref = ReferenceExtractor.extractReference(sms)
    assertEquals("425182910283", ref)
  }

  @Test
  fun testEndToEndCapturePipeline() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getDatabase(context)
    val pipeline = TransactionCapturePipeline(db)

    val rawSms = "Dear Customer, A/c XX4082 debited for Rs. 320.00 on 14-09-26 to VPA swiggy@icici via UPI Ref 425691028371"
    val tx = pipeline.processIncomingMessage(
      rawText = rawSms,
      source = CaptureSource.SMS,
      sourceAppOrSender = "HDFCBK",
      occurredAt = System.currentTimeMillis()
    )

    assertNotNull(tx)
    assertEquals(32000L, tx!!.amountPaise)
    assertEquals("Swiggy", tx.merchantName)
    assertEquals("Food", tx.categoryName)
    assertEquals(PaymentMethod.UPI, tx.paymentMethod)
    assertEquals("425691028371", tx.reference)

    // Re-processing the exact same transaction should detect duplicate and return null
    val duplicateTx = pipeline.processIncomingMessage(
      rawText = rawSms,
      source = CaptureSource.SMS,
      sourceAppOrSender = "HDFCBK",
      occurredAt = System.currentTimeMillis()
    )
    assertEquals(null, duplicateTx)
  }
}
