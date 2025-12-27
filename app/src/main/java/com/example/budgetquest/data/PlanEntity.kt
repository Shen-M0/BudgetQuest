package com.example.budgetquest.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plan_table")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val startDate: Long,
    val endDate: Long,
    val totalBudget: Int,
    val targetSavings: Int,
    val isActive: Boolean = true,

    // [必須確認] 這裡是否有預設值？
    // 如果你在舊資料庫上升級，但沒有給預設值，讀取舊資料時會崩潰
    @ColumnInfo(defaultValue = "未命名計畫") // 建議加上這個 Annotation 確保 Migration 安全
    val planName: String = "未命名計畫"
)