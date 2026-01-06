package com.example.budgetquest.ui.summary

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetquest.data.BudgetRepository
import com.example.budgetquest.data.CategoryEntity
import com.example.budgetquest.data.ExpenseEntity
import com.example.budgetquest.data.PlanEntity
import com.example.budgetquest.data.TagEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class CategoryStat(
    val name: String,
    val totalAmount: Int,
    val percentage: Int,
    val color: Color
)

data class PaymentStat(
    val method: String,
    val amount: Int,
    val percentage: Int
)

data class MerchantStat(
    val name: String,
    val count: Int,
    val totalAmount: Int
)

enum class MerchantSortType { Amount, Count }

enum class BudgetStatus { Achieved, Exceeded, None }

data class SummaryUiState(
    val plan: PlanEntity? = null,
    val filteredExpenses: List<ExpenseEntity> = emptyList(),
    val totalSpent: Int = 0,
    val actualSaved: Int = 0,
    val budgetStatus: BudgetStatus = BudgetStatus.None,
    val categoryStats: List<CategoryStat> = emptyList(),

    val totalRealSpent: Int = 0,

    val needAmount: Int = 0,
    val wantAmount: Int = 0,
    val needPercent: Int = 0,
    val wantPercent: Int = 0,

    val paymentStats: List<PaymentStat> = emptyList(),
    val topMerchants: List<MerchantStat> = emptyList(),
    val merchantSortType: MerchantSortType = MerchantSortType.Amount,

    val searchQuery: String = "",
    val selectedCategories: Set<String> = emptySet(),
    val selectedTags: Set<String> = emptySet()
)

// [新增] 一個內部資料類別，用來暫存篩選條件，解決 combine 參數過多的問題
private data class FilterState(
    val query: String,
    val catFilter: Set<String>,
    val tagFilter: Set<String>,
    val sortType: MerchantSortType
)

