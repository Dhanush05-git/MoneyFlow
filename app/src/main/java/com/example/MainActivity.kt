package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.TransactionEntity
import com.example.ui.ExpenseViewModel
import com.example.ui.calendar.ExpenseCalendarScreen
import com.example.ui.detail.TransactionDetailDialog
import com.example.ui.health.CaptureHealthScreen
import com.example.ui.home.HomeScreen
import com.example.ui.insights.InsightsScreen
import com.example.ui.manual.AddEditExpenseDialog
import com.example.ui.parserlab.ParserLabScreen
import com.example.ui.review.ReviewQueueScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.transactions.TransactionsScreen

enum class NavDestination(val label: String, val icon: ImageVector, val tag: String) {
  HOME("Home", Icons.Default.Home, "nav_home"),
  TRANSACTIONS("Ledger", Icons.Default.ReceiptLong, "nav_transactions"),
  CALENDAR("Calendar", Icons.Default.CalendarMonth, "nav_calendar"),
  INSIGHTS("Insights", Icons.Default.Insights, "nav_insights"),
  SETTINGS("Settings", Icons.Default.Settings, "nav_settings")
}

enum class SubScreen {
  NONE,
  PARSER_LAB,
  CAPTURE_HEALTH,
  REVIEW_QUEUE
}

class MainActivity : ComponentActivity() {

  private val viewModel: ExpenseViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        var currentNav by remember { mutableStateOf(NavDestination.HOME) }
        var currentSubScreen by remember { mutableStateOf(SubScreen.NONE) }

        var showAddExpenseDialog by remember { mutableStateOf(false) }
        var transactionToDetail by remember { mutableStateOf<TransactionEntity?>(null) }
        var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          bottomBar = {
            if (currentSubScreen == SubScreen.NONE) {
              NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
              ) {
                for (dest in NavDestination.entries) {
                  val isSelected = currentNav == dest
                  NavigationBarItem(
                    selected = isSelected,
                    onClick = { currentNav = dest },
                    icon = {
                      if (dest == NavDestination.SETTINGS && uiState.reviewCount > 0) {
                        BadgedBox(
                          badge = {
                            Badge {
                              Text("${uiState.reviewCount}")
                            }
                          }
                        ) {
                          Icon(dest.icon, contentDescription = dest.label)
                        }
                      } else {
                        Icon(dest.icon, contentDescription = dest.label)
                      }
                    },
                    label = { Text(dest.label) },
                    modifier = Modifier.testTag(dest.tag)
                  )
                }
              }
            }
          },
          floatingActionButton = {
            if (currentSubScreen == SubScreen.NONE && (currentNav == NavDestination.HOME || currentNav == NavDestination.TRANSACTIONS)) {
              FloatingActionButton(
                onClick = { showAddExpenseDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("global_fab_add_expense")
              ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
              }
            }
          }
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          ) {
            when (currentSubScreen) {
              SubScreen.PARSER_LAB -> {
                ParserLabScreen(
                  viewModel = viewModel,
                  onBack = { currentSubScreen = SubScreen.NONE }
                )
              }
              SubScreen.CAPTURE_HEALTH -> {
                CaptureHealthScreen(
                  uiState = uiState,
                  viewModel = viewModel,
                  onBack = { currentSubScreen = SubScreen.NONE }
                )
              }
              SubScreen.REVIEW_QUEUE -> {
                ReviewQueueScreen(
                  uiState = uiState,
                  onConfirm = { viewModel.confirmReview(it) },
                  onIgnore = { viewModel.ignoreReview(it) },
                  onEdit = { transactionToEdit = it },
                  onBack = { currentSubScreen = SubScreen.NONE }
                )
              }
              SubScreen.NONE -> {
                Crossfade(targetState = currentNav, label = "nav_crossfade") { dest ->
                  when (dest) {
                    NavDestination.HOME -> {
                      HomeScreen(
                        uiState = uiState,
                        onNavigateToReview = { currentSubScreen = SubScreen.REVIEW_QUEUE },
                        onNavigateToHealth = { currentSubScreen = SubScreen.CAPTURE_HEALTH },
                        onNavigateToParserLab = { currentSubScreen = SubScreen.PARSER_LAB },
                        onOpenAddExpense = { showAddExpenseDialog = true },
                        onSelectTransaction = { transactionToDetail = it },
                        onConfirmReview = { viewModel.confirmReview(it) },
                        onIgnoreReview = { viewModel.ignoreReview(it) }
                      )
                    }
                    NavDestination.TRANSACTIONS -> {
                      TransactionsScreen(
                        uiState = uiState,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onTypeFilterChange = { viewModel.setTypeFilter(it) },
                        onSourceFilterChange = { viewModel.setSourceFilter(it) },
                        onSelectTransaction = { transactionToDetail = it }
                      )
                    }
                    NavDestination.CALENDAR -> {
                      ExpenseCalendarScreen(
                        uiState = uiState,
                        onSelectTransaction = { transactionToDetail = it }
                      )
                    }
                    NavDestination.INSIGHTS -> {
                      InsightsScreen(
                        uiState = uiState
                      )
                    }
                    NavDestination.SETTINGS -> {
                      SettingsScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onNavigateToParserLab = { currentSubScreen = SubScreen.PARSER_LAB },
                        onNavigateToHealth = { currentSubScreen = SubScreen.CAPTURE_HEALTH },
                        onNavigateToReview = { currentSubScreen = SubScreen.REVIEW_QUEUE }
                      )
                    }
                  }
                }
              }
            }
          }
        }

        if (showAddExpenseDialog) {
          AddEditExpenseDialog(
            initialTransaction = null,
            onDismiss = { showAddExpenseDialog = false },
            onSave = { amt, merchant, cat, acc, method, type, dateMillis, notes ->
              viewModel.addManualExpense(amt, merchant, cat, acc, method, type, dateMillis, notes)
            }
          )
        }

        transactionToEdit?.let { txToEdit ->
          AddEditExpenseDialog(
            initialTransaction = txToEdit,
            onDismiss = { transactionToEdit = null },
            onSave = { amt, merchant, cat, acc, method, type, dateMillis, notes ->
              viewModel.updateTransaction(
                txToEdit.copy(
                  amountPaise = (amt * 100).toLong(),
                  merchantName = merchant,
                  categoryName = cat,
                  accountName = acc,
                  paymentMethod = method,
                  type = type,
                  occurredAt = dateMillis,
                  notes = notes
                )
              )
              transactionToEdit = null
            },
            onDelete = {
              viewModel.deleteTransaction(it)
              transactionToEdit = null
            }
          )
        }

        transactionToDetail?.let { txDetail ->
          TransactionDetailDialog(
            transaction = txDetail,
            onDismiss = { transactionToDetail = null },
            onEdit = {
              val current = transactionToDetail
              transactionToDetail = null
              transactionToEdit = current
            },
            onDelete = {
              viewModel.deleteTransaction(txDetail)
              transactionToDetail = null
            }
          )
        }
      }
    }
  }
}
