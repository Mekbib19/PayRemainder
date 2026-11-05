package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Payment
import com.example.data.model.PaymentConstants
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditPaymentSheet(
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        paymentType: String,
        amountMajor: Double,
        category: String,
        personName: String?,
        dueDate: Long,
        recurrence: String,
        reminderSchedule: String,
        notes: String?
    ) -> Unit,
    editingPayment: Payment? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(editingPayment?.title ?: "") }
    var paymentType by remember {
        mutableStateOf(editingPayment?.paymentType ?: PaymentConstants.TYPE_PERSONAL)
    }
    var amountText by remember {
        mutableStateOf(editingPayment?.let { (it.amountMinor / 100.0).toString() } ?: "")
    }

    val availableCategories = if (paymentType == PaymentConstants.TYPE_BUSINESS) {
        PaymentConstants.BUSINESS_CATEGORIES
    } else {
        PaymentConstants.PERSONAL_CATEGORIES
    }

    var selectedCategory by remember {
        mutableStateOf(editingPayment?.category ?: availableCategories.first())
    }
    var personName by remember { mutableStateOf(editingPayment?.personName ?: "") }
    var dueDate by remember {
        mutableStateOf(editingPayment?.dueDate ?: (System.currentTimeMillis() + 86400000L * 3))
    }
    var recurrence by remember {
        mutableStateOf(editingPayment?.recurrence ?: PaymentConstants.RECURRENCE_NONE)
    }
    var notes by remember { mutableStateOf(editingPayment?.notes ?: "") }

    val initialReminders = editingPayment?.reminderSchedule?.split(",")?.map { it.trim() }
        ?: listOf("1_DAY", "ON_DUE")
    val selectedReminders = remember { mutableStateListOf<String>().apply { addAll(initialReminders) } }

    var showDatePicker by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingPayment == null) "New Payment Record" else "Edit Payment",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_sheet_btn")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Type Selector (Personal vs Business)
            Text(
                text = "Payment Type",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = paymentType == PaymentConstants.TYPE_PERSONAL,
                    onClick = {
                        paymentType = PaymentConstants.TYPE_PERSONAL
                        selectedCategory = PaymentConstants.PERSONAL_CATEGORIES.first()
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                ) {
                    Text("Personal (Payable)")
                }
                SegmentedButton(
                    selected = paymentType == PaymentConstants.TYPE_BUSINESS,
                    onClick = {
                        paymentType = PaymentConstants.TYPE_BUSINESS
                        selectedCategory = PaymentConstants.BUSINESS_CATEGORIES.first()
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {
                        Icon(imageVector = Icons.Default.Business, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                ) {
                    Text("Business (Receivable)")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (titleError && it.isNotBlank()) titleError = false
                },
                label = { Text(if (paymentType == PaymentConstants.TYPE_BUSINESS) "Invoice / Payment Title *" else "Payment Title *") },
                placeholder = {
                    Text(if (paymentType == PaymentConstants.TYPE_BUSINESS) "e.g., Almaz Boutique Wholesale #104" else "e.g., House Rent, Wifi, Electricity")
                },
                isError = titleError,
                supportingText = if (titleError) { { Text("Please enter a payment title") } } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payment_title_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Amount and Currency Field
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        if (amountError && it.toDoubleOrNull() != null && it.toDouble() > 0) amountError = false
                    },
                    label = { Text("Amount *") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError,
                    supportingText = if (amountError) { { Text("Enter a valid amount") } } else null,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("payment_amount_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = "ETB",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Currency") },
                    modifier = Modifier.width(90.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Customer / Person Name
            OutlinedTextField(
                value = personName,
                onValueChange = { personName = it },
                label = { Text(if (paymentType == PaymentConstants.TYPE_BUSINESS) "Customer / Client Name *" else "Person / Payee (Optional)") },
                placeholder = {
                    Text(if (paymentType == PaymentConstants.TYPE_BUSINESS) "e.g., Ato Bekele, Woizero Tigist" else "e.g., Ato Girma (Landlord)")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payment_person_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Chips
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableCategories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Due Date Picker Button
            Text(
                text = "Due Date",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            val sdf = remember { SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { showDatePicker = true }
                    .padding(16.dp)
                    .testTag("due_date_picker_trigger")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = sdf.format(Date(dueDate)),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = "Tap to change due date",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Select Date",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recurrence
            Text(
                text = "Recurrence",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val recurrences = listOf(
                    PaymentConstants.RECURRENCE_NONE to "None",
                    PaymentConstants.RECURRENCE_WEEKLY to "Weekly",
                    PaymentConstants.RECURRENCE_MONTHLY to "Monthly",
                    PaymentConstants.RECURRENCE_YEARLY to "Yearly"
                )
                recurrences.forEach { (recKey, recLabel) ->
                    FilterChip(
                        selected = recurrence == recKey,
                        onClick = { recurrence = recKey },
                        label = { Text(recLabel, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reminder Schedule Checkboxes
            Text(
                text = "Reminder Schedule (Push Notifications)",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column {
                PaymentConstants.REMINDER_OPTIONS.forEach { (token, label) ->
                    val isChecked = selectedReminders.contains(token)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) {
                                    if (selectedReminders.size > 1) selectedReminders.remove(token)
                                } else {
                                    selectedReminders.add(token)
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (!selectedReminders.contains(token)) selectedReminders.add(token)
                                } else {
                                    if (selectedReminders.size > 1) selectedReminders.remove(token)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                placeholder = { Text("Account / invoice #, CBE Birr / Telebirr confirmation, phone number...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payment_notes_input"),
                minLines = 2,
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    val isTitleValid = title.isNotBlank()
                    val amountVal = amountText.toDoubleOrNull()
                    val isAmountValid = amountVal != null && amountVal > 0

                    if (!isTitleValid) titleError = true
                    if (!isAmountValid) amountError = true

                    if (isTitleValid && isAmountValid) {
                        onSave(
                            title,
                            paymentType,
                            amountVal!!,
                            selectedCategory,
                            personName.ifBlank { null },
                            dueDate,
                            recurrence,
                            selectedReminders.joinToString(","),
                            notes.ifBlank { null }
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_payment_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (editingPayment == null) "Save Payment" else "Update Payment",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        dueDate = it
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