class SummaryViewModel(private val repository: BudgetRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategories = MutableStateFlow<Set<String>>(emptySet())
    val selectedCategory = _selectedCategories.map { it.firstOrNull() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTag = _selectedTags.map { it.firstOrNull() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _targetPlanId = MutableStateFlow<Int?>(null)

    private val _merchantSortType = MutableStateFlow(MerchantSortType.Amount)

    fun setPlanId(id: Int) {
        _targetPlanId.value = if (id == -1) null else id
    }

    fun toggleMerchantSortType() {
        _merchantSortType.update { current ->
            if (current == MerchantSortType.Amount) MerchantSortType.Count else MerchantSortType.Amount
        }
    }

    private val targetPlanFlow = combine(_targetPlanId, repository.getAllPlansStream()) { targetId, plans ->
        if (targetId != null) {
            plans.find { it.id == targetId }
        } else {
            val today = System.currentTimeMillis()
            plans.find { plan -> plan.isActive && today >= plan.startDate && today <= plan.endDate }
        }
    }

    // [修正] 1. 先將 4 個篩選條件組合成一個 Flow
    private val filterStateFlow = combine(
        _searchQuery,
        _selectedCategories,
        _selectedTags,
        _merchantSortType
    ) { query, cat, tag, sort ->
        FilterState(query, cat, tag, sort)
    }

    // [修正] 2. 再將數據源 (Plan, Expenses) 與篩選條件 (FilterState) 組合
    // 這樣 combine 只會有 3 個參數，符合限制
    val uiState: StateFlow<SummaryUiState> = combine(
        targetPlanFlow,
        repository.getAllExpensesStream(),
        filterStateFlow
    ) { plan, allExpenses, filterState ->

        // 解構 filterState
        val (query, catFilter, tagFilter, sortType) = filterState

        val planExpenses = if (plan != null) {
            val start = getStartOfDay(plan.startDate)
            val end = getEndOfDay(plan.endDate)
            allExpenses.filter { it.date in start..end }
        } else {
            emptyList()
        }

        val totalRealSpent = planExpenses.sumOf { it.amount }
        val validExpenses = planExpenses.filter { !it.excludeFromBudget }
        val totalSpent = validExpenses.sumOf { it.amount }

        // --- 1. Need / Want 統計 ---
        val needAmount = planExpenses.filter { it.isNeed == true }.sumOf { it.amount }
        val wantAmount = planExpenses.filter { it.isNeed == false }.sumOf { it.amount }
        val totalNW = needAmount + wantAmount

        val (needPercent, wantPercent) = if (totalNW > 0) {
            val nPct = (needAmount.toFloat() / totalNW * 100).toInt()
            val wPct = 100 - nPct
            Pair(nPct, wPct)
        } else {
            Pair(0, 0)
        }

        // --- 2. 支付方式統計 ---
        val rawPaymentStats = planExpenses
            .groupBy { it.paymentMethod.ifBlank { "現金" } }
            .map { (method, list) ->
                val sum = list.sumOf { it.amount }
                PaymentStat(method, sum, if (totalRealSpent > 0) (sum.toFloat() / totalRealSpent * 100).toInt() else 0)
            }
            .sortedByDescending { it.amount }

        val paymentStats = adjustPercentages(rawPaymentStats, { it.percentage }, { item, newPct -> item.copy(percentage = newPct) })

        // --- 3. 店家排行 ---
        val rawMerchants = planExpenses
            .filter { it.merchant.isNotBlank() }
            .groupBy { it.merchant }
            .map { (merchant, list) ->
                MerchantStat(merchant, list.size, list.sumOf { it.amount })
            }

        val topMerchants = when (sortType) {
            MerchantSortType.Amount -> rawMerchants.sortedByDescending { it.totalAmount }
            MerchantSortType.Count -> rawMerchants.sortedByDescending { it.count }
        }.take(5)

        // --- 列表篩選 ---
        val filtered = planExpenses.filter { expense ->
            val matchQuery = query.isBlank() || expense.note.contains(query, ignoreCase = true) || expense.merchant.contains(query, ignoreCase = true)
            val matchCategory = catFilter.isEmpty() || catFilter.contains(expense.category)
            val matchTag = tagFilter.isEmpty() || tagFilter.any { expense.note.contains(it) }
            matchQuery && matchCategory && matchTag
        }.sortedByDescending { it.date }

        val actualSaved = (plan?.totalBudget ?: 0) - totalSpent - (plan?.targetSavings ?: 0)

        val status = if (plan != null) {
            val remaining = (plan.totalBudget - plan.targetSavings) - totalSpent
            if (remaining >= 0) BudgetStatus.Achieved else BudgetStatus.Exceeded
        } else {
            BudgetStatus.None
        }

        val stats = calculateCategoryStats(validExpenses)

        SummaryUiState(
            plan = plan,
            filteredExpenses = filtered,
            totalSpent = totalSpent,
            actualSaved = actualSaved,
            budgetStatus = status,
            categoryStats = stats,

            totalRealSpent = totalRealSpent,
            needAmount = needAmount,
            wantAmount = wantAmount,
            needPercent = needPercent,
            wantPercent = wantPercent,

            paymentStats = paymentStats,
            topMerchants = topMerchants,
            merchantSortType = sortType,

            searchQuery = query,
            selectedCategories = catFilter,
            selectedTags = tagFilter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SummaryUiState()
    )

    // ... (Actions 與 Helper 函式保持不變) ...
    fun toggleMerchantSort() = toggleMerchantSortType()
    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
    fun onCategoryFilterChanged(category: String) {
        _selectedCategories.update { if (it.contains(category)) emptySet() else setOf(category) }
    }
    fun onTagFilterChanged(tag: String) {
        _selectedTags.update { if (it.contains(tag)) emptySet() else setOf(tag) }
    }
    fun updateSearchQuery(query: String) = onSearchQueryChanged(query)
    fun toggleCategoryFilter(category: String) = onCategoryFilterChanged(category)
    fun toggleTagFilter(tag: String) = onTagFilterChanged(tag)
    fun deleteExpense(expense: ExpenseEntity) { viewModelScope.launch { repository.deleteExpense(expense) } }

    val visibleCategories = repository.getVisibleCategoriesStream().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val visibleTags = repository.getVisibleTagsStream().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCategories = repository.getAllCategoriesStream().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTags = repository.getAllTagsStream().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun addCategory(name: String, iconKey: String, colorHex: String) { viewModelScope.launch { repository.insertCategory(CategoryEntity(name = name, iconKey = iconKey, colorHex = colorHex)) } }
    fun toggleCategoryVisibility(category: CategoryEntity) { viewModelScope.launch { repository.updateCategory(category.copy(isVisible = !category.isVisible)) } }
    fun deleteCategory(category: CategoryEntity) { viewModelScope.launch { repository.deleteCategory(category) } }
    fun addTag(name: String) { viewModelScope.launch { repository.insertTag(TagEntity(name = name)) } }
    fun toggleTagVisibility(tag: TagEntity) { viewModelScope.launch { repository.updateTag(tag.copy(isVisible = !tag.isVisible)) } }
    fun deleteTag(tag: TagEntity) { viewModelScope.launch { repository.deleteTag(tag) } }

    private fun <T> adjustPercentages(
        items: List<T>,
        getPercent: (T) -> Int,
        updatePercent: (T, Int) -> T
    ): List<T> {
        if (items.isEmpty()) return items
        val sum = items.sumOf { getPercent(it) }
        val diff = 100 - sum
        if (diff > 0) {
            val maxItemIndex = items.indexOfFirst { it == items.maxByOrNull { item -> getPercent(item) } }
            if (maxItemIndex != -1) {
                return items.mapIndexed { index, item ->
                    if (index == maxItemIndex) updatePercent(item, getPercent(item) + diff) else item
                }
            }
        }
        return items
    }

    private fun calculateCategoryStats(expenses: List<ExpenseEntity>): List<CategoryStat> {
        val total = expenses.sumOf { it.amount }
        if (total == 0) return emptyList()
        val rawStats = expenses.groupBy { it.category }.map { (cat, list) ->
            val sum = list.sumOf { it.amount }
            CategoryStat(name = cat, totalAmount = sum, percentage = (sum.toFloat() / total * 100).toInt(), color = getCategoryColor(cat))
        }.sortedByDescending { it.totalAmount }
        return adjustPercentages(rawStats, { it.percentage }, { item, newPct -> item.copy(percentage = newPct) })
    }

    private fun getCategoryColor(category: String): Color {
        return when (category) {
            "飲食" -> Color(0xFFFFAB91)
            "購物" -> Color(0xFF90CAF9)
            "交通" -> Color(0xFFFFF59D)
            "娛樂" -> Color(0xFFCE93D8)
            else -> Color(0xFFE0E0E0)
        }
    }

    private fun getStartOfDay(time: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = time; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        return c.timeInMillis
    }
    private fun getEndOfDay(time: Long): Long {
        val c = Calendar.getInstance().apply { timeInMillis = time; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }
        return c.timeInMillis
    }
}