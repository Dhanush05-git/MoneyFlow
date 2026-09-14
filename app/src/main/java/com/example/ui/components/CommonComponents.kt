package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sms
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CaptureSource
import com.example.model.PaymentMethod
import com.example.model.TransactionEntity
import com.example.model.TransactionType
import com.example.ui.theme.CoralRose
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionListItem(
  transaction: TransactionEntity,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .testTag("tx_item_${transaction.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Category icon circle
      CategoryIconBox(category = transaction.categoryName)

      Spacer(modifier = Modifier.width(14.dp))

      // Title & Subtitle
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = transaction.merchantName,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
          ),
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(3.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "${transaction.categoryName} • ${transaction.paymentMethod.label}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Auto captured vs Manual indicator
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (transaction.source == CaptureSource.SMS || transaction.source == CaptureSource.NOTIFICATION) {
            Icon(
              imageVector = Icons.Default.Bolt,
              contentDescription = "Auto captured",
              tint = Emerald500,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "Auto captured (${transaction.source.label})",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
              color = Emerald500,
              fontWeight = FontWeight.Medium
            )
          } else {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Manual entry",
              tint = Slate400,
              modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "Manually added",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
              color = Slate400
            )
          }
        }
      }

      // Amount & Time
      Column(horizontalAlignment = Alignment.End) {
        val isNegative = transaction.type == TransactionType.EXPENSE || transaction.type == TransactionType.CARD_PAYMENT
        val amountColor = if (isNegative) MaterialTheme.colorScheme.onSurface else Emerald500
        val prefix = if (isNegative) "- " else "+ "

        Text(
          text = "$prefix${transaction.formatRupees()}",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          ),
          color = amountColor
        )

        Spacer(modifier = Modifier.height(3.dp))

        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        Text(
          text = timeFormatter.format(Date(transaction.occurredAt)),
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
fun CategoryIconBox(category: String, modifier: Modifier = Modifier) {
  val (color, icon) = getCategoryVisual(category)
  Box(
    modifier = modifier
      .size(44.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(color.copy(alpha = 0.15f)),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = category,
      tint = color,
      modifier = Modifier.size(22.dp)
    )
  }
}

fun getCategoryVisual(category: String): Pair<Color, ImageVector> {
  val clean = category.lowercase()
  return when {
    clean.contains("food") || clean.contains("dining") -> Pair(Color(0xFFF97316), Icons.Default.Restaurant)
    clean.contains("grocer") -> Pair(Color(0xFF10B981), Icons.Default.ShoppingBag)
    clean.contains("shop") -> Pair(Color(0xFFF59E0B), Icons.Default.ShoppingBag)
    clean.contains("trans") || clean.contains("fuel") -> Pair(Color(0xFF0EA5E9), Icons.Default.LocalAtm)
    clean.contains("bill") || clean.contains("util") -> Pair(CoralRose, Icons.Default.CreditCard)
    clean.contains("health") -> Pair(Color(0xFFEC4899), Icons.Default.LocalAtm)
    else -> Pair(Color(0xFF8B5CF6), Icons.Default.CreditCard)
  }
}

@Composable
fun ReviewAttentionCard(
  transaction: TransactionEntity,
  onConfirm: () -> Unit,
  onIgnore: () -> Unit,
  onEdit: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    modifier = modifier
      .fillMaxWidth()
      .testTag("review_card_${transaction.id}")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primaryContainer
        ) {
          Text(
            text = "${transaction.confidenceScore}% Confidence",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }

        Text(
          text = transaction.source.label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = transaction.merchantName,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Suggested: ${transaction.categoryName} • ${transaction.paymentMethod.label}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Text(
          text = transaction.formatRupees(),
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      if (!transaction.rawSnippet.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "\"${transaction.rawSnippet}\"",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
          color = Slate500,
          maxLines = 2
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onConfirm,
          colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
          modifier = Modifier
            .weight(1f)
            .testTag("confirm_review_btn_${transaction.id}")
        ) {
          Icon(Icons.Default.Check, contentDescription = "Confirm", modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Confirm", fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
          onClick = onEdit,
          modifier = Modifier.testTag("edit_review_btn_${transaction.id}")
        ) {
          Text("Edit")
        }

        OutlinedButton(
          onClick = onIgnore,
          modifier = Modifier.testTag("ignore_review_btn_${transaction.id}")
        ) {
          Text("Ignore")
        }
      }
    }
  }
}
