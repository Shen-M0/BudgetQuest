package com.example.budgetquest.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    // --- Plan 相關 ---
    @Query("SELECT * FROM category_table WHERE isVisible = 1")
    fun getVisibleCategoriesStream(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category_table")
    fun getAllCategoriesStream(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    // Tag
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

    // Plan
    @Query("SELECT * FROM plan_table WHERE isActive = 1 ORDER BY id DESC LIMIT 1")
    fun getCurrentPlanStream(): Flow<PlanEntity?>

    @Query("SELECT * FROM plan_table ORDER BY startDate ASC")
    suspend fun getAllPlans(): List<PlanEntity>

    @Query("SELECT * FROM plan_table ORDER BY startDate DESC")
    fun getAllPlansStream(): Flow<List<PlanEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlan(plan: PlanEntity): Long

    @Update
    suspend fun updatePlan(plan: PlanEntity)

    @Query("SELECT * FROM plan_table WHERE id = :id")
    suspend fun getPlanById(id: Int): PlanEntity?

    @Delete
    suspend fun deletePlan(plan: PlanEntity)

    // --- Expense 相關 ---
    @Query("SELECT * FROM expense_table ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

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

    @Query("SELECT * FROM expense_table WHERE date >= :start AND date <= :end")
    suspend fun getExpensesListByDate(start: Long, end: Long): List<ExpenseEntity>

    @Query("DELETE FROM expense_table WHERE date >= :start AND date <= :end")
    suspend fun deleteExpensesByRange(start: Long, end: Long)

    // --- Recurring (固定扣款/訂閱) 相關 ---

    // 1. [UI 用] 透過 Flow 觀察資料變化
    @Query("SELECT * FROM recurring_expense_table ORDER BY id DESC")
    fun getAllRecurringStream(): Flow<List<RecurringExpenseEntity>>

    // 2. [後台邏輯用] 直接取得 List (非 Flow)，用於 checkAndGenerateRecurringExpenses
    @Query("SELECT * FROM recurring_expense_table")
    suspend fun getAllRecurringExpensesList(): List<RecurringExpenseEntity>

    // 3. 透過 ID 取得單筆資料 (詳情頁/編輯用)
    @Query("SELECT * FROM recurring_expense_table WHERE id = :id")
    suspend fun getRecurringExpenseById(id: Long): RecurringExpenseEntity?

    // 4. 新增
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringExpense(expense: RecurringExpenseEntity)

    // 5. 更新 (終止/編輯)
    @Update
    suspend fun updateRecurringExpense(expense: RecurringExpenseEntity)

    // 6. 刪除
    @Delete
    suspend fun deleteRecurringExpense(expense: RecurringExpenseEntity)

    // --- Subscription Tag ---
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

    // --- System ---
    @RawQuery
    suspend fun checkpoint(supportSQLiteQuery: SupportSQLiteQuery): Int
}