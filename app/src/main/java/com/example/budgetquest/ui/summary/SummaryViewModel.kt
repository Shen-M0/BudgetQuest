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

data class SummaryUiState(
    val plan: PlanEntity? = null,
    val filteredExpenses: List<ExpenseEntity> = emptyList(),
    val totalSpent: Int = 0,
    val actualSaved: Int = 0,
    val resultMessage: String = "",
    val categoryStats: List<CategoryStat> = emptyList(),
    val searchQuery: String = "",
    val selectedCategories: Set<String> = emptySet(),
    val selectedTags: Set<String> = emptySet()
)

class SummaryViewModel(private val repository: BudgetRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategories = MutableStateFlow<Set<String>>(emptySet())
    val selectedCategory = _selectedCategories.map { it.firstOrNull() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTag = _selectedTags.map { it.firstOrNull() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // [新增] 指定要查看的計畫 ID (null 或 -1 代表未指定)
    private val _targetPlanId = MutableStateFlow<Int?>(null)

    // [新增] 設定計畫 ID
    fun setPlanId(id: Int) {
        _targetPlanId.value = if (id == -1) null else id
    }

    // [關鍵修正] 決定要顯示哪個計畫
    // 邏輯：如果有指定 ID -> 找該 ID 的計畫
    //       如果沒指定   -> 找包含今天且 Active 的計畫 (預設行為)
    private val targetPlanFlow = combine(_targetPlanId, repository.getAllPlansStream()) { targetId, plans ->
        if (targetId != null) {
            plans.find { it.id == targetId }
        } else {
            val today = System.currentTimeMillis()
            plans.find { plan -> plan.isActive && today >= plan.startDate && today <= plan.endDate }
        }
    }

    val uiState: StateFlow<SummaryUiState> = combine(
        targetPlanFlow, // 改用這個新的 Flow
        repository.getAllExpensesStream(),
        _searchQuery,
        _selectedCategories,
        _selectedTags
    ) { plan, allExpenses, query, catFilter, tagFilter ->

        val planExpenses = if (plan != null) {
            val start = getStartOfDay(plan.startDate)
            val end = getEndOfDay(plan.endDate)
            allExpenses.filter { it.date in start..end }
        } else {
            emptyList()
        }

        val filtered = planExpenses.filter { expense ->
            val matchQuery = query.isBlank() || expense.note.contains(query, ignoreCase = true)
            val matchCategory = catFilter.isEmpty() || catFilter.contains(expense.category)
            val matchTag = tagFilter.isEmpty() || tagFilter.any { expense.note.contains(it) }
            matchQuery && matchCategory && matchTag
        }.sortedByDescending { it.date }

        val totalSpent = planExpenses.sumOf { it.amount }
        val actualSaved = (plan?.totalBudget ?: 0) - totalSpent - (plan?.targetSavings ?: 0)

        val message = if (plan != null) {
            val remaining = (plan.totalBudget - plan.targetSavings) - totalSpent
            if (remaining >= 0) "太棒了！目標達成 🎉\n好習慣正在慢慢養成中。"
            else "注意！預算已超支 ⚠️\n建議檢視非必要開銷。"
        } else {
            "無計畫資料"
        }

        val stats = calculateCategoryStats(planExpenses)

        SummaryUiState(
            plan = plan,
            filteredExpenses = filtered,
            totalSpent = totalSpent,
            actualSaved = actualSaved,
            resultMessage = message,
            categoryStats = stats,
            searchQuery = query,
            selectedCategories = catFilter,
            selectedTags = tagFilter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SummaryUiState()
    )

    // --- Actions ---
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

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch { repository.deleteExpense(expense) }
    }

    // --- Management ---
    val visibleCategories = repository.getVisibleCategoriesStream().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val visibleTags = repository.getVisibleTagsStream().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allCategories = repository.getAllCategoriesStream().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTags = repository.getAllTagsStream().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // [修改] 接收 名稱、圖示、顏色 三個參數
    fun addCategory(name: String, iconKey: String, colorHex: String) {
        viewModelScope.launch {
            repository.insertCategory(
                CategoryEntity(
                    name = name,
                    iconKey = iconKey,
                    colorHex = colorHex
                )
            )
        }
    }
    fun toggleCategoryVisibility(category: CategoryEntity) { viewModelScope.launch { repository.updateCategory(category.copy(isVisible = !category.isVisible)) } }
    fun deleteCategory(category: CategoryEntity) { viewModelScope.launch { repository.deleteCategory(category) } }
    fun addTag(name: String) { viewModelScope.launch { repository.insertTag(TagEntity(name = name)) } }
    fun toggleTagVisibility(tag: TagEntity) { viewModelScope.launch { repository.updateTag(tag.copy(isVisible = !tag.isVisible)) } }
    fun deleteTag(tag: TagEntity) { viewModelScope.launch { repository.deleteTag(tag) } }

    // --- Helper ---
    private fun calculateCategoryStats(expenses: List<ExpenseEntity>): List<CategoryStat> {
        val total = expenses.sumOf { it.amount }
        if (total == 0) return emptyList()
        return expenses.groupBy { it.category }.map { (cat, list) ->
            val sum = list.sumOf { it.amount }
            CategoryStat(name = cat, totalAmount = sum, percentage = (sum.toFloat() / total * 100).toInt(), color = getCategoryColor(cat))
        }.sortedByDescending { it.totalAmount }
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