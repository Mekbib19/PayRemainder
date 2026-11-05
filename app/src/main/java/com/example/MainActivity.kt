package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.receiver.PaymentReminderScheduler
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PaymentViewModel

class MainActivity : ComponentActivity() {
    private val paymentViewModel: PaymentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create the notification channel on launch
        PaymentReminderScheduler.createNotificationChannel(applicationContext)

        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = paymentViewModel)
            }
        }
    }
}
