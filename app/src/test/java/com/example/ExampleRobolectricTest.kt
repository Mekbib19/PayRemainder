package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.PayReminderDatabase
import com.example.data.model.Payment
import com.example.data.model.PaymentConstants
import com.example.data.repository.PaymentRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: PayReminderDatabase
    private lateinit var repository: PaymentRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PayReminderDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PaymentRepository(database.paymentDao(), context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAppNameString() {
        val appName = context.getString(R.string.app_name)
        assertEquals("PayReminder", appName)
    }

    @Test
    fun testPaymentAmountMinorFormatting() {
        val payment = Payment(
            title = "Office Rent",
            paymentType = PaymentConstants.TYPE_PERSONAL,
            amountMinor = 1450000, // 14,500.00 ETB
            currency = "ETB",
            category = "House Rent",
            dueDate = System.currentTimeMillis() + 86400000L
        )

        assertEquals("14,500.00 ETB", payment.formattedAmount())
    }

    @Test
    fun testAddPaymentAndMarkAsPaidWithRecurrence() = runBlocking {
        val initialDueDate = System.currentTimeMillis() + 86400000L * 2
        val recurringPayment = Payment(
            title = "Monthly WiFi Bill",
            paymentType = PaymentConstants.TYPE_PERSONAL,
            amountMinor = 165000, // 1,650.00 ETB
            currency = "ETB",
            category = "Internet",
            dueDate = initialDueDate,
            recurrence = PaymentConstants.RECURRENCE_MONTHLY,
            status = PaymentConstants.STATUS_PENDING
        )

        val insertedId = repository.addPayment(recurringPayment)
        assertTrue(insertedId > 0)

        // Mark as paid
        repository.markPaymentPaid(insertedId)

        val all = repository.allPayments.first()
        // There should now be 2 payments: the paid occurrence and the next scheduled occurrence!
        assertEquals(2, all.size)

        val paidOccurrence = all.first { it.id == insertedId }
        assertTrue(paidOccurrence.isPaid)
        assertNotNull(paidOccurrence.paidAt)

        val nextOccurrence = all.first { it.id != insertedId }
        assertFalse(nextOccurrence.isPaid)
        assertEquals(PaymentConstants.STATUS_PENDING, nextOccurrence.status)
        assertTrue("Next occurrence should be scheduled in the future", nextOccurrence.dueDate > initialDueDate)
    }

    @Test
    fun testCustomerBusinessPaymentTracking() = runBlocking {
        val businessPayment = Payment(
            title = "Website Development Invoice",
            paymentType = PaymentConstants.TYPE_BUSINESS,
            amountMinor = 3500000, // 35,000.00 ETB
            currency = "ETB",
            category = "Customer Invoice",
            personName = "Dawit Alemayehu",
            dueDate = System.currentTimeMillis() + 86400000L * 5
        )

        val id = repository.addPayment(businessPayment)
        val loaded = database.paymentDao().getPaymentByIdOnce(id)
        assertNotNull(loaded)
        assertTrue(loaded!!.isBusiness)
        assertEquals("Dawit Alemayehu", loaded.personName)
        assertEquals("35,000.00 ETB", loaded.formattedAmount())
    }
}
