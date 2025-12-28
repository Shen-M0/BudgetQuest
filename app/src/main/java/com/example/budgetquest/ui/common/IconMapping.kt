package com.example.budgetquest.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.TrendingUp // 若上面報錯，請用這個

// 讓使用者選擇的圖示庫

// 根據字串 Key 回傳對應的 Material Icon
@Composable
fun getIconByKey(key: String): ImageVector {
    return when (key) {
        "FOOD" -> Icons.Default.Restaurant
        "SHOPPING" -> Icons.Default.ShoppingCart
        "TRANSPORT" -> Icons.Default.DirectionsCar
        "HOME" -> Icons.Default.Home
        "ENTERTAINMENT" -> Icons.Default.Movie
        "MEDICAL" -> Icons.Default.LocalHospital
        "EDUCATION" -> Icons.Default.School
        "BILLS" -> Icons.Default.Receipt
        "INVESTMENT" -> Icons.Default.TrendingUp
        "TRAVEL" -> Icons.Default.Flight
        "STAR" -> Icons.Default.Star
        "OTHER" -> Icons.Default.MoreHoriz
        "FACE" -> Icons.Default.Face
        "PET"-> Icons.Default.Favorite
        "WORK"-> Icons.Default.Email
        "CAR"-> Icons.Default.Call
        "PHONE" -> Icons.Default.Phone
        "CAKE"-> Icons.Default.CheckCircle
        else -> Icons.Default.Star // 預設值
    }
}