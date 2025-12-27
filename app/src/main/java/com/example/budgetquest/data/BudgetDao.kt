package com.example.budgetquest.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import androidx.sqlite.db.SupportSQLiteQuery // [新增 import]
@Dao
interface BudgetDao {
    // --- Plan 相關 ---

    // 只抓取 "可見" 的分類 (給記帳頁面用)
    // 只抓取 "可見" 的分類 (給記帳頁面用)
    @Query("SELECT * FROM category_table WHERE isVisible = 1")
    fun getVisibleCategoriesStream(): Flow<List<CategoryEntity>>

    // 抓取 "所有" 分類 (給管理頁面用，包含隱藏的)
    @Query("SELECT * FROM category_table")
    fun getAllCategoriesStream(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    // Tag 同理
    @Query("SELECT * FROM tag_table WHERE isVisible = 1")
    fun getVisibleTagsStream(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tag_table")
    fun getAllTagsStream(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity)

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Query("SELECT * FROM plan_table WHERE isActive = 1 ORDER BY id DESC LIMIT 1")
    fun getCurrentPlanStream(): Flow<PlanEntity?>

    @Query("SELECT * FROM plan_table ORDER BY startDate ASC")
    suspend fun getAllPlans(): List<PlanEntity>

    @Query("SELECT * FROM plan_table ORDER BY startDate DESC")
    fun getAllPlansStream(): Flow<List<PlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: PlanEntity)

    @Update
    suspend fun updatePlan(plan: PlanEntity)

    @Query("SELECT * FROM plan_table WHERE id = :id")
    suspend fun getPlanById(id: Int): PlanEntity?

    // --- Expense 相關 ---
    @Query("SELECT * FROM expense_table ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    // 範圍查詢
    @Query("SELECT * FROM expense_table WHERE date >= :start AND date <= :end ORDER BY date DESC")
    fun getExpensesByDate(start: Long, end: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expense_table WHERE id = :id")
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    // --- Recurring (訂閱) 相關 ---

    // 1. 給 UI 顯示用的 Flow
    @Query("SELECT * FROM recurring_expense_table ORDER BY id DESC")
    fun getAllRecurringStream(): Flow<List<RecurringExpenseEntity>>

    // 2. 給自動扣款邏輯用的 List (一次性讀取)
    @Query("SELECT * FROM recurring_expense_table")
    suspend fun getAllRecurringExpenses(): List<RecurringExpenseEntity>

    // 3. 新增訂閱
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addRecurring(recurring: RecurringExpenseEntity)

    // 4. 更新訂閱
    @Update
    suspend fun updateRecurring(recurring: RecurringExpenseEntity)

    // 5. 刪除訂閱
    @Delete
    suspend fun deleteRecurring(recurring: RecurringExpenseEntity)

    // --- Subscription Tag (訂閱備註) ---
    @Query("SELECT * FROM subscription_tag_table WHERE isVisible = 1")
    fun getVisibleSubTagsStream(): Flow<List<SubscriptionTagEntity>>

    @Query("SELECT * FROM subscription_tag_table")
    fun getAllSubTagsStream(): Flow<List<SubscriptionTagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubTag(tag: SubscriptionTagEntity)

    @Update
    suspend fun updateSubTag(tag: SubscriptionTagEntity)

    @Delete
    suspend fun deleteSubTag(tag: SubscriptionTagEntity)

    // BudgetDao.kt
    @Query("SELECT * FROM expense_table WHERE date >= :start AND date <= :end")
    suspend fun getExpensesListByDate(start: Long, end: Long): List<ExpenseEntity>

    // [新增] 強制寫入 Checkpoint，確保 .db 檔案包含最新資料
    @RawQuery
    suspend fun checkpoint(supportSQLiteQuery: SupportSQLiteQuery): Int

}