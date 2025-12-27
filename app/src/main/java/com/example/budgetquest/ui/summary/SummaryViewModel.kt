package com.example.budgetquest.ui.summary

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.budgetquest.data.BudgetRepository
import com.example.budgetquest.data.ExpenseEntity
import com.example.budgetquest.data.PlanEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar // 記得 import
import com.example.budgetquest.data.CategoryEntity
import com.example.budgetquest.data.TagEntity
import kotlinx.coroutines.flow.stateIn

data class CategoryStat(
    val name: String,
    val totalAmount: Int,
    val color: Color
)
data class SummaryUiState(
    val plan: PlanEntity? = null,
    val filteredExpenses: List<ExpenseEntity> = emptyList(),
    val totalSpent: Int = 0,
    val actualSaved: Int = 0,
    val resultMessage: String = "",
    val categoryStats: List<CategoryStat> = emptyList()
)

class SummaryViewModel(
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag = _selectedTag.asStateFlow()

    private val _currentPlanId = MutableStateFlow<Int?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SummaryUiState> = _currentPlanId.flatMapLatest { planId ->
        if (planId == null || planId == -1) {
            flowOf(SummaryUiState())
        } else {
            val planFlow = flow { emit(budgetRepository.getPlanById(planId)) }

            planFlow.flatMapLatest { plan ->
                if (plan == null) {
                    flowOf(SummaryUiState())
                } else {
                    // [BUG 修復核心]
                    // 將計畫的開始與結束時間，強制擴展到當天的 00:00:00 與 23:59:59
                    // 確保即使消費紀錄的時間點早於計畫建立的當下時間，只要是同一天也能被抓到
                    val calendar = Calendar.getInstance()

                    // 設定開始時間為當天 00:00:00
                    calendar.timeInMillis = plan.startDate
                    calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                    val queryStart = calendar.timeInMillis

                    // 設定結束時間為當天 23:59:59
                    calendar.timeInMillis = plan.endDate
                    calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
                    val queryEnd = calendar.timeInMillis

                    combine(
                        // 使用修正後的 queryStart 和 queryEnd
                        budgetRepository.getExpensesByRangeStream(queryStart, queryEnd),
                        _searchQuery,
                        _selectedCategory,
                        _selectedTag
                    ) { expenses, query, catFilter, tagFilter ->

                        val filtered = expenses.filter { expense ->
                            val matchQuery = query.isBlank() || expense.note.contains(query, ignoreCase = true)
                            val matchCategory = catFilter == null || expense.category == catFilter
                            val matchTag = tagFilter == null || expense.note.contains(tagFilter)
                            matchQuery && matchCategory && matchTag
                        }

                        val totalSpent = expenses.sumOf { it.amount }
                        val actualSaved = plan.totalBudget - totalSpent

                        // 日系溫柔風格的評語
                        val message = if (actualSaved >= plan.targetSavings) {
                            "太棒了！目標達成 🎉\n好習慣正在慢慢養成中。"
                        } else if (actualSaved > 0) {
                            "做得不錯！\n雖然未達標，但依然在進步。"
                        } else {
                            "稍微透支了呢。\n沒關係，下個階段再調整就好。"
                        }

                        val stats = expenses
                            .groupBy { it.category }
                            .map { (category, list) ->
                                CategoryStat(
                                    name = category,
                                    totalAmount = list.sumOf { it.amount },
                                    color = getCategoryColor(category)
                                )
                            }
                            .sortedByDescending { it.totalAmount }

                        SummaryUiState(
                            plan = plan,
                            filteredExpenses = filtered,
                            totalSpent = totalSpent,
                            actualSaved = actualSaved,
                            resultMessage = message,
                            categoryStats = stats
                        )
                    }
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SummaryUiState()
    )

    fun initialize(planId: Int) { _currentPlanId.value = planId }

    // [日系配色] 低飽和度、莫蘭迪色系
    private fun getCategoryColor(category: String): Color {
        return when(category) {
            "飲食" -> Color(0xFFFFAB91) // 柔和橘
            "購物" -> Color(0xFF90CAF9) // 柔和藍
            "交通" -> Color(0xFFFFF59D) // 柔和黃
            "娛樂" -> Color(0xFFCE93D8) // 柔和紫
            else -> Color(0xFFE0E0E0)   // 淺灰
        }
    }

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
    fun onCategoryFilterChanged(category: String) {
        if (_selectedCategory.value == category) _selectedCategory.value = null else _selectedCategory.value = category
    }
    fun onTagFilterChanged(tag: String) {
        if (_selectedTag.value == tag) _selectedTag.value = null else _selectedTag.value = tag
    }
    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch { budgetRepository.deleteExpense(expense) }
    }

    // [新增] 分類與標籤 (用於篩選器列表)
    val visibleCategories = budgetRepository.getVisibleCategoriesStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val visibleTags = budgetRepository.getVisibleTagsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // [新增] 所有分類與標籤 (用於管理 Dialog)
    val allCategories = budgetRepository.getAllCategoriesStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allTags = budgetRepository.getAllTagsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // [新增] CRUD 操作 (直接複製 TransactionViewModel 的邏輯)
    fun addCategory(name: String, iconKey: String, colorHex: String) {
        viewModelScope.launch { budgetRepository.insertCategory(CategoryEntity(name = name, iconKey = iconKey, colorHex = colorHex)) }
    }
    fun toggleCategoryVisibility(category: CategoryEntity) {
        viewModelScope.launch { budgetRepository.updateCategory(category.copy(isVisible = !category.isVisible)) }
    }
    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch { budgetRepository.deleteCategory(category) }
    }

    fun addTag(name: String) {
        viewModelScope.launch { budgetRepository.insertTag(TagEntity(name = name)) }
    }
    fun toggleTagVisibility(tag: TagEntity) {
        viewModelScope.launch { budgetRepository.updateTag(tag.copy(isVisible = !tag.isVisible)) }
    }
    fun deleteTag(tag: TagEntity) {
        viewModelScope.launch { budgetRepository.deleteTag(tag) }
    }




}