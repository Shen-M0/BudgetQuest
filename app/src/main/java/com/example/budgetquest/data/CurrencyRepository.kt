package com.example.budgetquest.data

import android.content.Context
import android.util.Log
import com.example.budgetquest.BuildConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// [修改] 在建構子加入 context: Context
class CurrencyRepository(
    private val context: Context,
    private val currencyDao: CurrencyDao,
    private val api: ExchangeRateApi
) {

    // ... isDataStale 函式保持不變 ...
    private fun isDataStale(lastUpdated: Long): Boolean {
        val oneDayInMillis = 24 * 60 * 60 * 1000
        return (System.currentTimeMillis() - lastUpdated) > oneDayInMillis
    }

    suspend fun getRates(baseCurrency: String, forceRefresh: Boolean = false) {
        withContext(Dispatchers.IO) {
            try {
                val lastUpdated = currencyDao.getLastUpdateTime() ?: 0L
                val hasData = lastUpdated > 0

                // 判斷邏輯：
                // 1. 強制更新
                // 2. 完全沒資料
                // 3. 資料過期
                if (forceRefresh || !hasData || isDataStale(lastUpdated)) {
                    Log.d("CurrencyRepo", "準備更新匯率 (base=$baseCurrency)...")
                    try {
                        // 嘗試從網路更新
                        fetchAndSaveRatesFromNetwork(baseCurrency)
                    } catch (e: Exception) {
                        Log.e("CurrencyRepo", "網路更新失敗: ${e.message}")

                        // [新增] 網路失敗後的 Fallback 機制
                        // 如果資料庫是空的 (代表第一次安裝且沒網路)，必須載入預設值
                        if (!hasData) {
                            Log.d("CurrencyRepo", "資料庫為空，載入本地預設匯率表...")
                            loadFromAssets(baseCurrency)
                        } else {
                            Log.d("CurrencyRepo", "使用舊有資料庫資料繼續運作。")
                        }
                    }
                } else {
                    Log.d("CurrencyRepo", "本地資料夠新，使用快取。")
                }
            } catch (e: Exception) {
                Log.e("CurrencyRepo", "Repository 發生未預期錯誤: ${e.message}")
            }
        }
    }

    // [重構] 將原本的 fetchAndSaveRates 改名並獨立出來
    private suspend fun fetchAndSaveRatesFromNetwork(base: String) {
        val apiKey = BuildConfig.EXCHANGE_RATE_API_KEY
        val response = api.getLatestRates(apiKey, base)

        if (response.result == "success") {
            saveRatesToDb(response, base)
            Log.d("CurrencyRepo", "網路匯率更新成功！")
        } else {
            throw Exception("API Error: ${response.result}")
        }
    }

    // [修改] loadFromAssets 函式
    private suspend fun loadFromAssets(base: String) {
        try {
            // 1. 讀取 JSON 檔案
            val jsonString = context.assets.open("default_rates.json")
                .bufferedReader()
                .use { it.readText() }

            // 2. [修改] 解析成 Map<String, ExchangeRateResponse>
            // 因為我們的 JSON 現在是 {"USD": {...}, "TWD": {...}} 的格式
            val type = object : TypeToken<Map<String, ExchangeRateResponse>>() {}.type
            val allRates: Map<String, ExchangeRateResponse> = Gson().fromJson(jsonString, type)

            // 3. [新增] 檢查是否有使用者選的幣別 (例如選了 "EUR" 但 JSON 只有 TWD, USD, JPY, CNY)
            val targetData = allRates[base]

            if (targetData != null) {
                saveRatesToDb(targetData, base)
                Log.d("CurrencyRepo", "本地預設匯率 ($base) 載入成功！")
            } else {
                // 如果找不到該幣別的預設資料，拋出特定錯誤
                throw Exception("OFFLINE_MISSING_CURRENCY")
            }

        } catch (e: Exception) {
            Log.e("CurrencyRepo", "讀取 Assets 失敗: ${e.message}")
            // 將錯誤往上拋，讓 UI 層知道
            throw e
        }
    }

    // [新增] 統一的儲存邏輯 (避免重複程式碼)
    private suspend fun saveRatesToDb(response: ExchangeRateResponse, base: String) {
        val now = System.currentTimeMillis()

        // 注意：如果你 JSON 裡的 base_code 是 TWD，但使用者現在選 USD，
        // 這裡直接存進去會有匯率基準不對的問題。
        // 但作為 "Offline Fallback"，有資料總比沒資料好。
        // 進階做法是根據 JSON 裡的 conversion_rates 做交叉匯率計算，這裡先簡化處理。

        val entities = response.rates.map { (code, rate) ->
            CurrencyRateEntity(
                code = code,
                rate = rate,
                baseCurrency = base, // 標記這些匯率是屬於哪個 Base 的
                lastUpdated = now
            )
        }

        currencyDao.clearAll()
        currencyDao.insertAll(entities)
    }

    suspend fun getRateFor(targetCode: String): Double {
        return currencyDao.getRate(targetCode)?.rate ?: 1.0
    }

    suspend fun getSupportedCurrencies(): List<String> {
        return currencyDao.getAllRates().map { it.code }.sorted()
    }
}