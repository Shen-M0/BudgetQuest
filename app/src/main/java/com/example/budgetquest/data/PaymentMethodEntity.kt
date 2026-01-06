package com.example.budgetquest.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_method_table")
data class PaymentMethodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val isVisible: Boolean = true,
    val order: Int = 0,

    // [新增] 資源 Key，用於多語言反查
    // 預設為 null (使用者自訂的支付方式就沒有 key)
    val resourceKey: String? = null
)