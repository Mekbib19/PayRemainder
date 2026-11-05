package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Payment
import com.example.ui.components.AddEditPaymentSheet
import com.example.ui.components.PaymentDetailDialog
import com.example.ui.viewmodel.PaymentViewModel

enum class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    PAYMENTS("Payments", Icons.Filled.Payments, Icons.Outlined.Payments),
    HISTORY("History", Icons.Filled.History, Icons.Outlined.History),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: PaymentViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }

    var showAddSheet by remember { mutableStateOf(false) }
    var editingPayment by remember { mutableStateOf<Payment?>(null) }
    var selectedDetailPayment by remember { mutableStateOf<Payment?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // Handle Android back button
    BackHandler(enabled = currentTab != NavigationTab.HOME || showAddSheet || editingPayment != null || selectedDetailPayment != null) {
        when {
            selectedDetailPayment != null -> selectedDetailPayment = null
            editingPayment != null -> editingPayment = null
            showAddSheet -> showAddSheet = false
            currentTab != NavigationTab.HOME -> currentTab = NavigationTab.HOME
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentTab) {
                            NavigationTab.HOME -> "PayReminder"
                            NavigationTab.PAYMENTS -> "Active Payments"
                            NavigationTab.HISTORY -> "Payment History"
                            NavigationTab.SETTINGS -> "Settings"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                NavigationTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab != NavigationTab.SETTINGS) {
                FloatingActionButton(
                    onClick = {
                        editingPayment = null
                        showAddSheet = true
                    },
                    modifier = Modifier.testTag("fab_add_payment"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Payment",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavigationTab.HOME -> {
                    DashboardScreen(
                        uiState = uiState,
                        onAddPaymentClick = {
                            editingPayment = null
                            showAddSheet = true
                        },
                        onViewHistoryClick = { currentTab = NavigationTab.HISTORY },
                        onViewAllPaymentsClick = { currentTab = NavigationTab.PAYMENTS },
                        onPaymentClick = { payment -> selectedDetailPayment = payment },
                        onMarkPaid = { payment -> viewModel.markAsPaid(payment) }
                    )
                }

                NavigationTab.PAYMENTS -> {
                    PaymentsListScreen(
                        uiState = uiState,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onTypeFilterChange = { viewModel.setTypeFilter(it) },
                        onStatusFilterChange = { viewModel.setStatusFilter(it) },
                        onPaymentClick = { payment -> selectedDetailPayment = payment },
                        onMarkPaid = { payment -> viewModel.markAsPaid(payment) },
                        onAddPaymentClick = {
                            editingPayment = null
                            showAddSheet = true
                        }
                    )
                }

                NavigationTab.HISTORY -> {
                    HistoryScreen(
                        uiState = uiState,
                        onPaymentClick = { payment -> selectedDetailPayment = payment }
                    )
                }

                NavigationTab.SETTINGS -> {
                    SettingsScreen(
                        uiState = uiState,
                        onTestNotification = { viewModel.triggerTestNotification() },
                        onResetData = { viewModel.resetSampleData() }
                    )
                }
            }
        }
    }

    // Add or Edit Payment Modal Bottom Sheet
    if (showAddSheet || editingPayment != null) {
        AddEditPaymentSheet(
            onDismiss = {
                showAddSheet = false
                editingPayment = null
            },
            onSave = { title, paymentType, amountMajor, category, personName, dueDate, recurrence, reminderSchedule, notes ->
                if (editingPayment != null) {
                    val amountMinor = (amountMajor * 100).toLong()
                    viewModel.updatePayment(
                        editingPayment!!.copy(
                            title = title,
                            paymentType = paymentType,
                            amountMinor = amountMinor,
                            category = category,
                            personName = personName,
                            dueDate = dueDate,
                            recurrence = recurrence,
                            reminderSchedule = reminderSchedule,
                            notes = notes,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } else {
                    viewModel.addPayment(
                        title = title,
                        paymentType = paymentType,
                        amountMajor = amountMajor,
                        category = category,
                        personName = personName,
                        dueDate = dueDate,
                        recurrence = recurrence,
                        reminderSchedule = reminderSchedule,
                        notes = notes
                    )
                }
                showAddSheet = false
                editingPayment = null
            },
            editingPayment = editingPayment
        )
    }

    // Payment Detail Dialog
    selectedDetailPayment?.let { payment ->
        PaymentDetailDialog(
            payment = payment,
            onDismiss = { selectedDetailPayment = null },
            onMarkPaid = {
                viewModel.markAsPaid(payment)
                selectedDetailPayment = null
            },
            onMarkUnpaid = {
                viewModel.markAsUnpaid(payment)
                selectedDetailPayment = null
            },
            onEdit = {
                selectedDetailPayment = null
                editingPayment = payment
            },
            onDelete = {
                viewModel.deletePayment(payment)
                selectedDetailPayment = null
            }
        )
    }
}
