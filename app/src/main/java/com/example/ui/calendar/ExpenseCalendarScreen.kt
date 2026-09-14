package com.example.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TransactionEntity
import com.example.model.TransactionType
import com.example.ui.ExpenseUiState
import com.example.ui.components.TransactionListItem
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralRose
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.TealAccent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ExpenseCalendarScreen(
  uiState: ExpenseUiState,
  onSelectTransaction: (TransactionEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  var displayedCal by remember {
    val cal = Calendar.getInstance()
    // Defaults to current month (e.g. September 2026)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    mutableStateOf(cal)
  }

  var selectedDayNumber by remember {
    val cal = Calendar.getInstance()
    mutableStateOf(cal.get(Calendar.DAY_OF_MONTH))
  }

  val monthYearTitle = remember(displayedCal) {
    val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    sdf.format(displayedCal.time)
  }

  // Pre-calculate daily sums for the displayed month
  val (dailySums, monthlyTotalPaise, maxDay, minDay, averageDayPaise) = remember(uiState.allTransactions, displayedCal) {
    val year = displayedCal.get(Calendar.YEAR)
    val month = displayedCal.get(Calendar.MONTH)
    val daysInMonth = displayedCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val sums = LongArray(daysInMonth + 1)
    val cal = Calendar.getInstance()

    for (tx in uiState.allTransactions) {
      if (tx.type == TransactionType.EXPENSE || tx.type == TransactionType.CARD_PAYMENT) {
        cal.timeInMillis = tx.occurredAt
        if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month) {
          val day = cal.get(Calendar.DAY_OF_MONTH)
          if (day in 1..daysInMonth) {
            sums[day] += tx.amountPaise
          }
        }
      }
    }

    var total = 0L
    var maxSpend = 0L
    var maxD = 1
    var minSpend = Long.MAX_VALUE
    var minD = 1
    var activeDays = 0

    for (d in 1..daysInMonth) {
      val s = sums[d]
      total += s
      if (s > maxSpend) {
        maxSpend = s
        maxD = d
      }
      if (s in 1 until minSpend) {
        minSpend = s
        minD = d
      }
      if (s > 0) activeDays++
    }

    val avg = if (activeDays > 0) total / activeDays else 0L
    val minDayResult = if (minSpend == Long.MAX_VALUE) Pair(1, 0L) else Pair(minD, minSpend)

    listOf(sums, total, Pair(maxD, maxSpend), minDayResult, avg)
  }

  // Filter transactions for the selected day
  val selectedDayTransactions = remember(uiState.allTransactions, displayedCal, selectedDayNumber) {
    val year = displayedCal.get(Calendar.YEAR)
    val month = displayedCal.get(Calendar.MONTH)
    val cal = Calendar.getInstance()

    uiState.allTransactions.filter { tx ->
      cal.timeInMillis = tx.occurredAt
      cal.get(Calendar.YEAR) == year &&
      cal.get(Calendar.MONTH) == month &&
      cal.get(Calendar.DAY_OF_MONTH) == selectedDayNumber
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
      // Title
      Text(
        text = "Expense Calendar",
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground
      )
      Text(
        text = "P&L style daily spending intensity tracker",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    // Trader stats row
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(
                text = "MONTH TOTAL",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "₹${(monthlyTotalPaise as Long) / 100}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "DAILY AVERAGE",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "₹${(averageDayPaise as Long) / 100}/day",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Emerald500
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          @Suppress("UNCHECKED_CAST")
          val maxPair = maxDay as Pair<Int, Long>
          @Suppress("UNCHECKED_CAST")
          val minPair = minDay as Pair<Int, Long>

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = CoralRose.copy(alpha = 0.12f),
              modifier = Modifier.weight(1f)
            ) {
              Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = CoralRose, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                  Text("Highest Day", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = CoralRose)
                  Text("Day ${maxPair.first}: ₹${maxPair.second / 100}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = CoralRose)
                }
              }
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Emerald500.copy(alpha = 0.12f),
              modifier = Modifier.weight(1f)
            ) {
              Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Emerald500, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                  Text("Spending Streak", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Emerald500)
                  Text("3 days < ₹500", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Emerald500)
                }
              }
            }
          }
        }
      }
    }

    // Calendar Card
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          // Month navigation header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = {
                val newCal = Calendar.getInstance().apply {
                  time = displayedCal.time
                  add(Calendar.MONTH, -1)
                }
                displayedCal = newCal
              }
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
            }

            Text(
              text = monthYearTitle,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(
              onClick = {
                val newCal = Calendar.getInstance().apply {
                  time = displayedCal.time
                  add(Calendar.MONTH, 1)
                }
                displayedCal = newCal
              }
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Weekdays header
          val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
          Row(modifier = Modifier.fillMaxWidth()) {
            for (w in weekdays) {
              Text(
                text = w,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Month Days Grid
          val firstDayOfWeek = remember(displayedCal) {
            val c = Calendar.getInstance().apply {
              time = displayedCal.time
              set(Calendar.DAY_OF_MONTH, 1)
            }
            // In Java Calendar, Sunday is 1, Monday is 2
            val d = c.get(Calendar.DAY_OF_WEEK)
            if (d == Calendar.SUNDAY) 6 else d - 2 // 0 for Mon ... 6 for Sun
          }

          val totalDays = displayedCal.getActualMaximum(Calendar.DAY_OF_MONTH)
          val totalCells = ((firstDayOfWeek + totalDays + 6) / 7) * 7

          val sumsArray = dailySums as LongArray

          for (row in 0 until (totalCells / 7)) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
            ) {
              for (col in 0..6) {
                val cellIndex = row * 7 + col
                val dayNum = cellIndex - firstDayOfWeek + 1

                if (dayNum in 1..totalDays) {
                  val spendPaise = sumsArray[dayNum]
                  val isSelected = dayNum == selectedDayNumber

                  DayCell(
                    dayNum = dayNum,
                    spendPaise = spendPaise,
                    isSelected = isSelected,
                    onClick = { selectedDayNumber = dayNum },
                    modifier = Modifier.weight(1f)
                  )
                } else {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }
        }
      }
    }

    // Selected Day Section
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        val totalPaise = (dailySums as LongArray).getOrElse(selectedDayNumber) { 0L }
        Text(
          text = "Day $selectedDayNumber Breakdown",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onBackground
        )

        Text(
          text = "Total: ₹${totalPaise / 100}",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
          color = Emerald500
        )
      }
    }

    if (selectedDayTransactions.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No expenses recorded on Day $selectedDayNumber",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    } else {
      items(selectedDayTransactions, key = { it.id }) { tx ->
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

@Composable
fun DayCell(
  dayNum: Int,
  spendPaise: Long,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val spendRupees = spendPaise / 100

  // Spending heat-map background colors
  val bgColor = when {
    spendRupees == 0L -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    spendRupees < 500 -> Emerald500.copy(alpha = 0.20f)
    spendRupees in 500..1500 -> AmberWarning.copy(alpha = 0.25f)
    else -> CoralRose.copy(alpha = 0.25f)
  }

  val textColor = when {
    spendRupees == 0L -> MaterialTheme.colorScheme.onSurfaceVariant
    spendRupees < 500 -> Emerald500
    spendRupees in 500..1500 -> AmberWarning
    else -> CoralRose
  }

  val borderModifier = if (isSelected) {
    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
  } else {
    Modifier
  }

  Box(
    modifier = modifier
      .padding(2.dp)
      .aspectRatio(0.85f)
      .clip(RoundedCornerShape(10.dp))
      .background(bgColor)
      .then(borderModifier)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = dayNum.toString(),
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
          fontSize = 11.sp
        ),
        color = MaterialTheme.colorScheme.onSurface
      )

      if (spendRupees > 0) {
        val label = if (spendRupees >= 1000) {
          String.format("%.1fk", spendRupees / 1000.0)
        } else {
          "₹$spendRupees"
        }
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp
          ),
          color = textColor,
          maxLines = 1
        )
      } else {
        Text(
          text = "-",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
          color = Slate400.copy(alpha = 0.5f)
        )
      }
    }
  }
}
