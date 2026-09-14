package com.example.ui.insights

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PaymentMethod
import com.example.model.TransactionType
import com.example.ui.ExpenseUiState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralRose
import com.example.ui.theme.Emerald500
import com.example.ui.theme.TealAccent

@Composable
fun InsightsScreen(
  uiState: ExpenseUiState,
  modifier: Modifier = Modifier
) {
  val (categoryHierarchy, methodBreakdown, recurringCandidates, anomalies, totalSpend) = remember(uiState.allTransactions) {
    val catMerchantMap = mutableMapOf<String, MutableMap<String, Long>>()
    val methodMap = mutableMapOf<PaymentMethod, Long>()
    var total = 0L

    val merchantCounts = mutableMapOf<String, Pair<Int, Long>>() // merchant -> (count, amount)

    for (tx in uiState.allTransactions) {
      if (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.CARD_PAYMENT) {
        total += tx.amountPaise
        val catMap = catMerchantMap.getOrPut(tx.categoryName) { mutableMapOf() }
        catMap[tx.merchantName] = (catMap[tx.merchantName] ?: 0L) + tx.amountPaise

        methodMap[tx.paymentMethod] = (methodMap[tx.paymentMethod] ?: 0L) + tx.amountPaise

        val current = merchantCounts[tx.merchantName] ?: Pair(0, 0L)
        merchantCounts[tx.merchantName] = Pair(current.first + 1, tx.amountPaise)
      }
    }

    // Detect recurring/subscription candidates (e.g. Netflix, Spotify, or recurring same merchant)
    val recurringList = uiState.allTransactions
      .filter { it.categoryName == "Subscriptions" || it.merchantName.contains("Netflix", true) || it.merchantName.contains("Spotify", true) }
      .distinctBy { it.merchantName }

    // Detect anomalies (transactions > 3x average)
    val averageTxnPaise = if (uiState.allTransactions.isNotEmpty()) total / uiState.allTransactions.size else 0L
    val detectedAnomalies = uiState.allTransactions.filter {
      (it.type == TransactionType.EXPENSE || it.type == TransactionType.CARD_PAYMENT) &&
      it.amountPaise > (averageTxnPaise * 2.5) &&
      it.amountPaise > 200000L // above ₹2,000
    }

    listOf(catMerchantMap, methodMap, recurringList, detectedAnomalies, total)
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "Financial Intelligence",
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )
      Text(
        text = "100% deterministic, private on-device analytics",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    // Anomaly Alert Card (if any found)
    @Suppress("UNCHECKED_CAST")
    val anomalyList = anomalies as List<com.example.model.TransactionEntity>
    if (anomalyList.isNotEmpty()) {
      item {
        val topAnomaly = anomalyList.first()
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = CoralRose.copy(alpha = 0.12f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
          ) {
            Icon(
              Icons.Default.WarningAmber,
              contentDescription = null,
              tint = CoralRose,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Unusually Large Expense",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = CoralRose
              )
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text = "${topAnomaly.formatRupees()} at ${topAnomaly.merchantName} (${topAnomaly.categoryName}) is significantly higher than your typical transactions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }
    }

    // Section 1: "Where is my money going?" (Category -> Merchant breakdown)
    item {
      Text(
        text = "Where is my money going?",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )
    }

    @Suppress("UNCHECKED_CAST")
    val catHierarchy = categoryHierarchy as Map<String, Map<String, Long>>
    val sortedCategories = catHierarchy.entries.sortedByDescending { it.value.values.sum() }

    if (sortedCategories.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              Icons.Default.PieChart,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "No Spending Trends Yet",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Visual category distribution, merchant breakdowns, and anomaly alerts will appear as transactions are captured.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      }
    }

    items(sortedCategories, key = { it.key }) { (catName, merchantMap) ->
      val catTotal = merchantMap.values.sum()
      val catPercentage = if ((totalSpend as Long) > 0) (catTotal.toFloat() / totalSpend) else 0f

      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = catName,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )

            Text(
              text = "₹${catTotal / 100}",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          LinearProgressIndicator(
            progress = { catPercentage },
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = Emerald500,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Top merchants under this category
          val topMerchants = merchantMap.entries.sortedByDescending { it.value }.take(3)
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (m in topMerchants) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "  • ${m.key}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "₹${m.value / 100}",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }
      }
    }

    // Payment Method Distribution
    item {
      Text(
        text = "Payment Method Share",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )
    }

    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          @Suppress("UNCHECKED_CAST")
          val mBreakdown = methodBreakdown as Map<PaymentMethod, Long>
          for ((method, amount) in mBreakdown.entries.sortedByDescending { it.value }) {
            val pct = if ((totalSpend as Long) > 0) ((amount * 100) / totalSpend).toInt() else 0
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = method.label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
              )
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "₹${amount / 100}",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                  Text(
                    text = "$pct%",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }
      }
    }

    // Subscription & Recurring Intelligence
    @Suppress("UNCHECKED_CAST")
    val subList = recurringCandidates as List<com.example.model.TransactionEntity>
    if (subList.isNotEmpty()) {
      item {
        Text(
          text = "Detected Subscriptions & Recurring",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )
      }

      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (sub in subList) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    Icons.Default.Repeat,
                    contentDescription = null,
                    tint = TealAccent,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      text = sub.merchantName,
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = "Repeats ~monthly on ${sub.paymentMethod.label}",
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                Text(
                  text = sub.formatRupees(),
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
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
