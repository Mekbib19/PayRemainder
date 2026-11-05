package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "payment_type")
    val paymentType: String, // "PERSONAL" or "BUSINESS"

    @ColumnInfo(name = "amount_minor")
    val amountMinor: Long, // Amount in minor currency unit (e.g., Ethiopian cents/santim: 100 = 1.00 ETB)

    @ColumnInfo(name = "currency")
    val currency: String = "ETB",

    @ColumnInfo(name = "category")
    val category: String, // e.g. "House rent", "Electricity", "Water", "Customer Invoice"

    @ColumnInfo(name = "person_name")
    val personName: String? = null, // Customer name (business) or payee/recipient (personal)

    @ColumnInfo(name = "due_date")
    val dueDate: Long, // Epoch timestamp in milliseconds

    @ColumnInfo(name = "status")
    val status: String = "PENDING", // "PENDING", "PAID", "OVERDUE"

    @ColumnInfo(name = "recurrence")
    val recurrence: String = "NONE", // "NONE", "WEEKLY", "MONTHLY", "YEARLY"

    @ColumnInfo(name = "reminder_schedule")
    val reminderSchedule: String = "1_DAY,ON_DUE", // Comma-separated: 7_DAYS,3_DAYS,1_DAY,ON_DUE,AFTER_DUE

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "paid_at")
    val paidAt: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isPaid: Boolean
        get() = status == "PAID"

    val isBusiness: Boolean
        get() = paymentType.equals("BUSINESS", ignoreCase = true)

    val isPersonal: Boolean
        get() = !isBusiness

    fun effectiveStatus(currentTime: Long = System.currentTimeMillis()): String {
        if (status == "PAID") return "PAID"
        return if (dueDate < currentTime) "OVERDUE" else "PENDING"
    }

    fun formattedAmount(): String {
        val majorUnits = amountMinor / 100.0
        val format = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "${format.format(majorUnits)} $currency"
    }

    fun formattedDueDate(): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(dueDate))
    }

    fun formattedPaidDate(): String? {
        if (paidAt == null) return null
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(paidAt))
    }

    fun daysDifference(currentTime: Long = System.currentTimeMillis()): Long {
        val diff = dueDate - currentTime
        return TimeUnit.MILLISECONDS.toDays(diff)
    }

    fun dueStatusLabel(currentTime: Long = System.currentTimeMillis()): String {
        if (isPaid) return "Paid"
        val diffDays = daysDifference(currentTime)
        return when {
            diffDays < 0 -> "Overdue by ${-diffDays} day${if (-diffDays > 1) "s" else ""}"
            diffDays == 0L -> "Due Today"
            diffDays == 1L -> "Due Tomorrow"
            else -> "Due in $diffDays days"
        }
    }
}
