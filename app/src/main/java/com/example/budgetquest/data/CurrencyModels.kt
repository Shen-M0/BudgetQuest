package com.example.budgetquest.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

// 這是 ExchangeRate-API 回傳的標準 JSON 格式
data class ExchangeRateResponse(
    @SerializedName("result") val result: String,
    @SerializedName("time_last_update_unix") val lastUpdateUnix: Long,
    @SerializedName("base_code") val baseCode: String,
    @SerializedName("conversion_rates") val rates: Map<String, Double>
)


// [新增] 這是要存入 Room 資料庫的結構
@Entity(tableName = "currency_rates")
data class CurrencyRateEntity(
    @PrimaryKey val code: String, // 幣別代碼，如 "USD", "JPY"
    val rate: Double,             // 匯率
    val baseCurrency: String,     // 基準幣別，如 "TWD"
    val lastUpdated: Long         // 上次更新時間 (Unix Timestamp)
)
