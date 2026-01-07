package com.example.budgetquest

import android.app.Application
import android.content.Context // <--- 關鍵修正：補上這一行
import com.example.budgetquest.data.BudgetDatabase
import com.example.budgetquest.data.BudgetRepository
import com.example.budgetquest.data.CurrencyRepository
import com.example.budgetquest.data.ExchangeRateApi
import com.example.budgetquest.data.OfflineBudgetRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BudgetQuestApplication : Application() {
    // 建立 AppContainer (手動依賴注入)


    // [修改] 傳入 applicationContext
    val currencyRepository: CurrencyRepository by lazy {
        CurrencyRepository(
            context = this.applicationContext, // [新增這行]
            currencyDao = BudgetDatabase.getDatabase(this).currencyDao(),
            api = CurrencyNetwork.api
        )
    }


    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
    }


}

// 簡單的手動 DI 容器介面
interface AppContainer {
    val budgetRepository: BudgetRepository
}

// 容器實作
class AppDataContainer(private val context: Context) : AppContainer {
    override val budgetRepository: BudgetRepository by lazy {
        OfflineBudgetRepository(BudgetDatabase.getDatabase(context).budgetDao())
    }
}

object CurrencyNetwork {
    private const val BASE_URL = "https://v6.exchangerate-api.com/"

    // 建立 Retrofit 實例 (懶加載，用到時才建立)
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // 提供 API 服務供外部呼叫
    val api: ExchangeRateApi by lazy {
        retrofit.create(ExchangeRateApi::class.java)
    }


}