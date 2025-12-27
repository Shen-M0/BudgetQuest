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
    val frequency: String,
    val customDays: Int = 0,
    val lastGeneratedDate: Long,
    // [新增] 結束日期 (Nullable，如果為 null 代表無限期)
    val endDate: Long? = null
)