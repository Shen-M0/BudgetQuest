package com.example.budgetquest.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

// [優化] 加入 indices，加速根據 date 的查詢速度
@Entity(
    tableName = "expense_table",
    indices = [Index(value = ["date"])]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long,           // 消費日期
    val amount: Int,          // 金額
    val note: String,         // 備註
    val category: String
)