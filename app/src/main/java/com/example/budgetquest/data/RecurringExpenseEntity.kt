package com.example.budgetquest.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// [修正] 改回 recurring_expense_table 以配合 DAO 的查詢
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
    val planId: Int = -1
)