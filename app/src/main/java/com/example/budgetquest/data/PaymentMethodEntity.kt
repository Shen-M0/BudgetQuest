package com.example.budgetquest.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_method_table")
data class PaymentMethodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val isVisible: Boolean = true,
    val order: Int = 0 // 用於排序
)