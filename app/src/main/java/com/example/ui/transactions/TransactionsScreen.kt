package com.example.ui.transactions

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CaptureSource
import com.example.model.TransactionEntity
import com.example.model.TransactionType
import com.example.ui.ExpenseUiState
import com.example.ui.components.TransactionListItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionsScreen(
  uiState: ExpenseUiState,
  onSearchQueryChange: (String) -> Unit,
  onTypeFilterChange: (TransactionType?) -> Unit,
  onSourceFilterChange: (CaptureSource?) -> Unit,
  onSelectTransaction: (TransactionEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  // Group transactions by calendar day
  val groupedTransactions = remember(uiState.filteredTransactions) {
    val dayFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
    val todayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val todayStr = todayFormat.format(Date())

    uiState.filteredTransactions.groupBy { tx ->
      val txDate = Date(tx.occurredAt)
      val txStr = todayFormat.format(txDate)
      when (txStr) {
        todayStr -> "Today"
        else -> dayFormat.format(txDate)
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = "Transactions Ledger",
      style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Search bar with conditions support (e.g. "Amazon", "Food", "> 500")
    OutlinedTextField(
      value = uiState.searchQuery,
      onValueChange = onSearchQueryChange,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("tx_search_input"),
      placeholder = { Text("Search merchant, category, or '> 500'...") },
      leadingIcon = {
        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
      },
      trailingIcon = {
        if (uiState.searchQuery.isNotEmpty()) {
          IconButton(onClick = { onSearchQueryChange("") }) {
            Icon(Icons.Default.Clear, contentDescription = "Clear")
          }
        }
      },
      shape = RoundedCornerShape(14.dp),
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline
      )
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Filter chips row
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      item {
        FilterChip(
          selected = uiState.typeFilter == null && uiState.sourceFilter == null,
          onClick = {
            onTypeFilterChange(null)
            onSourceFilterChange(null)
          },
          label = { Text("All") },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
          )
        )
      }

      item {
        FilterChip(
          selected = uiState.typeFilter == TransactionType.EXPENSE,
          onClick = {
            onTypeFilterChange(if (uiState.typeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE)
          },
          label = { Text("Expenses") }
        )
      }

      item {
        FilterChip(
          selected = uiState.sourceFilter == CaptureSource.SMS,
          onClick = {
            onSourceFilterChange(if (uiState.sourceFilter == CaptureSource.SMS) null else CaptureSource.SMS)
          },
          label = { Text("SMS Captured") }
        )
      }

      item {
        FilterChip(
          selected = uiState.sourceFilter == CaptureSource.NOTIFICATION,
          onClick = {
            onSourceFilterChange(if (uiState.sourceFilter == CaptureSource.NOTIFICATION) null else CaptureSource.NOTIFICATION)
          },
          label = { Text("App Notifications") }
        )
      }

      item {
        FilterChip(
          selected = uiState.typeFilter == TransactionType.CARD_PAYMENT,
          onClick = {
            onTypeFilterChange(if (uiState.typeFilter == TransactionType.CARD_PAYMENT) null else TransactionType.CARD_PAYMENT)
          },
          label = { Text("Card Bills") }
        )
      }

      item {
        FilterChip(
          selected = uiState.typeFilter == TransactionType.INCOME,
          onClick = {
            onTypeFilterChange(if (uiState.typeFilter == TransactionType.INCOME) null else TransactionType.INCOME)
          },
          label = { Text("Income") }
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // List grouped by day
    if (uiState.filteredTransactions.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(24.dp)
        ) {
          Icon(
            Icons.Default.FilterList,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = if (uiState.allTransactions.isEmpty()) "Ledger is Empty" else "No matching transactions",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = if (uiState.allTransactions.isEmpty()) 
              "Transactions received or spent via bank SMS and UPI apps will automatically appear here." 
            else 
              "Try clearing active search or filter chips.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        groupedTransactions.forEach { (dateHeader, txsInDay) ->
          item(key = "header_$dateHeader") {
            val daySumPaise = txsInDay.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.CARD_PAYMENT }
              .sumOf { it.amountPaise }
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = dateHeader,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "Total: ₹${daySumPaise / 100}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          items(txsInDay, key = { it.id }) { tx ->
            TransactionListItem(
              transaction = tx,
              onClick = { onSelectTransaction(tx) }
            )
          }
        }

        item {
          Spacer(modifier = Modifier.height(80.dp))
        }
      }
    }
  }
}
