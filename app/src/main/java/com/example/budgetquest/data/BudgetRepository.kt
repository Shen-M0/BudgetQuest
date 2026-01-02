package com.example.budgetquest.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Calendar

interface BudgetRepository {
    // Plan
    fun getCurrentPlanStream(): Flow<PlanEntity?>
    fun getAllPlansStream(): Flow<List<PlanEntity>>
    suspend fun getAllPlans(): List<PlanEntity>
    suspend fun insertPlan(plan: PlanEntity): Long
    suspend fun updatePlan(plan: PlanEntity)
    suspend fun getPlanById(id: Int): PlanEntity?
    suspend fun deletePlan(plan: PlanEntity)

    // Expense
    fun getAllExpensesStream(): Flow<List<ExpenseEntity>>
    fun getExpensesByDateStream(date: Long): Flow<List<ExpenseEntity>>
    fun getExpensesByRangeStream(start: Long, end: Long): Flow<List<ExpenseEntity>>
    suspend fun insertExpense(expense: ExpenseEntity)
    suspend fun deleteExpense(expense: ExpenseEntity)
    suspend fun updateExpense(expense: ExpenseEntity)
    suspend fun getExpenseById(id: Long): ExpenseEntity?
    suspend fun getExpensesByRangeList(start: Long, end: Long): List<ExpenseEntity>
    suspend fun deleteExpensesByRange(start: Long, end: Long)

    // Recurring (固定扣款)
    fun getAllRecurringExpensesStream(): Flow<List<RecurringExpenseEntity>> // 改名以配合 DAO 語意
    suspend fun getRecurringExpenseById(id: Long): RecurringExpenseEntity?
    suspend fun insertRecurringExpense(expense: RecurringExpenseEntity)
    suspend fun updateRecurringExpense(expense: RecurringExpenseEntity)
    suspend fun deleteRecurringExpense(expense: RecurringExpenseEntity)
    suspend fun checkAndGenerateRecurringExpenses()

    // Category & Tag
    fun getVisibleCategoriesStream(): Flow<List<CategoryEntity>>
    fun getAllCategoriesStream(): Flow<List<CategoryEntity>>
    suspend fun insertCategory(category: CategoryEntity)
    suspend fun updateCategory(category: CategoryEntity)
    suspend fun deleteCategory(category: CategoryEntity)

    fun getVisibleTagsStream(): Flow<List<TagEntity>>
    fun getAllTagsStream(): Flow<List<TagEntity>>
    suspend fun insertTag(tag: TagEntity)
    suspend fun updateTag(tag: TagEntity)
    suspend fun deleteTag(tag: TagEntity)

    // Subscription Tag
    fun getVisibleSubTagsStream(): Flow<List<SubscriptionTagEntity>>
    fun getAllSubTagsStream(): Flow<List<SubscriptionTagEntity>>
    suspend fun insertSubTag(tag: SubscriptionTagEntity)
    suspend fun updateSubTag(tag: SubscriptionTagEntity)
    suspend fun deleteSubTag(tag: SubscriptionTagEntity)
}

class OfflineBudgetRepository(private val budgetDao: BudgetDao) : BudgetRepository {

    // --- Plan ---
    override fun getCurrentPlanStream(): Flow<PlanEntity?> =
        budgetDao.getCurrentPlanStream().flowOn(Dispatchers.IO)

    override fun getAllPlansStream(): Flow<List<PlanEntity>> =
        budgetDao.getAllPlansStream().flowOn(Dispatchers.IO)

    override suspend fun getAllPlans(): List<PlanEntity> = withContext(Dispatchers.IO) {
        budgetDao.getAllPlans()
    }

    override suspend fun insertPlan(plan: PlanEntity) = withContext(Dispatchers.IO) {
        budgetDao.insertPlan(plan)
    }

    override suspend fun updatePlan(plan: PlanEntity) = withContext(Dispatchers.IO) {
        budgetDao.updatePlan(plan)
    }

    override suspend fun getPlanById(id: Int): PlanEntity? = withContext(Dispatchers.IO) {
        budgetDao.getPlanById(id)
    }

    override suspend fun deletePlan(plan: PlanEntity) = withContext(Dispatchers.IO) {
        budgetDao.deletePlan(plan)
    }

    // --- Expense ---
    override fun getAllExpensesStream(): Flow<List<ExpenseEntity>> =
        budgetDao.getAllExpenses().flowOn(Dispatchers.IO)

    override fun getExpensesByRangeStream(start: Long, end: Long): Flow<List<ExpenseEntity>> {
        return budgetDao.getExpensesByDate(start, end).flowOn(Dispatchers.IO)
    }

