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
    val planId: Int = -1, // 關聯的計畫 ID (-1 代表不屬於特定計畫或是全域)
    val date: Long,           // 消費日期
    val amount: Int,          // 金額
    val note: String,         // 備註
    val category: String,

    // [新增] 圖片路徑 (URI 字串)
    val imageUri: String? = null,

    // [修正] 預設為空字串，代表未指定
    val paymentMethod: String = "",

    // [修正] 改為 Boolean? (可為空)，null 代表未指定
    val isNeed: Boolean? = null,
    val excludeFromBudget: Boolean = false,
    val merchant: String = "",
    val recurringRuleId: Long? = null

)