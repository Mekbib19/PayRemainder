package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {
    fun getCategoryIcon(category: String): ImageVector {
        val lower = category.lowercase()
        return when {
            lower.contains("rent") || lower.contains("house") -> Icons.Default.Home
            lower.contains("electric") || lower.contains("power") || lower.contains("light") -> Icons.Default.Bolt
            lower.contains("water") -> Icons.Default.WaterDrop
            lower.contains("internet") || lower.contains("wifi") || lower.contains("telecom") -> Icons.Default.Wifi
            lower.contains("school") || lower.contains("fee") || lower.contains("education") -> Icons.Default.School
            lower.contains("loan") || lower.contains("equb") || lower.contains("idir") -> Icons.Default.AccountBalance
            lower.contains("invoice") || lower.contains("bill") -> Icons.Default.Receipt
            lower.contains("sale") || lower.contains("wholesale") || lower.contains("retail") -> Icons.Default.ShoppingBag
            else -> Icons.Default.Paid
        }
    }
}