    override fun getExpensesByDateStream(date: Long): Flow<List<ExpenseEntity>> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis
        return budgetDao.getExpensesByDate(start, end).flowOn(Dispatchers.IO)
    }

    override suspend fun insertExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        budgetDao.insertExpense(expense)
    }

    override suspend fun deleteExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        budgetDao.deleteExpense(expense)
    }

    override suspend fun updateExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        budgetDao.updateExpense(expense)
    }

    override suspend fun getExpenseById(id: Long): ExpenseEntity? = withContext(Dispatchers.IO) {
        budgetDao.getExpenseById(id)
    }

    override suspend fun getExpensesByRangeList(start: Long, end: Long): List<ExpenseEntity> = withContext(Dispatchers.IO) {
        budgetDao.getExpensesListByDate(start, end)
    }

    override suspend fun deleteExpensesByRange(start: Long, end: Long) = withContext(Dispatchers.IO) {
        budgetDao.deleteExpensesByRange(start, end)
    }

    // --- Recurring (固定扣款) ---

    // UI 用 (Flow)
    override fun getAllRecurringExpensesStream(): Flow<List<RecurringExpenseEntity>> =
        budgetDao.getAllRecurringStream().flowOn(Dispatchers.IO)

    // 詳情/編輯用
    override suspend fun getRecurringExpenseById(id: Long): RecurringExpenseEntity? = withContext(Dispatchers.IO) {
        budgetDao.getRecurringExpenseById(id)
    }

    override suspend fun insertRecurringExpense(expense: RecurringExpenseEntity) = withContext(Dispatchers.IO) {
        budgetDao.insertRecurringExpense(expense)
    }

    override suspend fun updateRecurringExpense(expense: RecurringExpenseEntity) = withContext(Dispatchers.IO) {
        budgetDao.updateRecurringExpense(expense)
    }

    override suspend fun deleteRecurringExpense(expense: RecurringExpenseEntity) = withContext(Dispatchers.IO) {
        budgetDao.deleteRecurringExpense(expense)
    }

    // 後台檢查邏輯
    override suspend fun checkAndGenerateRecurringExpenses() = withContext(Dispatchers.IO) {
        // [修正] 這裡使用 getAllRecurringExpensesList() 取得 List，而不是 Flow
        val recurringList = budgetDao.getAllRecurringExpensesList()
        val today = System.currentTimeMillis()

        recurringList.forEach { recurring ->
            var nextDueDate = calculateNextDueDate(recurring)
            var currentRecurring = recurring
            var hasUpdates = false

            // 檢查是否到期 且 (沒有結束日期 或 未超過結束日期)
            while (nextDueDate <= today && (recurring.endDate == null || nextDueDate <= recurring.endDate!!)) { // 注意 endDate!! 的判斷
                val newExpense = ExpenseEntity(
                    date = nextDueDate,
                    amount = currentRecurring.amount,
                    note = "${currentRecurring.note} (自動扣款)",
                    category = currentRecurring.category
                )
                budgetDao.insertExpense(newExpense)

                currentRecurring = currentRecurring.copy(lastGeneratedDate = nextDueDate)
                hasUpdates = true
                nextDueDate = calculateNextDueDate(currentRecurring)
            }

            if (hasUpdates) {
                budgetDao.updateRecurringExpense(currentRecurring)
            }
        }
    }

    private fun calculateNextDueDate(recurring: RecurringExpenseEntity): Long {
        val calendar = Calendar.getInstance()
        // 如果從未生成過，從開始日期算起；否則從上次生成日期算起
        val baseDate = if (recurring.lastGeneratedDate == 0L) recurring.startDate else recurring.lastGeneratedDate
        calendar.timeInMillis = baseDate

        when (recurring.frequency) {
            "MONTH" -> calendar.add(Calendar.MONTH, 1)
            "WEEK" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "DAY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "CUSTOM" -> calendar.add(Calendar.DAY_OF_YEAR, recurring.customDays)
        }
        return calendar.timeInMillis
    }

    // --- Category & Tag & SubTag ---
    override fun getVisibleCategoriesStream() = budgetDao.getVisibleCategoriesStream().flowOn(Dispatchers.IO)
    override fun getAllCategoriesStream() = budgetDao.getAllCategoriesStream().flowOn(Dispatchers.IO)
    override suspend fun insertCategory(category: CategoryEntity) = withContext(Dispatchers.IO) { budgetDao.insertCategory(category) }
    override suspend fun updateCategory(category: CategoryEntity) = withContext(Dispatchers.IO) { budgetDao.updateCategory(category) }
    override suspend fun deleteCategory(category: CategoryEntity) = withContext(Dispatchers.IO) { budgetDao.deleteCategory(category) }

    override fun getVisibleTagsStream() = budgetDao.getVisibleTagsStream().flowOn(Dispatchers.IO)
    override fun getAllTagsStream() = budgetDao.getAllTagsStream().flowOn(Dispatchers.IO)
    override suspend fun insertTag(tag: TagEntity) = withContext(Dispatchers.IO) { budgetDao.insertTag(tag) }
    override suspend fun updateTag(tag: TagEntity) = withContext(Dispatchers.IO) { budgetDao.updateTag(tag) }
    override suspend fun deleteTag(tag: TagEntity) = withContext(Dispatchers.IO) { budgetDao.deleteTag(tag) }

    override fun getVisibleSubTagsStream() = budgetDao.getVisibleSubTagsStream().flowOn(Dispatchers.IO)
    override fun getAllSubTagsStream() = budgetDao.getAllSubTagsStream().flowOn(Dispatchers.IO)
    override suspend fun insertSubTag(tag: SubscriptionTagEntity) = withContext(Dispatchers.IO) { budgetDao.insertSubTag(tag) }
    override suspend fun updateSubTag(tag: SubscriptionTagEntity) = withContext(Dispatchers.IO) { budgetDao.updateSubTag(tag) }
    override suspend fun deleteSubTag(tag: SubscriptionTagEntity) = withContext(Dispatchers.IO) { budgetDao.deleteSubTag(tag) }
}