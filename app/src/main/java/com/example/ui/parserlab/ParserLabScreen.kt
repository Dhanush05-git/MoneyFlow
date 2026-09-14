package com.example.ui.parserlab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capture.ParseOutcome
import com.example.model.CaptureSource
import com.example.ui.ExpenseViewModel
import com.example.ui.theme.CoralRose
import com.example.ui.theme.Emerald500
import kotlinx.coroutines.launch

data class TestScenario(val label: String, val message: String)

@Composable
fun ParserLabScreen(
  viewModel: ExpenseViewModel,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val testScenarios = listOf(
    TestScenario(
      "Swiggy UPI",
      "Dear Customer, A/c XX4082 debited for Rs. 320.00 on 14-09-26 to VPA swiggy@icici via UPI Ref 425691028371"
    ),
    TestScenario(
      "Amazon Card",
      "INR 1,250.00 spent on your HDFC Bank Card ending in 8841 at AMZN MKTP IN on 14-Sep-26. Avl limit: Rs 1,42,000"
    ),
    TestScenario(
      "Ola UPI",
      "Paid Rs 220.00 to OLA CABS via Google Pay UPI Ref 425182910283"
    ),
    TestScenario(
      "Card Bill Payment",
      "Payment of Rs 12,000 received towards your HDFC Credit Card XX8841. Thank you."
    ),
    TestScenario(
      "Own Account Transfer",
      "Transferred Rs. 10,000 to own account SBI Savings via IMPS Ref 9182301928"
    ),
    TestScenario(
      "Refund",
      "Rs. 420.00 refunded to your A/c XX4082 from Swiggy for Order 49201. UPI Ref 4910293847"
    ),
    TestScenario(
      "Security OTP (Reject)",
      "Your SBI OTP for login is 928104. Do not share with anyone. Valid for 10 minutes."
    ),
    TestScenario(
      "Loan Marketing (Reject)",
      "Congratulations! You are pre-approved for an instant loan of Rs 5,00,000 at zero interest. Apply now."
    )
  )

  var inputMessage by remember { mutableStateOf(testScenarios.first().message) }
  var parseResult by remember { mutableStateOf<ParseOutcome?>(null) }
  val scope = rememberCoroutineScope()

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Parser Laboratory",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "Test on-device deterministic transaction extraction",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Button(
          onClick = onBack,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
          Text("Done", color = MaterialTheme.colorScheme.onSurface)
        }
      }
    }

    // Pre-loaded Scenario Chips
    item {
      Text(
        text = "Sample Test Scenarios",
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(6.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(testScenarios) { scenario ->
          FilterChip(
            selected = inputMessage == scenario.message,
            onClick = {
              inputMessage = scenario.message
              parseResult = null
            },
            label = { Text(scenario.label) }
          )
        }
      }
    }

    // Input Editor
    item {
      OutlinedTextField(
        value = inputMessage,
        onValueChange = {
          inputMessage = it
          parseResult = null
        },
        label = { Text("Raw SMS or Notification Text") },
        modifier = Modifier
          .fillMaxWidth()
          .height(140.dp)
          .testTag("parser_lab_input"),
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
      )
    }

    // Action button
    item {
      Button(
        onClick = {
          scope.launch {
            parseResult = viewModel.testParse(inputMessage, CaptureSource.SMS, "HDFCBK")
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("parse_now_btn"),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(14.dp)
      ) {
        Icon(Icons.Default.Science, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Execute Parser Engine", fontWeight = FontWeight.Bold)
      }
    }

    // Result Card
    item {
      val result = parseResult
      if (result != null) {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            when (result) {
              is ParseOutcome.Rejected -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    Icons.Default.Close,
                    contentDescription = "Rejected",
                    tint = CoralRose,
                    modifier = Modifier.size(24.dp)
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = "Message Filtered / Rejected",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CoralRose
                  )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                  text = "Reason: ${result.reason}",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              is ParseOutcome.Success -> {
                val c = result.candidate
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      Icons.Default.CheckCircle,
                      contentDescription = "Success",
                      tint = Emerald500,
                      modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                      text = "Transaction Extracted",
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                      color = Emerald500
                    )
                  }

                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Emerald500.copy(alpha = 0.15f)
                  ) {
                    Text(
                      text = "${c.confidenceScore}% Confidence",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = Emerald500,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Detail rows
                DetailRow("Amount", "₹${c.amountPaise / 100} (${c.amountPaise} paise)")
                DetailRow("Transaction Type", c.type.label)
                DetailRow("Merchant", c.merchantNormalized)
                DetailRow("Category", c.categoryCandidate)
                DetailRow("Payment Method", c.paymentMethod.label)
                DetailRow("Account / Bank", c.accountCandidate)
                DetailRow("Reference (UPI/UTR)", c.reference ?: "Not present")
                DetailRow("Fingerprint", c.fingerprint)

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                  text = "Matched Deterministic Rules:",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                for (r in c.confidenceReasons) {
                  Text(
                    text = "  ✓ $r",
                    style = MaterialTheme.typography.bodySmall,
                    color = Emerald500
                  )
                }
              }
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(80.dp))
    }
  }
}

@Composable
fun DetailRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}
