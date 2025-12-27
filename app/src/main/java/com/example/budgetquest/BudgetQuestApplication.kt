package com.example.budgetquest

import android.app.Application
import android.content.Context // <--- 關鍵修正：補上這一行
import com.example.budgetquest.data.BudgetDatabase
import com.example.budgetquest.data.BudgetRepository
import com.example.budgetquest.data.OfflineBudgetRepository

class BudgetQuestApplication : Application() {
    // 建立 AppContainer (手動依賴注入)
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