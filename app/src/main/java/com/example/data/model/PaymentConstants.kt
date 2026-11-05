package com.example.data.model

object PaymentConstants {
    const val TYPE_PERSONAL = "PERSONAL"
    const val TYPE_BUSINESS = "BUSINESS"

    const val STATUS_PENDING = "PENDING"
    const val STATUS_PAID = "PAID"
    const val STATUS_OVERDUE = "OVERDUE"

    const val RECURRENCE_NONE = "NONE"
    const val RECURRENCE_WEEKLY = "WEEKLY"
    const val RECURRENCE_MONTHLY = "MONTHLY"
    const val RECURRENCE_YEARLY = "YEARLY"

    val PERSONAL_CATEGORIES = listOf(
        "House Rent",
        "Electricity",
        "Water",
        "Internet",
        "School Fees",
        "Loans",
        "Subscriptions",
        "Equb / Idir",
        "Groceries / Household",
        "Other Expenses"
    )

    val BUSINESS_CATEGORIES = listOf(
        "Customer Invoice",
        "Client Services",
        "Product Sale",
        "Consulting Fee",
        "Wholesale Credit",
        "Retail Customer Debt",
        "Maintenance & Repair",
        "Other Business"
    )

    val REMINDER_OPTIONS = listOf(
        "7_DAYS" to "7 days before",
        "3_DAYS" to "3 days before",
        "1_DAY" to "1 day before",
        "ON_DUE" to "On due date",
        "AFTER_DUE" to "After due date (if unpaid)"
    )

    const val DEFAULT_CURRENCY = "ETB"
}
