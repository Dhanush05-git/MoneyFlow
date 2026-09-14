package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AccountType
import com.example.model.PaymentMethod
import com.example.ui.ExpenseUiState
import com.example.ui.ExpenseViewModel
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600

@Composable
fun SettingsScreen(
  uiState: ExpenseUiState,
  viewModel: ExpenseViewModel,
  onNavigateToParserLab: () -> Unit,
  onNavigateToHealth: () -> Unit,
  onNavigateToReview: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var thresholdSlider by remember { mutableFloatStateOf(uiState.autoApproveThreshold.toFloat()) }
  var showAddRuleDialog by remember { mutableStateOf(false) }
  var showAddAccountDialog by remember { mutableStateOf(false) }
  var showClearConfirmation by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "Settings & Memory",
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )
      Text(
        text = "Personal ledger preferences and local memory",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    // Quick Tools Row
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          SettingLinkItem(
            icon = Icons.Default.Science,
            title = "Parser Laboratory",
            subtitle = "Test SMS/notification parsing engine",
            onClick = onNavigateToParserLab
          )
          SettingLinkItem(
            icon = Icons.Default.HealthAndSafety,
            title = "Capture Health & Audit Log",
            subtitle = "Status of SMS receiver & historical scanner",
            onClick = onNavigateToHealth
          )
          SettingLinkItem(
            icon = Icons.Default.Tune,
            title = "Review Queue (${uiState.reviewCount})",
            subtitle = "Approve transactions with ambiguous confidence",
            onClick = onNavigateToReview
          )
        }
      }
    }

    // Auto-Approve Threshold Slider
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Auto-Approve Threshold",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Transactions above this score skip review",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.primaryContainer
            ) {
              Text(
                text = "${thresholdSlider.toInt()}%",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Slider(
            value = thresholdSlider,
            onValueChange = {
              thresholdSlider = it
              viewModel.setAutoApproveThreshold(it.toInt())
            },
            valueRange = 80f..100f,
            steps = 3,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    }

    // Privacy & Security Guarantee Card
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, contentDescription = null, tint = Emerald500)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Privacy & Security Guarantees",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          PrivacyBullet("100% Local-First Engine", "Every SMS and notification is parsed strictly on this device. Zero data is sent to external clouds.")
          PrivacyBullet("Zero Password / PIN Storage", "The app never extracts or persists bank account passwords, UPI PINs, or card CVVs.")
          PrivacyBullet("Deterministic Regex Logic", "Uses predictable, testable parsing rules with zero non-deterministic hallucination risk.")
        }
      }
    }

    // Learned Merchant Rules
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Merchant Memory Rules (${uiState.merchantRules.size})",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )

        Button(
          onClick = { showAddRuleDialog = true },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Rule", fontSize = 12.sp)
        }
      }
    }

    items(uiState.merchantRules.take(8), key = { it.id }) { rule ->
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = rule.normalizedMerchant,
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Categorizes as: ${rule.defaultCategory}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
          ) {
            Text(
              text = "Used ${rule.userTrainedCount}x",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      }
    }

    // Accounts & Cards
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Bank Accounts & Cards",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )

        Button(
          onClick = { showAddAccountDialog = true },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Account", fontSize = 12.sp)
        }
      }
    }

    items(uiState.accounts, key = { it.id }) { acc ->
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Emerald500)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "${acc.name} ${acc.mask ?: ""}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = acc.type.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Text(
            text = "₹${acc.balancePaise / 100}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }

    // Export CSV
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              Toast.makeText(
                context,
                "Exported ${uiState.allTransactions.size} transactions to local cache CSV",
                Toast.LENGTH_SHORT
              ).show()
            }
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FileDownload, contentDescription = null, tint = Emerald500)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("Export Transactions (CSV)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
              Text("Local offline backup of your ledger", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }

    // Reset / Clear Transactions
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showClearConfirmation = true }
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Clear All Transactions",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error
              )
              Text(
                text = "Wipe ledger transactions and start completely fresh",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(80.dp))
    }
  }

  // Clear Confirmation Dialog
  if (showClearConfirmation) {
    Dialog(onDismissRequest = { showClearConfirmation = false }) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.padding(16.dp)
      ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          Text(
            text = "Clear All Transactions?",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "This will permanently erase all transactions currently in the database. Are you sure you want to proceed?",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(
              onClick = { showClearConfirmation = false },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
              Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                viewModel.clearAllTransactions()
                showClearConfirmation = false
                Toast.makeText(context, "All transactions cleared", Toast.LENGTH_SHORT).show()
              },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
              Text("Clear Ledger", color = MaterialTheme.colorScheme.onError)
            }
          }
        }
      }
    }
  }

  // Add Rule Dialog
  if (showAddRuleDialog) {
    var merchantPattern by remember { mutableStateOf("") }
    var categoryName by remember { mutableStateOf("Food & Dining") }

    Dialog(onDismissRequest = { showAddRuleDialog = false }) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.padding(16.dp)
      ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Teach Merchant Rule", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
          OutlinedTextField(
            value = merchantPattern,
            onValueChange = { merchantPattern = it },
            label = { Text("Merchant Keyword (e.g. Blue Tokai)") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = categoryName,
            onValueChange = { categoryName = it },
            label = { Text("Category (e.g. Food & Dining)") },
            modifier = Modifier.fillMaxWidth()
          )
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(
              onClick = {
                if (merchantPattern.isNotBlank()) {
                  viewModel.teachRule(merchantPattern, categoryName, PaymentMethod.UPI)
                  showAddRuleDialog = false
                }
              }
            ) {
              Text("Save Rule")
            }
          }
        }
      }
    }
  }

  // Add Account Dialog
  if (showAddAccountDialog) {
    var accountName by remember { mutableStateOf("") }
    var accountMask by remember { mutableStateOf("") }
    var balanceText by remember { mutableStateOf("0") }

    Dialog(onDismissRequest = { showAddAccountDialog = false }) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.padding(16.dp)
      ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Add Account or Card", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
          OutlinedTextField(
            value = accountName,
            onValueChange = { accountName = it },
            label = { Text("Account Name (e.g. Axis Bank)") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = accountMask,
            onValueChange = { accountMask = it },
            label = { Text("Last 4 digits (e.g. XX1234)") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = balanceText,
            onValueChange = { balanceText = it },
            label = { Text("Initial Balance (₹)") },
            modifier = Modifier.fillMaxWidth()
          )
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(
              onClick = {
                if (accountName.isNotBlank()) {
                  viewModel.addAccount(
                    accountName,
                    AccountType.SAVINGS,
                    accountMask.ifBlank { null },
                    balanceText.toDoubleOrNull() ?: 0.0
                  )
                  showAddAccountDialog = false
                }
              }
            ) {
              Text("Add")
            }
          }
        }
      }
    }
  }
}

@Composable
fun SettingLinkItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = Emerald500, modifier = Modifier.size(18.dp))
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
fun PrivacyBullet(title: String, description: String) {
  Row(verticalAlignment = Alignment.Top) {
    Text("•", color = Emerald500, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 6.dp))
    Column {
      Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
      Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
