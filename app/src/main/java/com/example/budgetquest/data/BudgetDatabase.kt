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
    version = 8,
    exportSchema = false
)
abstract class BudgetDatabase : RoomDatabase() {

    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var Instance: BudgetDatabase? = null

        fun getDatabase(context: Context): BudgetDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, BudgetDatabase::class.java, "budget_database")
                    .fallbackToDestructiveMigration() // [重要] 開發階段允許重建資料庫
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // 使用 Coroutine 寫入預設資料
                            CoroutineScope(Dispatchers.IO).launch {
                                populateDatabase(getDatabase(context).budgetDao())
                            }
                        }
                    })
                    .build()
                    .also { Instance = it }
            }
        }

        // [修改] 預設資料現在包含 resourceKey
        private suspend fun populateDatabase(dao: BudgetDao) {
            // 分類
            val categories = listOf(
                CategoryEntity(name = "飲食", iconKey = "FOOD", colorHex = "#EF5350", resourceKey = "cat_food"),
                CategoryEntity(name = "購物", iconKey = "SHOPPING", colorHex = "#EC407A", resourceKey = "cat_shopping"),
                CategoryEntity(name = "交通", iconKey = "TRANSPORT", colorHex = "#AB47BC", resourceKey = "cat_transport"),
                CategoryEntity(name = "居家", iconKey = "HOME", colorHex = "#7E57C2", resourceKey = "cat_home"),
                CategoryEntity(name = "娛樂", iconKey = "ENTERTAINMENT", colorHex = "#5C6BC0", resourceKey = "cat_entertainment"),
                CategoryEntity(name = "醫療", iconKey = "MEDICAL", colorHex = "#42A5F5", resourceKey = "cat_medical"),
                CategoryEntity(name = "教育", iconKey = "EDUCATION", colorHex = "#29B6F6", resourceKey = "cat_education"),
                CategoryEntity(name = "帳單", iconKey = "BILLS", colorHex = "#26C6DA", resourceKey = "cat_bills"),
                CategoryEntity(name = "投資", iconKey = "INVESTMENT", colorHex = "#26A69A", resourceKey = "cat_investment"),
                CategoryEntity(name = "其他", iconKey = "OTHER", colorHex = "#66BB6A", resourceKey = "cat_other"),
                CategoryEntity(name = "旅遊", iconKey = "TRAVEL", colorHex = "#FFA726", resourceKey = "cat_travel")
            )
            categories.forEach { dao.insertCategory(it) }

            // 備註
            val tags = listOf(
                TagEntity(name = "早餐", resourceKey = "note_breakfast"),
                TagEntity(name = "午餐", resourceKey = "note_lunch"),
                TagEntity(name = "晚餐", resourceKey = "note_dinner"),
                TagEntity(name = "飲料", resourceKey = "note_drink"),
                TagEntity(name = "公車", resourceKey = "note_bus"),
                TagEntity(name = "捷運", resourceKey = "note_mrt"),
                TagEntity(name = "加油", resourceKey = "note_gas"),
                TagEntity(name = "電影", resourceKey = "note_movie"),
                TagEntity(name = "遊戲", resourceKey = "note_game"),
                TagEntity(name = "日用品", resourceKey = "note_daily_use")
            )
            tags.forEach { dao.insertTag(it) }

            // 訂閱標籤 (可選，邏輯相同)
            val subTags = listOf(
                // 生活類
                SubscriptionTagEntity(name = "房租", resourceKey = "sub_rent"),
                SubscriptionTagEntity(name = "電話費", resourceKey = "sub_phone"),
                SubscriptionTagEntity(name = "網路費", resourceKey = "sub_internet"),
                SubscriptionTagEntity(name = "水電瓦斯", resourceKey = "sub_utilities"),
                SubscriptionTagEntity(name = "管理費", resourceKey = "sub_management_fee"),
                SubscriptionTagEntity(name = "保險", resourceKey = "sub_insurance"),
                SubscriptionTagEntity(name = "健身房", resourceKey = "sub_gym"),

                // 數位服務類 (雖然大多是英文，但設定 resourceKey 可讓日文版顯示片假名，或未來方便修改)
                SubscriptionTagEntity(name = "Netflix", resourceKey = "sub_netflix"),
                SubscriptionTagEntity(name = "Spotify", resourceKey = "sub_spotify"),
                SubscriptionTagEntity(name = "YouTube Premium", resourceKey = "sub_youtube_premium"),
                SubscriptionTagEntity(name = "YouTube Music", resourceKey = "sub_youtube_music"),
                SubscriptionTagEntity(name = "Disney+", resourceKey = "sub_disney"),
                SubscriptionTagEntity(name = "iCloud", resourceKey = "sub_icloud"),
                SubscriptionTagEntity(name = "Google One", resourceKey = "sub_google_one"),
                SubscriptionTagEntity(name = "ChatGPT", resourceKey = "sub_chatgpt")
            )
            subTags.forEach { dao.insertSubTag(it) }
        }
    }
}