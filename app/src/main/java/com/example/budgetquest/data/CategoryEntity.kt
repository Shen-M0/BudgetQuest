package com.example.budgetquest.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_table")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val iconKey: String, // 儲存圖示的代號，例如 "FACE", "CART"
    val colorHex: String, // 儲存顏色的 Hex，例如 "#FFAB91"
    val isVisible: Boolean = true, // 用於隱藏/顯示
    val isDefault: Boolean = false, // 是否為系統預設 (建議預設不可刪除，只能隱藏)
    // [新增] 資源鍵值，如果是 null 代表使用者自訂分類
    val resourceKey: String? = null
)

@Entity(tableName = "tag_table")
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val isVisible: Boolean = true,
    // [新增]
    val resourceKey: String? = null
)