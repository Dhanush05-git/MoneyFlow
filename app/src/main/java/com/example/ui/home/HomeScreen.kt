package com.example.ui.home

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CaptureSource
import com.example.model.TransactionEntity
import com.example.model.TransactionType
import com.example.ui.ExpenseUiState
import com.example.ui.components.ReviewAttentionCard
import com.example.ui.components.TransactionListItem
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.TealAccent
import java.util.Calendar

@Composable
fun HomeScreen(
  uiState: ExpenseUiState,
  onNavigateToReview: () -> Unit,
  onNavigateToHealth: () -> Unit,
  onNavigateToParserLab: () -> Unit,
  onOpenAddExpense: () -> Unit,
  onSelectTransaction: (TransactionEntity) -> Unit,
  onConfirmReview: (Long) -> Unit,
  onIgnoreReview: (Long) -> Unit,
  modifier: Modifier = Modifier
) {
  val (todaySpentPaise, monthSpentPaise, autoCapturedCount, categorySums) = remember(uiState.allTransactions) {
    val cal = Calendar.getInstance()
    val todayYear = cal.get(Calendar.YEAR)
    val todayDayOfYear = cal.get(Calendar.DAY_OF_YEAR)
    val thisMonth = cal.get(Calendar.MONTH)

    var todayPaise = 0L
    var monthPaise = 0L
    var autoCount = 0
    val catMap = mutableMapOf<String, Long>()

    val txCal = Calendar.getInstance()
    for (tx in uiState.allTransactions) {
      if (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.CARD_PAYMENT) {
        txCal.timeInMillis = tx.occurredAt
        if (txCal.get(Calendar.YEAR) == todayYear && txCal.get(Calendar.MONTH) == thisMonth) {
          monthPaise += tx.amountPaise
          catMap[tx.categoryName] = (catMap[tx.categoryName] ?: 0L) + tx.amountPaise
        }
        if (txCal.get(Calendar.YEAR) == todayYear && txCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear) {
          todayPaise += tx.amountPaise
        }
      }
      if (tx.source == CaptureSource.SMS || tx.source == CaptureSource.NOTIFICATION) {
        autoCount++
      }
    }
    listOf(todayPaise, monthPaise, autoCount, catMap)
  }

  val greeting = remember {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    when {
      hour < 12 -> "Good morning"
      hour < 17 -> "Good afternoon"
      else -> "Good evening"
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(10.dp))
      // Top header greeting + quick action pills
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = greeting,
            style = MaterialTheme.typography.headlineMedium.copy(
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "Automated Personal Expense Ledger",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          // Parser Lab shortcut
          IconButton(
            onClick = onNavigateToParserLab,
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .testTag("home_parser_lab_btn")
          ) {
            Icon(
              Icons.Default.Science,
              contentDescription = "Parser Lab",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          }

          // Capture Health shortcut
          IconButton(
            onClick = onNavigateToHealth,
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .testTag("home_capture_health_btn")
          ) {
            Icon(
              Icons.Default.HealthAndSafety,
              contentDescription = "Capture Health",
              tint = Emerald500,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }

    // Hero Spending Card
    item {
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("hero_spend_card")
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.linearGradient(
                colors = listOf(
                  Emerald700.copy(alpha = 0.25f),
                  MaterialTheme.colorScheme.surface
                )
              )
            )
            .padding(20.dp)
        ) {
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top
            ) {
              Column {
                Text(
                  text = "SPENT TODAY",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                  ),
                  color = Emerald500
                )
                Spacer(modifier = Modifier.height(4.dp))
                val todayRs = (todaySpentPaise as Long) / 100
                Text(
                  text = "₹$todayRs",
                  style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp
                  ),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "SPENT THIS MONTH",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                  ),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                val monthRs = (monthSpentPaise as Long) / 100
                Text(
                  text = "₹$monthRs",
                  style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                  ),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Trend comparison pill
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = RoundedCornerShape(20.dp),
                color = Emerald500.copy(alpha = 0.15f)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = Emerald500,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "${uiState.allTransactions.size} total recorded",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Emerald500
                  )
                }
              }

              Spacer(modifier = Modifier.width(8.dp))

              Text(
                text = if (uiState.allTransactions.isEmpty()) "Awaiting first transaction" else "Active tracking",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category breakdown quick pills
            @Suppress("UNCHECKED_CAST")
            val catMap = categorySums as Map<String, Long>
            val topCats = catMap.entries.sortedByDescending { it.value }.take(4)
            if (topCats.isNotEmpty()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                for (entry in topCats) {
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                  ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                      Text(
                        text = entry.key.split(" ").first(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                      )
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                        text = "₹${entry.value / 100}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                      )
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Automation summary banner
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Default.Bolt,
                  contentDescription = null,
                  tint = Emerald500,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "$autoCapturedCount automatic captures",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              if (uiState.reviewCount > 0) {
                Text(
                  text = "${uiState.reviewCount} need review →",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.clickable { onNavigateToReview() }
                )
              } else {
                Text(
                  text = "Review queue clear",
                  style = MaterialTheme.typography.labelSmall,
                  color = Emerald500
                )
              }
            }
          }
        }
      }
    }

    // Review Prompt Card (if any pending)
    if (uiState.reviewQueue.isNotEmpty()) {
      item {
        val topReviewItem = uiState.reviewQueue.first()
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Needs Review (${uiState.reviewCount})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
              )
            }

            Text(
              text = "View All",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.clickable { onNavigateToReview() }
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          ReviewAttentionCard(
            transaction = topReviewItem,
            onConfirm = { onConfirmReview(topReviewItem.id) },
            onIgnore = { onIgnoreReview(topReviewItem.id) },
            onEdit = { onSelectTransaction(topReviewItem) }
          )
        }
      }
    }

    // Recent Transactions Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Recent Transactions",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )

        Button(
          onClick = onOpenAddExpense,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("home_add_expense_btn")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
      }
    }

    // Transaction list (top 10) or Empty State
    if (uiState.allTransactions.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              Icons.Default.HealthAndSafety,
              contentDescription = null,
              tint = Emerald500,
              modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "No Transactions Recorded Yet",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Expense Ledger automatically records incoming bank SMS and UPI payment notifications. You can also backfill your past 60 days of banking SMS instantly.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Button(
                onClick = onNavigateToHealth,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
              ) {
                Text("Scan Past SMS", fontSize = 13.sp)
              }
              OutlinedButton(
                onClick = onOpenAddExpense,
                shape = RoundedCornerShape(12.dp)
              ) {
                Text("Add Manually", fontSize = 13.sp)
              }
            }
          }
        }
      }
    } else {
      items(uiState.allTransactions.take(10), key = { it.id }) { tx ->
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
