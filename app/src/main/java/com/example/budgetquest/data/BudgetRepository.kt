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

    // Expense
    fun getAllExpensesStream(): Flow<List<ExpenseEntity>>
    fun getExpensesByDateStream(date: Long): Flow<List<ExpenseEntity>>
    fun getExpensesByRangeStream(start: Long, end: Long): Flow<List<ExpenseEntity>>
    suspend fun insertExpense(expense: ExpenseEntity)
    suspend fun deleteExpense(expense: ExpenseEntity)
    suspend fun updateExpense(expense: ExpenseEntity)
    suspend fun getExpenseById(id: Long): ExpenseEntity?

    // Recurring
    fun getAllRecurringStream(): Flow<List<RecurringExpenseEntity>>
    suspend fun addRecurring(recurring: RecurringExpenseEntity)
    suspend fun deleteRecurring(recurring: RecurringExpenseEntity)
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

    // [新增]
    suspend fun getExpensesByRangeList(start: Long, end: Long): List<ExpenseEntity>

    // 在 BudgetRepository 介面中加入
    suspend fun deletePlan(plan: PlanEntity)



}

class OfflineBudgetRepository(private val budgetDao: BudgetDao) : BudgetRepository {

    // --- Plan ---
    override fun getCurrentPlanStream(): Flow<PlanEntity?> =
        budgetDao.getCurrentPlanStream().flowOn(Dispatchers.IO)

    override fun getAllPlansStream(): Flow<List<PlanEntity>> =
        budgetDao.getAllPlansStream().flowOn(Dispatchers.IO)

    // [關鍵檢查 1] 這裡必須有 suspend，且呼叫 budgetDao.getAllPlans()
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

    // --- Recurring ---
    override fun getAllRecurringStream(): Flow<List<RecurringExpenseEntity>> =
        budgetDao.getAllRecurringStream().flowOn(Dispatchers.IO)

    override suspend fun addRecurring(recurring: RecurringExpenseEntity) = withContext(Dispatchers.IO) {
        budgetDao.addRecurring(recurring)
    }

    override suspend fun deleteRecurring(recurring: RecurringExpenseEntity) = withContext(Dispatchers.IO) {
        budgetDao.deleteRecurring(recurring)
    }

    override suspend fun checkAndGenerateRecurringExpenses() = withContext(Dispatchers.IO) {
        val recurringList = budgetDao.getAllRecurringExpenses()
        val today = System.currentTimeMillis()

        recurringList.forEach { recurring ->
            var nextDueDate = calculateNextDueDate(recurring)
            var currentRecurring = recurring
            var hasUpdates = false

            // 檢查是否到期 且 (沒有結束日期 或 未超過結束日期)
            while (nextDueDate <= today && (recurring.endDate == null || nextDueDate <= recurring.endDate)) {
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
                budgetDao.updateRecurring(currentRecurring)
            }
        }
    }

    private fun calculateNextDueDate(recurring: RecurringExpenseEntity): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = recurring.lastGeneratedDate
        when (recurring.frequency) {
            "MONTH" -> calendar.add(Calendar.MONTH, 1)
            "WEEK" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "DAY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "CUSTOM" -> calendar.add(Calendar.DAY_OF_YEAR, recurring.customDays)
        }
        return calendar.timeInMillis
    }

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

    // [關鍵檢查 2] 這裡必須有 suspend，且呼叫 budgetDao.getExpensesListByDate
    override suspend fun getExpensesByRangeList(start: Long, end: Long): List<ExpenseEntity> = withContext(Dispatchers.IO) {
        budgetDao.getExpensesListByDate(start, end)
    }

    // 在 OfflineBudgetRepository 類別中加入
    override suspend fun deletePlan(plan: PlanEntity) = budgetDao.deletePlan(plan)



}