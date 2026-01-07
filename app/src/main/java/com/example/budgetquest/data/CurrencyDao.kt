package com.example.budgetquest.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CurrencyDao {
    // 取得所有匯率
    @Query("SELECT * FROM currency_rates")
    suspend fun getAllRates(): List<CurrencyRateEntity>

    // 取得特定幣別的匯率
    @Query("SELECT * FROM currency_rates WHERE code = :code")
    suspend fun getRate(code: String): CurrencyRateEntity?

    // 取得最後更新時間 (隨便抓一筆資料的時間即可，因為整批是一起更新的)
    @Query("SELECT lastUpdated FROM currency_rates LIMIT 1")
    suspend fun getLastUpdateTime(): Long?

    // 插入或更新匯率 (如果幣別已存在則覆蓋)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rates: List<CurrencyRateEntity>)

    // 清空舊匯率 (切換基準幣別時可能需要)
    @Query("DELETE FROM currency_rates")
    suspend fun clearAll()
}