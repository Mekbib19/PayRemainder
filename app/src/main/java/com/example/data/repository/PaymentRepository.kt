package com.example.data.repository

import android.content.Context
import com.example.data.db.PaymentDao
import com.example.data.model.Payment
import com.example.data.model.PaymentConstants
import com.example.data.receiver.PaymentReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class PaymentRepository(
    private val paymentDao: PaymentDao,
    private val context: Context
) {
    val allPayments: Flow<List<Payment>> = paymentDao.getAllPayments().map { list ->
        val now = System.currentTimeMillis()
        list.map { payment ->
            if (payment.status != PaymentConstants.STATUS_PAID && payment.dueDate < now) {
                payment.copy(status = PaymentConstants.STATUS_OVERDUE)
            } else {
                payment
            }
        }
    }

    fun getPaymentById(id: Long): Flow<Payment?> = paymentDao.getPaymentById(id)

    suspend fun addPayment(payment: Payment): Long {
        val id = paymentDao.insertPayment(payment)
        val inserted = payment.copy(id = id)
        PaymentReminderScheduler.scheduleRemindersForPayment(context, inserted)
        return id
    }

    suspend fun updatePayment(payment: Payment) {
        paymentDao.updatePayment(payment)
        PaymentReminderScheduler.cancelRemindersForPayment(context, payment.id)
        if (payment.status != PaymentConstants.STATUS_PAID) {
            PaymentReminderScheduler.scheduleRemindersForPayment(context, payment)
        }
    }

    suspend fun deletePayment(payment: Payment) {
        PaymentReminderScheduler.cancelRemindersForPayment(context, payment.id)
        paymentDao.deletePayment(payment)
    }

    suspend fun deletePaymentById(id: Long) {
        PaymentReminderScheduler.cancelRemindersForPayment(context, id)
        paymentDao.deletePaymentById(id)
    }

    suspend fun markPaymentPaid(paymentId: Long) {
        val payment = paymentDao.getPaymentByIdOnce(paymentId) ?: return
        if (payment.status == PaymentConstants.STATUS_PAID) return

        // 1. Mark current payment as PAID
        val updatedCurrent = payment.copy(
            status = PaymentConstants.STATUS_PAID,
            paidAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        paymentDao.updatePayment(updatedCurrent)
        PaymentReminderScheduler.cancelRemindersForPayment(context, paymentId)

        // 2. Handle recurring payments: spawn next occurrence cleanly
        if (payment.recurrence != PaymentConstants.RECURRENCE_NONE) {
            val nextDueDate = calculateNextOccurrenceDate(payment.dueDate, payment.recurrence)
            val nextPayment = payment.copy(
                id = 0, // Generate new ID
                dueDate = nextDueDate,
                status = PaymentConstants.STATUS_PENDING,
                paidAt = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val newId = paymentDao.insertPayment(nextPayment)
            PaymentReminderScheduler.scheduleRemindersForPayment(context, nextPayment.copy(id = newId))
        }
    }

    suspend fun markPaymentUnpaid(paymentId: Long) {
        val payment = paymentDao.getPaymentByIdOnce(paymentId) ?: return
        val now = System.currentTimeMillis()
        val restoredStatus = if (payment.dueDate < now) PaymentConstants.STATUS_OVERDUE else PaymentConstants.STATUS_PENDING
        val updated = payment.copy(
            status = restoredStatus,
            paidAt = null,
            updatedAt = now
        )
        paymentDao.updatePayment(updated)
        PaymentReminderScheduler.scheduleRemindersForPayment(context, updated)
    }

    private fun calculateNextOccurrenceDate(currentDueDate: Long, recurrence: String): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentDueDate
        }
        when (recurrence) {
            PaymentConstants.RECURRENCE_WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            PaymentConstants.RECURRENCE_MONTHLY -> cal.add(Calendar.MONTH, 1)
            PaymentConstants.RECURRENCE_YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }

    suspend fun seedSampleDataIfEmpty() {
        val current = paymentDao.getActivePaymentsOnce()
        if (current.isNotEmpty()) return

        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L

        val samples = listOf(
            Payment(
                title = "Apartment Rent (Addis)",
                paymentType = PaymentConstants.TYPE_PERSONAL,
                amountMinor = 1400000, // 14,000.00 ETB
                currency = "ETB",
                category = "House Rent",
                personName = "Ato Girma (Landlord)",
                dueDate = now + (3 * oneDay), // In 3 days
                status = PaymentConstants.STATUS_PENDING,
                recurrence = PaymentConstants.RECURRENCE_MONTHLY,
                reminderSchedule = "7_DAYS,3_DAYS,1_DAY,ON_DUE",
                notes = "Transfer via CBE Birr or Telebirr before 5 PM"
            ),
            Payment(
                title = "Ethio Telecom Fiber Internet",
                paymentType = PaymentConstants.TYPE_PERSONAL,
                amountMinor = 165000, // 1,650.00 ETB
                currency = "ETB",
                category = "Internet",
                personName = "Ethio Telecom",
                dueDate = now + (1 * oneDay), // Tomorrow
                status = PaymentConstants.STATUS_PENDING,
                recurrence = PaymentConstants.RECURRENCE_MONTHLY,
                reminderSchedule = "3_DAYS,1_DAY,ON_DUE",
                notes = "Account #1029384756"
            ),
            Payment(
                title = "Abebe Tech Solution Invoice #204",
                paymentType = PaymentConstants.TYPE_BUSINESS,
                amountMinor = 2850000, // 28,500.00 ETB
                currency = "ETB",
                category = "Customer Invoice",
                personName = "Abebe Kebede",
                dueDate = now + (5 * oneDay), // In 5 days
                status = PaymentConstants.STATUS_PENDING,
                recurrence = PaymentConstants.RECURRENCE_NONE,
                reminderSchedule = "7_DAYS,3_DAYS,1_DAY,ON_DUE,AFTER_DUE",
                notes = "Web app development milestone 2 delivery"
            ),
            Payment(
                title = "Electricity Pre-paid Card",
                paymentType = PaymentConstants.TYPE_PERSONAL,
                amountMinor = 50000, // 500.00 ETB
                currency = "ETB",
                category = "Electricity",
                personName = "Addis Ababa EEU",
                dueDate = now - (2 * oneDay), // 2 days overdue
                status = PaymentConstants.STATUS_OVERDUE,
                recurrence = PaymentConstants.RECURRENCE_MONTHLY,
                reminderSchedule = "1_DAY,ON_DUE,AFTER_DUE",
                notes = "Meter number: 8839201"
            ),
            Payment(
                title = "Almaz Boutique Wholesale Supply",
                paymentType = PaymentConstants.TYPE_BUSINESS,
                amountMinor = 920000, // 9,200.00 ETB
                currency = "ETB",
                category = "Wholesale Credit",
                personName = "Woizero Almaz",
                dueDate = now - (1 * oneDay), // 1 day overdue
                status = PaymentConstants.STATUS_OVERDUE,
                recurrence = PaymentConstants.RECURRENCE_NONE,
                reminderSchedule = "1_DAY,ON_DUE,AFTER_DUE",
                notes = "Bole Medhanialem branch order"
            ),
            Payment(
                title = "Monthly Equb Contribution",
                paymentType = PaymentConstants.TYPE_PERSONAL,
                amountMinor = 500000, // 5,000.00 ETB
                currency = "ETB",
                category = "Equb / Idir",
                personName = "Equb Chairman Dawit",
                dueDate = now - (10 * oneDay),
                status = PaymentConstants.STATUS_PAID,
                recurrence = PaymentConstants.RECURRENCE_MONTHLY,
                reminderSchedule = "3_DAYS,1_DAY,ON_DUE",
                paidAt = now - (10 * oneDay),
                notes = "Turn #4 winner paid in full"
            )
        )

        for (sample in samples) {
            val id = paymentDao.insertPayment(sample)
            if (sample.status != PaymentConstants.STATUS_PAID) {
                PaymentReminderScheduler.scheduleRemindersForPayment(context, sample.copy(id = id))
            }
        }
    }
}
