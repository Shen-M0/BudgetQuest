package com.example.budgetquest.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_expense_table")
data class RecurringExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Int,
    val note: String,
    val category: String,
    val startDate: Long,
    val frequency: String, // "MONTH", "WEEK", "DAY", "CUSTOM"
    val customDays: Int = 0,
    val lastGeneratedDate: Long = 0,
    val endDate: Long? = null,
    val planId: Int = -1,
    // [修正] 補上這個欄位，用於顯示「每月幾號」扣款
    val dayOfMonth: Int = 1
)