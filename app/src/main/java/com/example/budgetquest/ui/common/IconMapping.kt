package com.example.budgetquest.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

// 讓使用者選擇的圖示庫
enum class CategoryIcon(val key: String, val icon: ImageVector) {
    FACE("FACE", Icons.Default.Face),
    CART("CART", Icons.Default.ShoppingCart),
    HOME("HOME", Icons.Default.Home),
    STAR("STAR", Icons.Default.Star),
    PET("PET", Icons.Default.Favorite), // 寵物
    WORK("WORK", Icons.Default.Email), // 工作
    CAR("CAR", Icons.Default.Call), // 改用 Call 暫代，或找適合的
    PHONE("PHONE", Icons.Default.Phone),
    CAKE("CAKE", Icons.Default.CheckCircle) // 甜點
}

fun getIconByKey(key: String): ImageVector {
    return CategoryIcon.values().find { it.key == key }?.icon ?: Icons.Default.Star
}