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

// Migration 定義
val MIGRATION_1_2 = object : Migration(1, 2) { override fun migrate(db: SupportSQLiteDatabase) { } }
val MIGRATION_2_3 = object : Migration(2, 3) { override fun migrate(db: SupportSQLiteDatabase) { } }
val MIGRATION_3_4 = object : Migration(3, 4) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE recurring_expense_table ADD COLUMN endDate INTEGER") } }
val MIGRATION_4_5 = object : Migration(4, 5) { override fun migrate(db: SupportSQLiteDatabase) { /* Migration 邏輯 */ } }
val MIGRATION_5_6 = object : Migration(5, 6) { override fun migrate(db: SupportSQLiteDatabase) { /* Migration 邏輯 */ } }
val MIGRATION_6_7 = object : Migration(6, 7) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE plan_table ADD COLUMN planName TEXT NOT NULL DEFAULT '我的存錢計畫'") } }

// [修正] 版本 8 -> 9 的遷移邏輯：配合新的需求 (預設空值/Null)
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // --- 1. 更新 expense_table ---
        db.execSQL("ALTER TABLE expense_table ADD COLUMN imageUri TEXT")
        // paymentMethod 預設空字串
        db.execSQL("ALTER TABLE expense_table ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE expense_table ADD COLUMN merchant TEXT")
        db.execSQL("ALTER TABLE expense_table ADD COLUMN excludeFromBudget INTEGER NOT NULL DEFAULT 0")
        // isNeed 預設 NULL
        db.execSQL("ALTER TABLE expense_table ADD COLUMN isNeed INTEGER")
        // recurringRuleId
        db.execSQL("ALTER TABLE expense_table ADD COLUMN recurringRuleId INTEGER")

        // --- 2. 更新 recurring_expense_table (補齊欄位) ---
        db.execSQL("ALTER TABLE recurring_expense_table ADD COLUMN imageUri TEXT")
        db.execSQL("ALTER TABLE recurring_expense_table ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE recurring_expense_table ADD COLUMN merchant TEXT")
        db.execSQL("ALTER TABLE recurring_expense_table ADD COLUMN excludeFromBudget INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE recurring_expense_table ADD COLUMN isNeed INTEGER")
    }
}

@Database(
    entities = [
        PlanEntity::class,
        ExpenseEntity::class,
        RecurringExpenseEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        SubscriptionTagEntity::class,
        PaymentMethodEntity::class // [確保] 這裡有包含
    ],
    version = 9,
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
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                        MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
                        MIGRATION_8_9
                    )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                populateDatabase(getDatabase(context).budgetDao())
                            }
                        }
                    })
                    .build()
                    .also { Instance = it }
            }
        }

        // 預設資料
        private suspend fun populateDatabase(dao: BudgetDao) {
            // 1. 分類
            val categories = listOf(
                CategoryEntity(name = "飲食", iconKey = "FOOD", colorHex = "#EF5350", resourceKey = "cat_food"),
                CategoryEntity(name = "娛樂", iconKey = "ENTERTAINMENT", colorHex = "#5C6BC0", resourceKey = "cat_entertainment"),
                CategoryEntity(name = "帳單", iconKey = "BILLS", colorHex = "#26C6DA", resourceKey = "cat_bills"),
                CategoryEntity(name = "居家", iconKey = "HOME", colorHex = "#7E57C2", resourceKey = "cat_home"),
                CategoryEntity(name = "購物", iconKey = "SHOPPING", colorHex = "#EC407A", resourceKey = "cat_shopping"),
                CategoryEntity(name = "交通", iconKey = "TRANSPORT", colorHex = "#AB47BC", resourceKey = "cat_transport"),
                CategoryEntity(name = "醫療", iconKey = "MEDICAL", colorHex = "#42A5F5", resourceKey = "cat_medical"),
                CategoryEntity(name = "教育", iconKey = "EDUCATION", colorHex = "#29B6F6", resourceKey = "cat_education"),
                CategoryEntity(name = "投資", iconKey = "INVESTMENT", colorHex = "#26A69A", resourceKey = "cat_investment"),
                CategoryEntity(name = "旅遊", iconKey = "TRAVEL", colorHex = "#FFA726", resourceKey = "cat_travel") ,
                CategoryEntity(name = "其他", iconKey = "OTHER", colorHex = "#66BB6A", resourceKey = "cat_other")
            )
            categories.forEach { dao.insertCategory(it) }

            // 2. 備註
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

            // 3. 訂閱標籤
            val subTags = listOf(
                SubscriptionTagEntity(name = "Netflix", resourceKey = "sub_netflix"),
                SubscriptionTagEntity(name = "Spotify", resourceKey = "sub_spotify"),
                SubscriptionTagEntity(name = "YouTube Premium", resourceKey = "sub_youtube_premium"),
                SubscriptionTagEntity(name = "YouTube Music", resourceKey = "sub_youtube_music"),
                SubscriptionTagEntity(name = "Disney+", resourceKey = "sub_disney"),
                SubscriptionTagEntity(name = "iCloud", resourceKey = "sub_icloud"),
                SubscriptionTagEntity(name = "Google One", resourceKey = "sub_google_one"),
                SubscriptionTagEntity(name = "ChatGPT", resourceKey = "sub_chatgpt"),
                SubscriptionTagEntity(name = "房租", resourceKey = "sub_rent"),
                SubscriptionTagEntity(name = "電話費", resourceKey = "sub_phone"),
                SubscriptionTagEntity(name = "網路費", resourceKey = "sub_internet"),
                SubscriptionTagEntity(name = "水電瓦斯", resourceKey = "sub_utilities"),
                SubscriptionTagEntity(name = "管理費", resourceKey = "sub_management_fee"),
                SubscriptionTagEntity(name = "保險", resourceKey = "sub_insurance"),
                SubscriptionTagEntity(name = "健身房", resourceKey = "sub_gym")
            )
            subTags.forEach { dao.insertSubTag(it) }

            // 4. [新增] 支付方式 (完全依照您的風格)
            val paymentMethods = listOf(
                PaymentMethodEntity(name = "現金", order = 0),
                PaymentMethodEntity(name = "信用卡", order = 1),
                PaymentMethodEntity(name = "LinePay", order = 2),
                PaymentMethodEntity(name = "悠遊卡", order = 3),
                PaymentMethodEntity(name = "轉帳", order = 4)
            )
            paymentMethods.forEach { dao.insertPaymentMethod(it) }
        }
    }
}