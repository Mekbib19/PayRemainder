package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.PayReminderDatabase
import com.example.data.model.Payment
import com.example.data.model.PaymentConstants
import com.example.data.receiver.PaymentReminderScheduler
import com.example.data.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class TypeFilter { ALL, PERSONAL, BUSINESS }
enum class StatusFilter { ALL_ACTIVE, DUE_SOON, OVERDUE, ALL }

data class PaymentSummary(
    val totalPendingMinor: Long = 0,
    val countPending: Int = 0,
    val totalOverdueMinor: Long = 0,
    val countOverdue: Int = 0,
    val totalUpcomingMinor: Long = 0,
    val countUpcoming: Int = 0,
    val totalCustomerOwedMinor: Long = 0,
    val countCustomerOwed: Int = 0,
    val totalPaidMinor: Long = 0,
    val countPaid: Int = 0
)

data class PaymentUiState(
    val allPayments: List<Payment> = emptyList(),
    val activePayments: List<Payment> = emptyList(),
    val paidPayments: List<Payment> = emptyList(),
    val summary: PaymentSummary = PaymentSummary(),
    val selectedTypeFilter: TypeFilter = TypeFilter.ALL,
    val selectedStatusFilter: StatusFilter = StatusFilter.ALL_ACTIVE,
    val searchQuery: String = "",
    val filteredPayments: List<Payment> = emptyList(),
    val upcomingUrgentPayments: List<Payment> = emptyList(),
    val recentActivities: List<Payment> = emptyList(),
    val isLoading: Boolean = false,
    val userMessage: String? = null
)

class PaymentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PaymentRepository

    private val _typeFilter = MutableStateFlow(TypeFilter.ALL)
    private val _statusFilter = MutableStateFlow(StatusFilter.ALL_ACTIVE)
    private val _searchQuery = MutableStateFlow("")
    private val _userMessage = MutableStateFlow<String?>(null)

    init {
        val db = PayReminderDatabase.getDatabase(application)
        repository = PaymentRepository(db.paymentDao(), application)
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    val uiState: StateFlow<PaymentUiState> = combine(
        repository.allPayments,
        _typeFilter,
        _statusFilter,
        _searchQuery,
        _userMessage
    ) { payments, typeFilter, statusFilter, query, message ->
        val now = System.currentTimeMillis()
        val sevenDaysMs = TimeUnit.DAYS.toMillis(7)

        val active = payments.filter { it.status != PaymentConstants.STATUS_PAID }
        val paid = payments.filter { it.status == PaymentConstants.STATUS_PAID }

        var pendingMinor = 0L
        var countPending = 0
        var overdueMinor = 0L
        var countOverdue = 0
        var upcomingMinor = 0L
        var countUpcoming = 0
        var customerOwedMinor = 0L
        var countCustomerOwed = 0
        var paidMinor = 0L

        for (p in payments) {
            if (p.isPaid) {
                paidMinor += p.amountMinor
            } else {
                if (p.isBusiness) {
                    customerOwedMinor += p.amountMinor
                    countCustomerOwed++
                }

                if (p.dueDate < now) {
                    overdueMinor += p.amountMinor
                    countOverdue++
                } else {
                    pendingMinor += p.amountMinor
                    countPending++

                    if (p.dueDate <= now + sevenDaysMs) {
                        upcomingMinor += p.amountMinor
                        countUpcoming++
                    }
                }
            }
        }

        val summary = PaymentSummary(
            totalPendingMinor = pendingMinor,
            countPending = countPending,
            totalOverdueMinor = overdueMinor,
            countOverdue = countOverdue,
            totalUpcomingMinor = upcomingMinor,
            countUpcoming = countUpcoming,
            totalCustomerOwedMinor = customerOwedMinor,
            countCustomerOwed = countCustomerOwed,
            totalPaidMinor = paidMinor,
            countPaid = paid.size
        )

        // Filter active payments for the Payments tab
        val filtered = active.filter { payment ->
            val matchesType = when (typeFilter) {
                TypeFilter.ALL -> true
                TypeFilter.PERSONAL -> payment.isPersonal
                TypeFilter.BUSINESS -> payment.isBusiness
            }

            val matchesStatus = when (statusFilter) {
                StatusFilter.ALL_ACTIVE -> true
                StatusFilter.OVERDUE -> payment.dueDate < now
                StatusFilter.DUE_SOON -> payment.dueDate in now..(now + sevenDaysMs)
                StatusFilter.ALL -> true
            }

            val matchesSearch = query.isBlank() ||
                    payment.title.contains(query, ignoreCase = true) ||
                    (payment.personName?.contains(query, ignoreCase = true) == true) ||
                    payment.category.contains(query, ignoreCase = true)

            matchesType && matchesStatus && matchesSearch
        }

        val urgent = active.filter { it.dueDate <= now + sevenDaysMs }.sortedBy { it.dueDate }
        val recent = payments.sortedByDescending { it.updatedAt }.take(6)

        PaymentUiState(
            allPayments = payments,
            activePayments = active,
            paidPayments = paid,
            summary = summary,
            selectedTypeFilter = typeFilter,
            selectedStatusFilter = statusFilter,
            searchQuery = query,
            filteredPayments = filtered,
            upcomingUrgentPayments = urgent,
            recentActivities = recent,
            userMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PaymentUiState(isLoading = true)
    )

    fun setTypeFilter(filter: TypeFilter) {
        _typeFilter.value = filter
    }

    fun setStatusFilter(filter: StatusFilter) {
        _statusFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun addPayment(
        title: String,
        paymentType: String,
        amountMajor: Double,
        category: String,
        personName: String?,
        dueDate: Long,
        recurrence: String,
        reminderSchedule: String,
        notes: String?
    ) {
        viewModelScope.launch {
            val amountMinor = (amountMajor * 100).toLong()
            val now = System.currentTimeMillis()
            val initialStatus = if (dueDate < now) PaymentConstants.STATUS_OVERDUE else PaymentConstants.STATUS_PENDING

            val payment = Payment(
                title = title.trim(),
                paymentType = paymentType,
                amountMinor = amountMinor,
                currency = PaymentConstants.DEFAULT_CURRENCY,
                category = category,
                personName = personName?.trim()?.ifBlank { null },
                dueDate = dueDate,
                status = initialStatus,
                recurrence = recurrence,
                reminderSchedule = reminderSchedule,
                notes = notes?.trim()?.ifBlank { null }
            )
            repository.addPayment(payment)
            _userMessage.value = "Payment '${payment.title}' saved successfully!"
        }
    }

    fun updatePayment(payment: Payment) {
        viewModelScope.launch {
            repository.updatePayment(payment)
            _userMessage.value = "Updated '${payment.title}'"
        }
    }

    fun markAsPaid(payment: Payment) {
        viewModelScope.launch {
            repository.markPaymentPaid(payment.id)
            val recurrenceNote = if (payment.recurrence != PaymentConstants.RECURRENCE_NONE) {
                " Marked as paid. Next ${payment.recurrence.lowercase()} occurrence created!"
            } else {
                " Marked as paid."
            }
            _userMessage.value = "'${payment.title}'$recurrenceNote"
        }
    }

    fun markAsUnpaid(payment: Payment) {
        viewModelScope.launch {
            repository.markPaymentUnpaid(payment.id)
            _userMessage.value = "Restored '${payment.title}' to active status."
        }
    }

    fun deletePayment(payment: Payment) {
        viewModelScope.launch {
            repository.deletePayment(payment)
            _userMessage.value = "Deleted '${payment.title}'"
        }
    }

    fun triggerTestNotification() {
        PaymentReminderScheduler.triggerTestNotification(getApplication())
        _userMessage.value = "Test notification triggered! Check your notifications."
    }

    fun resetSampleData() {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
            _userMessage.value = "Sample payments refreshed."
        }
    }
}
