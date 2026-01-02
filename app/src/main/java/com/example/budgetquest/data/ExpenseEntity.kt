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

    // [新增] 支付方式 (預設現金)
    val paymentMethod: String = "Cash",

    // [新增] 店家/地點
    val merchant: String? = null,

    // [新增] 不計入預算 (預設 false = 都要計入)
    val excludeFromBudget: Boolean = false,

    // [新增] 消費性質 (預設 true = 需要 Need, false = 想要 Want)
    val isNeed: Boolean = true
)