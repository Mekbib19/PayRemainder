package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Payment
import com.example.ui.components.PaymentCard
import com.example.ui.viewmodel.PaymentUiState
import com.example.ui.viewmodel.StatusFilter
import com.example.ui.viewmodel.TypeFilter
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsListScreen(
    uiState: PaymentUiState,
    onSearchChange: (String) -> Unit,
    onTypeFilterChange: (TypeFilter) -> Unit,
    onStatusFilterChange: (StatusFilter) -> Unit,
    onPaymentClick: (Payment) -> Unit,
    onMarkPaid: (Payment) -> Unit,
    onAddPaymentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("payments_list_screen")
    ) {
        // Search Header
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search by title, customer, or category...") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("payment_search_bar"),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        // Type Filter Tabs: All, Personal, Business
        TabRow(
            selectedTabIndex = when (uiState.selectedTypeFilter) {
                TypeFilter.ALL -> 0
                TypeFilter.PERSONAL -> 1
                TypeFilter.BUSINESS -> 2
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = uiState.selectedTypeFilter == TypeFilter.ALL,
                onClick = { onTypeFilterChange(TypeFilter.ALL) },
                text = { Text("All Active") }
            )
            Tab(
                selected = uiState.selectedTypeFilter == TypeFilter.PERSONAL,
                onClick = { onTypeFilterChange(TypeFilter.PERSONAL) },
                text = { Text("Personal") }
            )
            Tab(
                selected = uiState.selectedTypeFilter == TypeFilter.BUSINESS,
                onClick = { onTypeFilterChange(TypeFilter.BUSINESS) },
                text = { Text("Business") }
            )
        }

        // Sub Filter Chips (All Active, Due Soon, Overdue)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.selectedStatusFilter == StatusFilter.ALL_ACTIVE,
                onClick = { onStatusFilterChange(StatusFilter.ALL_ACTIVE) },
                label = { Text("All Active") }
            )
            FilterChip(
                selected = uiState.selectedStatusFilter == StatusFilter.DUE_SOON,
                onClick = { onStatusFilterChange(StatusFilter.DUE_SOON) },
                label = { Text("Due Soon (7d)") }
            )
            FilterChip(
                selected = uiState.selectedStatusFilter == StatusFilter.OVERDUE,
                onClick = { onStatusFilterChange(StatusFilter.OVERDUE) },
                label = { Text("Overdue") }
            )
        }

        // Total sum for current filtered list
        val currentSumMinor = uiState.filteredPayments.sumOf { it.amountMinor }
        val currentMajor = currentSumMinor / 100.0
        val formattedSum = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(currentMajor)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${uiState.filteredPayments.size} ${if (uiState.filteredPayments.size == 1) "record" else "records"}",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Text(
                text = "Total: $formattedSum ETB",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }

        // Payments LazyColumn
        if (uiState.filteredPayments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) "No matching payments" else "No active payments here",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) "Try searching with a different name, invoice, or category." else "All scheduled payments for this category are settled or none added yet.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAddPaymentClick,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Payment")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.filteredPayments, key = { it.id }) { payment ->
                    PaymentCard(
                        payment = payment,
                        onCardClick = { onPaymentClick(payment) },
                        onMarkPaid = { onMarkPaid(payment) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
}
