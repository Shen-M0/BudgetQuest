package com.example.budgetquest.ui.subscription

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.R
import com.example.budgetquest.data.RecurringExpenseEntity
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.common.AuroraPrimaryButton
import com.example.budgetquest.ui.common.FluidBoundsTransform
import com.example.budgetquest.ui.common.GlassCard
import com.example.budgetquest.ui.common.GlassChip
import com.example.budgetquest.ui.common.GlassIconButton
import com.example.budgetquest.ui.common.GlassTextField
import com.example.budgetquest.ui.common.getIconByKey
import com.example.budgetquest.ui.common.getSmartCategoryName
import com.example.budgetquest.ui.common.getSmartTagName
import com.example.budgetquest.ui.theme.AppTheme
import com.example.budgetquest.ui.transaction.CategoryManagerDialog
import com.example.budgetquest.ui.transaction.SubTagManagerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SubscriptionScreen(
    planId: Int,
    startDate: Long,
    endDate: Long,
    editId: Long = -1L,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    onItemClick: (Long) -> Unit,
    viewModel: SubscriptionViewModel = viewModel(factory = AppViewModelProvider.Factory),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val uiState = viewModel.uiState
    // [修正] 因為 ViewModel 只有一個 recurringList 了，這裡的型別推斷就會恢復正常
    val list by viewModel.recurringList.collectAsState()
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessageId) {
        uiState.errorMessageId?.let { errorId ->
            snackbarHostState.showSnackbar(
                message = context.getString(errorId),
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    val periodsMap = mapOf(
        "MONTH" to R.string.freq_month,
        "WEEK" to R.string.freq_week,
        "DAY" to R.string.freq_day,
        "CUSTOM" to R.string.freq_custom
    )

    var lastClickTime by remember { mutableLongStateOf(0L) }
    fun debounce(action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 500L) {
            lastClickTime = now
            action()
        }
    }

    // 初始化與載入編輯資料
    LaunchedEffect(planId, startDate, endDate, editId) {
        viewModel.initialize(planId, startDate, endDate)
        if (editId != -1L) {
            // 如果有傳入 editId，通知 ViewModel 載入該筆資料到輸入框
            viewModel.loadForEditing(editId)
        }
    }

    val startCalendar = remember { Calendar.getInstance() }
    val endCalendar = remember { Calendar.getInstance() }

    val dateFormatStr = stringResource(R.string.format_date_standard)
    val dateFormatter = remember(dateFormatStr) { SimpleDateFormat(dateFormatStr, Locale.getDefault()) }

    val startDatePickerDialog = remember(context) {
        DatePickerDialog(
            context,
            { _, y, m, d ->
                startCalendar.set(y, m, d)
                viewModel.updateUiState(startDate = startCalendar.timeInMillis)
            },
            startCalendar.get(Calendar.YEAR), startCalendar.get(Calendar.MONTH), startCalendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    LaunchedEffect(uiState.startDate) {
        startCalendar.timeInMillis = uiState.startDate
        startDatePickerDialog.updateDate(startCalendar.get(Calendar.YEAR), startCalendar.get(Calendar.MONTH), startCalendar.get(Calendar.DAY_OF_MONTH))
    }

    val endDatePickerDialog = remember(context) {
        DatePickerDialog(
            context,
            { _, y, m, d ->
                endCalendar.set(y, m, d)
                viewModel.updateUiState(endDate = endCalendar.timeInMillis)
            },
            endCalendar.get(Calendar.YEAR), endCalendar.get(Calendar.MONTH), endCalendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    LaunchedEffect(uiState.endDate) {
        val time = uiState.endDate ?: System.currentTimeMillis()
        endCalendar.timeInMillis = time
        endDatePickerDialog.updateDate(endCalendar.get(Calendar.YEAR), endCalendar.get(Calendar.MONTH), endCalendar.get(Calendar.DAY_OF_MONTH))
    }

    val categories by viewModel.visibleCategories.collectAsState()
    val subTags by viewModel.visibleSubTags.collectAsState()
    var showCategoryManager by remember { mutableStateOf(false) }
    var showSubTagManager by remember { mutableStateOf(false) }

    if (showCategoryManager) {
        val allCategories by viewModel.allCategories.collectAsState()
        CategoryManagerDialog(allCategories, { showCategoryManager = false }, viewModel::addCategory, viewModel::toggleCategoryVisibility, viewModel::deleteCategory)
    }
    if (showSubTagManager) {
        val allSubTags by viewModel.allSubTags.collectAsState()
        SubTagManagerDialog(allSubTags, { showSubTagManager = false }, viewModel::addSubTag, viewModel::toggleSubTagVisibility, viewModel::deleteSubTag)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    // 根據是否為編輯模式顯示不同標題
                    val title = if (editId != -1L) "編輯固定扣款" else stringResource(R.string.title_fixed_expenses)
                    Text(title, color = AppTheme.colors.textPrimary, fontSize = 18.sp)
                },
                navigationIcon = {
                    GlassIconButton(
                        onClick = { debounce(onBackClick) },
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), tint = AppTheme.colors.textPrimary, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = AppTheme.colors.surface.copy(alpha = 0.8f)
                )
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = AppTheme.colors.fail,
                        contentColor = Color.White
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 輸入區塊
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 第一排：開始日期 & 金額
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        JapaneseDateButton(stringResource(R.string.label_date_start), dateFormatter.format(Date(uiState.startDate))) {
                            debounce { startDatePickerDialog.show() }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            GlassTextField(
                                value = uiState.amount,
                                onValueChange = { viewModel.updateUiState(amount = it) },
                                label = stringResource(R.string.label_amount),
                                isNumber = true,
                                placeholder = ""
                            )
                        }
                    }

                    // 第二排：結束日期 & 頻率
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        JapaneseDateButton(
                            stringResource(R.string.label_date_end),
                            if (uiState.endDate != null) dateFormatter.format(Date(uiState.endDate!!)) else stringResource(R.string.label_date_infinite)
                        ) {
                            debounce { endDatePickerDialog.show() }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(periodsMap.toList()) { (key, resId) ->
                                GlassChip(
                                    label = stringResource(resId),
                                    selected = uiState.frequency == key,
                                    onClick = { viewModel.updateUiState(frequency = key) }
                                )
                            }
                        }
                    }

                    // 自訂天數 (當頻率為 Custom 時顯示)
                    if (uiState.frequency == "CUSTOM") {
                        GlassTextField(
                            value = uiState.customDays,
                            onValueChange = { viewModel.updateUiState(customDays = it) },
                            label = stringResource(R.string.label_interval_days),
                            isNumber = true,
                            placeholder = ""
                        )
                    }

                    HorizontalDivider(color = AppTheme.colors.divider, thickness = 1.dp)

                    // 分類選擇
                    Text(stringResource(R.string.label_category_name), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories, key = { it.id }) { category ->
                            GlassChip(
                                label = getSmartCategoryName(category.name),
                                selected = uiState.category == category.name,
                                icon = getIconByKey(category.iconKey),
                                onClick = { viewModel.updateUiState(category = category.name) }
                            )
                        }
                        item {
                            GlassIconButton(onClick = { debounce { showCategoryManager = true } }, size = 32.dp) {
                                Icon(Icons.Default.Add, stringResource(R.string.desc_edit_button), tint = AppTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // 訂閱名稱 (SubTags)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(subTags, key = { it.id }) { tag ->
                            val displayName = getSmartTagName(tag.name, tag.resourceKey)
                            GlassChip(
                                label = displayName,
                                selected = uiState.note == displayName,
                                onClick = { viewModel.updateUiState(note = displayName) }
                            )
                        }
                        item {
                            GlassIconButton(onClick = { debounce { showSubTagManager = true } }, size = 32.dp) {
                                Icon(Icons.Default.Add, stringResource(R.string.desc_edit_button), tint = AppTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // 名稱輸入框
                    GlassTextField(
                        value = getSmartTagName(uiState.note),
                        onValueChange = { viewModel.updateUiState(note = it) },
                        label = stringResource(R.string.hint_subscription_name),
                        placeholder = ""
                    )

                    // 儲存按鈕
                    AuroraPrimaryButton(
                        text = if (editId != -1L) "儲存變更" else stringResource(R.string.btn_add_to_list),
                        onClick = {
                            debounce {
                                // 呼叫 ViewModel 的儲存邏輯 (內部會判斷是新增還是更新)
                                viewModel.saveSubscription { onSaveSuccess() }
                            }
                        }
                    )
                }
            }

            // 列表區塊 (僅在新增模式下顯示，編輯模式下通常只需要專注於編輯，但顯示也無妨，這裡保留顯示)
            Text(stringResource(R.string.label_current_subscriptions), color = AppTheme.colors.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                // [修正] 這裡明確指定 items 的來源
                items(
                    items = list,
                    key = { it.id } // 確保 RecurringExpenseEntity 有 id 欄位
                ) { item ->

                    var itemModifier = Modifier.fillMaxWidth()
                    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            itemModifier = itemModifier.sharedElement(
                                state = rememberSharedContentState(key = "sub_${item.id}"),
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = FluidBoundsTransform
                            )
                        }
                    }

                    Box(modifier = itemModifier) {
                        JapaneseSubscriptionItem(
                            item = item,
                            frequencyLabel = stringResource(periodsMap[item.frequency] ?: R.string.freq_custom),
                            onClick = { debounce { onItemClick(item.id) } }
                        )
                    }
                }
            }
        }
    }
}

// ... 下方共用元件 ...

@Composable
fun JapaneseDateButton(label: String, value: String, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable { onClick() }) {
        Text(label, fontSize = 12.sp, color = AppTheme.colors.textSecondary)
        Text(value, fontSize = 14.sp, color = AppTheme.colors.textPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun JapaneseSubscriptionItem(
    item: RecurringExpenseEntity,
    frequencyLabel: String,
    onClick: () -> Unit // [修改] 傳入點擊 Callback
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick // [新增] 啟用卡片點擊
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = AppTheme.colors.background.copy(alpha = 0.5f), modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Star, null, tint = AppTheme.colors.accent, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(getSmartTagName(item.note), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppTheme.colors.textPrimary)
                    Text(
                        stringResource(R.string.format_subscription_price, item.amount, frequencyLabel),
                        fontSize = 12.sp,
                        color = AppTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}