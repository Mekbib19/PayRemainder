package com.example.data.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.db.PayReminderDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PaymentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule active reminders after reboot
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = PayReminderDatabase.getDatabase(context)
                    val activePayments = db.paymentDao().getActivePaymentsOnce()
                    for (payment in activePayments) {
                        PaymentReminderScheduler.scheduleRemindersForPayment(context, payment)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        if (action == PaymentReminderScheduler.ACTION_REMINDER) {
            val paymentId = intent.getLongExtra(PaymentReminderScheduler.EXTRA_PAYMENT_ID, -1L)
            val title = intent.getStringExtra(PaymentReminderScheduler.EXTRA_PAYMENT_TITLE) ?: "Payment Due"
            val amount = intent.getStringExtra(PaymentReminderScheduler.EXTRA_PAYMENT_AMOUNT) ?: ""
            val reminderLabel = intent.getStringExtra(PaymentReminderScheduler.EXTRA_REMINDER_LABEL) ?: "Reminder"
            val paymentType = intent.getStringExtra(PaymentReminderScheduler.EXTRA_PAYMENT_TYPE) ?: "PERSONAL"

            PaymentReminderScheduler.createNotificationChannel(context)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingOpenIntent = PendingIntent.getActivity(
                context,
                paymentId.toInt(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val contentText = if (paymentType == "BUSINESS") {
                "$reminderLabel: Customer invoice '$title' ($amount)"
            } else {
                "$reminderLabel: '$title' ($amount)"
            }

            val notification = NotificationCompat.Builder(context, PaymentReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("PayReminder: $title")
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$contentText. Open PayReminder to review details or mark as paid."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingOpenIntent)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = if (paymentId > 0) paymentId.toInt() else System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, notification)
        }
    }
}
