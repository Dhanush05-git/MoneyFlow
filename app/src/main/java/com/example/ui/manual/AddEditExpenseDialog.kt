package com.example.ui.manual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.PaymentMethod
import com.example.model.TransactionEntity
import com.example.model.TransactionType
import com.example.ui.theme.CoralRose

@Composable
fun AddEditExpenseDialog(
  initialTransaction: TransactionEntity? = null,
  onDismiss: () -> Unit,
  onSave: (
    amountRupees: Double,
    merchant: String,
    category: String,
    account: String,
    paymentMethod: PaymentMethod,
    type: TransactionType,
    dateMillis: Long,
    notes: String?
  ) -> Unit,
  onDelete: ((TransactionEntity) -> Unit)? = null
) {
  var amountText by remember {
    mutableStateOf(initialTransaction?.let { "${it.amountRupees}" } ?: "")
  }
  var merchantText by remember {
    mutableStateOf(initialTransaction?.merchantName ?: "")
  }
  var selectedCategory by remember {
    mutableStateOf(initialTransaction?.categoryName ?: "Food & Dining")
  }
  var selectedMethod by remember {
    mutableStateOf(initialTransaction?.paymentMethod ?: PaymentMethod.UPI)
  }
  var selectedType by remember {
    mutableStateOf(initialTransaction?.type ?: TransactionType.EXPENSE)
  }
  var selectedAccount by remember {
    mutableStateOf(initialTransaction?.accountName ?: "HDFC Bank")
  }
  var notesText by remember {
    mutableStateOf(initialTransaction?.notes ?: "")
  }

  val categories = listOf(
    "Food & Dining", "Groceries", "Shopping", "Transport",
    "Bills & Utilities", "Subscriptions", "Entertainment", "Health"
  )

  val accounts = listOf("HDFC Bank", "SBI Savings", "ICICI Bank", "Credit Card", "Cash")

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
        .testTag("add_edit_dialog")
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (initialTransaction == null) "Add Expense" else "Edit Transaction",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )

          if (initialTransaction != null && onDelete != null) {
            IconButton(
              onClick = {
                onDelete(initialTransaction)
                onDismiss()
              }
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CoralRose)
            }
          }
        }

        // Amount input
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it },
          label = { Text("Amount (₹)") },
          leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 12.dp)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("amount_input"),
          singleLine = true
        )

        // Merchant input
        OutlinedTextField(
          value = merchantText,
          onValueChange = { merchantText = it },
          label = { Text("Merchant / Store Name") },
          placeholder = { Text("e.g. Swiggy, Starbucks, Amazon") },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("merchant_input"),
          singleLine = true
        )

        // Category Chips
        Text(
          text = "Category",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(categories) { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = { Text(cat) }
            )
          }
        }

        // Payment Method Chips
        Text(
          text = "Payment Method",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(PaymentMethod.entries) { method ->
            FilterChip(
              selected = selectedMethod == method,
              onClick = { selectedMethod = method },
              label = { Text(method.label) }
            )
          }
        }

        // Account Chips
        Text(
          text = "Account",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(accounts) { acc ->
            FilterChip(
              selected = selectedAccount == acc,
              onClick = { selectedAccount = acc },
              label = { Text(acc) }
            )
          }
        }

        // Notes
        OutlinedTextField(
          value = notesText,
          onValueChange = { notesText = it },
          label = { Text("Notes (Optional)") },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        // Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f)
          ) {
            Text("Cancel")
          }

          Button(
            onClick = {
              val amt = amountText.toDoubleOrNull() ?: 0.0
              if (amt > 0) {
                onSave(
                  amt,
                  merchantText,
                  selectedCategory,
                  selectedAccount,
                  selectedMethod,
                  selectedType,
                  initialTransaction?.occurredAt ?: System.currentTimeMillis(),
                  notesText.ifBlank { null }
                )
                onDismiss()
              }
            },
            modifier = Modifier
              .weight(1f)
              .testTag("save_expense_btn")
          ) {
            Text("Save", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
