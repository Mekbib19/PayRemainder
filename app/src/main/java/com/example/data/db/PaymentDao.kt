package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Payment
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY status ASC, due_date ASC")
    fun getAllPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE status = :status ORDER BY due_date ASC")
    fun getPaymentsByStatus(status: String): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE id = :id")
    fun getPaymentById(id: Long): Flow<Payment?>

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getPaymentByIdOnce(id: Long): Payment?

    @Query("SELECT * FROM payments WHERE status != 'PAID'")
    suspend fun getActivePaymentsOnce(): List<Payment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Update
    suspend fun updatePayment(payment: Payment)

    @Delete
    suspend fun deletePayment(payment: Payment)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePaymentById(id: Long)

    @Query("DELETE FROM payments")
    suspend fun clearAll()
}
