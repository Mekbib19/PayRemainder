package com.example.data.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.Payment
import java.util.Calendar

object PaymentReminderScheduler {
    const val CHANNEL_ID = "payreminder_reminders_channel"
    const val CHANNEL_NAME = "Payment Reminders"
    const val ACTION_REMINDER = "com.example.payreminder.ACTION_PAYMENT_REMINDER"

    const val EXTRA_PAYMENT_ID = "extra_payment_id"
    const val EXTRA_PAYMENT_TITLE = "extra_payment_title"
    const val EXTRA_PAYMENT_AMOUNT = "extra_payment_amount"
    const val EXTRA_PAYMENT_TYPE = "extra_payment_type"
    const val EXTRA_REMINDER_LABEL = "extra_reminder_label"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies about pending, upcoming, and overdue payments in PayReminder"
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleRemindersForPayment(context: Context, payment: Payment) {
        if (payment.status == "PAID") {
            cancelRemindersForPayment(context, payment.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val scheduleTokens = payment.reminderSchedule.split(",").map { it.trim() }
        val now = System.currentTimeMillis()

        for (token in scheduleTokens) {
            val triggerTime = computeReminderTime(payment.dueDate, token)
            if (triggerTime != null && triggerTime > now) {
                val requestCode = generateRequestCode(payment.id, token)
                val intent = Intent(context, PaymentReminderReceiver::class.java).apply {
                    action = ACTION_REMINDER
                    putExtra(EXTRA_PAYMENT_ID, payment.id)
                    putExtra(EXTRA_PAYMENT_TITLE, payment.title)
                    putExtra(EXTRA_PAYMENT_AMOUNT, payment.formattedAmount())
                    putExtra(EXTRA_PAYMENT_TYPE, payment.paymentType)
                    putExtra(EXTRA_REMINDER_LABEL, getLabelForToken(token))
                }

                val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    }
                    Log.d("PayReminder", "Scheduled reminder for ${payment.title} at $triggerTime ($token)")
                } catch (e: SecurityException) {
                    Log.e("PayReminder", "Cannot schedule exact alarm: ${e.message}")
                }
            }
        }
    }

    fun cancelRemindersForPayment(context: Context, paymentId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val tokens = listOf("7_DAYS", "3_DAYS", "1_DAY", "ON_DUE", "AFTER_DUE")
        for (token in tokens) {
            val requestCode = generateRequestCode(paymentId, token)
            val intent = Intent(context, PaymentReminderReceiver::class.java).apply {
                action = ACTION_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    fun triggerTestNotification(context: Context) {
        createNotificationChannel(context)
        val intent = Intent(context, PaymentReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_PAYMENT_ID, -1L)
            putExtra(EXTRA_PAYMENT_TITLE, "Demo: Monthly Electricity Bill")
            putExtra(EXTRA_PAYMENT_AMOUNT, "450.00 ETB")
            putExtra(EXTRA_PAYMENT_TYPE, "PERSONAL")
            putExtra(EXTRA_REMINDER_LABEL, "Test Reminder (Due Tomorrow)")
        }
        context.sendBroadcast(intent)
    }

    private fun computeReminderTime(dueDate: Long, token: String): Long? {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dueDate
            // Align reminder to 9:00 AM
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return when (token) {
            "7_DAYS" -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.timeInMillis
            }
            "3_DAYS" -> {
                cal.add(Calendar.DAY_OF_YEAR, -3)
                cal.timeInMillis
            }
            "1_DAY" -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.timeInMillis
            }
            "ON_DUE" -> cal.timeInMillis
            "AFTER_DUE" -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            else -> null
        }
    }

    private fun getLabelForToken(token: String): String {
        return when (token) {
            "7_DAYS" -> "7 days remaining"
            "3_DAYS" -> "3 days remaining"
            "1_DAY" -> "Due tomorrow"
            "ON_DUE" -> "Due today!"
            "AFTER_DUE" -> "Overdue payment alert!"
            else -> "Payment Reminder"
        }
    }

    private fun generateRequestCode(paymentId: Long, token: String): Int {
        val tokenHash = token.hashCode()
        return ((paymentId * 31) + tokenHash).toInt()
    }
}
