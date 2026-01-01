package com.example.budgetquest.ui.subscription

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton // 用於列表內的刪除按鈕
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.R
import com.example.budgetquest.data.RecurringExpenseEntity
import com.example.budgetquest.ui.AppViewModelProvider
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.budgetquest.ui.transaction.CategoryManagerDialog
import com.example.budgetquest.ui.transaction.SubTagManagerDialog
import com.example.budgetquest.ui.common.getIconByKey
import com.example.budgetquest.ui.common.getSmartCategoryName
import com.example.budgetquest.ui.common.getSmartTagName
import com.example.budgetquest.ui.common.AuroraPrimaryButton // [新增]
import com.example.budgetquest.ui.common.GlassCard // [新增]
import com.example.budgetquest.ui.common.GlassChip // [新增]
import com.example.budgetquest.ui.common.GlassIconButton // [新增]
import com.example.budgetquest.ui.common.GlassTextField // [新增]
import com.example.budgetquest.ui.theme.AppTheme

// [移除] 所有本地定義的樣式與元件 (getGlassBrush, getBorderBrush, GlassIconContainer, AuroraPrimaryButton, GlassSmallEditButton, JapaneseTextField, JapaneseCompactChip)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    planId: Int,
    startDate: Long,
    endDate: Long,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: SubscriptionViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState = viewModel.uiState
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

    LaunchedEffect(planId, startDate, endDate) {
        viewModel.initialize(planId, startDate, endDate)
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
                title = { Text(stringResource(R.string.title_fixed_expenses), color = AppTheme.colors.textPrimary, fontSize = 18.sp) },
                navigationIcon = {
                    // [優化] 使用 GlassIconButton
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
            modifier = Modifier.padding(innerPadding).padding(horizontal = 20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // [優化] 輸入區塊改用 GlassCard 封裝
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        JapaneseDateButton(stringResource(R.string.label_date_start), dateFormatter.format(Date(uiState.startDate))) {
                            debounce { startDatePickerDialog.show() }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            // [優化] 使用 GlassTextField
                            GlassTextField(
                                value = uiState.amount,
                                onValueChange = { viewModel.updateUiState(amount = it) },
                                label = stringResource(R.string.label_amount),
                                isNumber = true,
                                placeholder = ""
                            )
                        }
                    }
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
                                // [優化] 使用 GlassChip
                                GlassChip(
                                    label = stringResource(resId),
                                    selected = uiState.frequency == key,
                                    onClick = { viewModel.updateUiState(frequency = key) }
                                )
                            }
                        }
                    }
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
                        // [優化] 編輯按鈕：使用 GlassIconButton 的變體 (小尺寸 32dp)
                        item {
                            GlassIconButton(
                                onClick = { debounce { showCategoryManager = true } },
                                size = 32.dp
                            ) {
                                Icon(Icons.Default.Add, stringResource(R.string.desc_edit_button), tint = AppTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(subTags, key = { it.id }) { tag ->
                            val displayName = getSmartTagName(tag.name, tag.resourceKey)
                            GlassChip(
                                label = displayName,
                                selected = uiState.note == displayName,
                                onClick = { viewModel.updateUiState(note = displayName) }
                            )
                        }
                        // [優化] 編輯按鈕
                        item {
                            GlassIconButton(
                                onClick = { debounce { showSubTagManager = true } },
                                size = 32.dp
                            ) {
                                Icon(Icons.Default.Add, stringResource(R.string.desc_edit_button), tint = AppTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    GlassTextField(
                        value = getSmartTagName(uiState.note),
                        onValueChange = { viewModel.updateUiState(note = it) },
                        label = stringResource(R.string.hint_subscription_name),
                        placeholder = ""
                    )

                    // [優化] 使用 AuroraPrimaryButton
                    AuroraPrimaryButton(
                        text = stringResource(R.string.btn_add_to_list),
                        onClick = {
                            debounce {
                                viewModel.addSubscription { onSaveSuccess() }
                            }
                        }
                    )
                }
            }

            Text(stringResource(R.string.label_current_subscriptions), color = AppTheme.colors.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(list, key = { it.id }) { item ->
                    JapaneseSubscriptionItem(
                        item,
                        frequencyLabel = stringResource(periodsMap[item.frequency] ?: R.string.freq_custom),
                        onDelete = {
                            debounce { viewModel.deleteSubscription(item) }
                        }
                    )
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

// [移除] JapaneseTextField (已改用 GlassTextField)
// [移除] JapaneseCompactChip (已改用 GlassChip)

@Composable
fun JapaneseSubscriptionItem(
    item: RecurringExpenseEntity,
    frequencyLabel: String,
    onDelete: () -> Unit
) {
    // [優化] 列表項目改用 GlassCard 封裝
    GlassCard(modifier = Modifier.fillMaxWidth()) {
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
            // 這裡的刪除按鈕如果是單純 Icon，可以保留 IconButton，或者也改用 GlassIconButton(size=32.dp)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, stringResource(R.string.content_desc_delete), tint = AppTheme.colors.textSecondary.copy(alpha = 0.5f))
            }
        }
    }
}