package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.CaptureSource
import com.example.model.PaymentMethod
import com.example.model.TransactionEntity
import com.example.model.TransactionStatus
import com.example.model.TransactionType
import com.example.ui.components.TransactionListItem
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun transaction_list_item_screenshot() {
    val sampleTx = TransactionEntity(
      id = 1L,
      amountPaise = 32000L,
      currency = "INR",
      type = TransactionType.EXPENSE,
      status = TransactionStatus.CONFIRMED,
      occurredAt = 1789456800000L,
      merchantName = "Swiggy",
      categoryName = "Food & Dining",
      accountName = "HDFC Bank",
      paymentMethod = PaymentMethod.UPI,
      source = CaptureSource.SMS,
      confidenceScore = 98,
      reference = "425691028371"
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        TransactionListItem(
          transaction = sampleTx,
          onClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/transaction_item.png")
  }
}
