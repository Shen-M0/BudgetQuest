package com.example.budgetquest.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Migration 定義保持不變，給舊用戶升級用
val MIGRATION_1_2 = object : Migration(1, 2) { override fun migrate(db: SupportSQLiteDatabase) { } }
val MIGRATION_2_3 = object : Migration(2, 3) { override fun migrate(db: SupportSQLiteDatabase) { } }
val MIGRATION_3_4 = object : Migration(3, 4) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE recurring_expense_table ADD COLUMN endDate INTEGER") } }
val MIGRATION_4_5 = object : Migration(4, 5) { override fun migrate(db: SupportSQLiteDatabase) { /* Migration 邏輯 */ } }
val MIGRATION_5_6 = object : Migration(5, 6) { override fun migrate(db: SupportSQLiteDatabase) { /* Migration 邏輯 */ } }
val MIGRATION_6_7 = object : Migration(6, 7) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE plan_table ADD COLUMN planName TEXT NOT NULL DEFAULT '我的存錢計畫'") } }

@Database(
    entities = [PlanEntity::class, ExpenseEntity::class, RecurringExpenseEntity::class, CategoryEntity::class, TagEntity::class, SubscriptionTagEntity::class],
    version = 7,
    exportSchema = false
)
abstract class BudgetDatabase : RoomDatabase() {

    abstract fun budgetDao(): BudgetDao

    // [新增] 這裡定義預設資料的回呼
    private class BudgetDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // 資料庫建立時執行
            Instance?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.budgetDao())
                }
            }
        }

        suspend fun populateDatabase(budgetDao: BudgetDao) {
            // 1. 預設分類
            val categories = listOf(
                CategoryEntity(name = "飲食", iconKey = "FACE", colorHex = "#FFFFAB91", isDefault = true),
                CategoryEntity(name = "購物", iconKey = "CART", colorHex = "#FF90CAF9", isDefault = true),
                CategoryEntity(name = "交通", iconKey = "HOME", colorHex = "#FFFFF59D", isDefault = true),
                CategoryEntity(name = "娛樂", iconKey = "STAR", colorHex = "#FFCE93D8", isDefault = true),
                CategoryEntity(name = "其他", iconKey = "STAR", colorHex = "#FFE0E0E0", isDefault = true)
            )
            categories.forEach { budgetDao.insertCategory(it) }

            // 2. 預設備註 (一般)
            val tags = listOf("早餐", "午餐", "晚餐", "飲料", "交通", "日用品")
            tags.forEach { budgetDao.insertTag(TagEntity(name = it)) }

            // 3. 預設備註 (訂閱)
            val subs = listOf("Netflix", "Spotify", "YouTube Premium", "YouTube Music", "Apple Music", "Disney+", "iCloud", "ChatGPT", "健身房", "電話費")
            subs.forEach { budgetDao.insertSubTag(SubscriptionTagEntity(name = it)) }
        }
    }

    companion object {
        @Volatile
        private var Instance: BudgetDatabase? = null

        fun getDatabase(context: Context): BudgetDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, BudgetDatabase::class.java, "budget_database")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    // [關鍵] 加入 Callback
                    .addCallback(BudgetDatabaseCallback(CoroutineScope(Dispatchers.IO)))
                    .build()
                    .also { Instance = it }
            }
        }
    }
}