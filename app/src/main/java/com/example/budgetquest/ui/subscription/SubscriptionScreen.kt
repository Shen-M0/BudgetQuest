package com.example.budgetquest.ui.subscription

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
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
import com.example.budgetquest.ui.theme.AppTheme

// [美術] 定義玻璃筆刷
@Composable
private fun getGlassBrush(): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            AppTheme.colors.surface.copy(alpha = 0.65f),
            AppTheme.colors.surface.copy(alpha = 0.35f)
        )
    )
}

@Composable
private fun getBorderBrush(): Brush {
    return Brush.linearGradient(
        colors = listOf(
            AppTheme.colors.textPrimary.copy(alpha = 0.25f),
            AppTheme.colors.textPrimary.copy(alpha = 0.10f)
        )
    )
}

// [美術] 1. 玻璃圓形按鈕容器 (TopBar 用)
@Composable
fun GlassIconContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val glassBrush = getGlassBrush()
    val borderBrush = getBorderBrush()

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(glassBrush)
            .border(1.dp, borderBrush, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// [美術] 2. 極光漸層主要按鈕 (用於加入清單)
@Composable
fun AuroraPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            AppTheme.colors.accent,
            AppTheme.colors.accent.copy(alpha = 0.7f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = AppTheme.colors.accent, spotColor = AppTheme.colors.accent)
            .clip(RoundedCornerShape(16.dp))
            .background(gradientBrush)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// [美術] 3. 玻璃編輯按鈕 (小圓鈕)
@Composable
fun GlassSmallEditButton(onClick: () -> Unit) {
    val glassBrush = getGlassBrush()
    val borderBrush = getBorderBrush()

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(glassBrush)
            .border(1.dp, borderBrush, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.desc_edit_button),
            tint = AppTheme.colors.textSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

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

    // [美術] 取得筆刷
    val glassBrush = getGlassBrush()
    val borderBrush = getBorderBrush()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_fixed_expenses), color = AppTheme.colors.textPrimary, fontSize = 18.sp) },
                navigationIcon = {
                    // [美術] 優化：使用玻璃圓鈕
                    Box(modifier = Modifier.padding(start = 12.dp).clip(CircleShape).clickable { debounce(onBackClick) }) {
                        GlassIconContainer(modifier = Modifier.size(40.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), tint = AppTheme.colors.textPrimary, modifier = Modifier.size(20.dp))
                        }
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
            // [美術] 輸入區塊 (玻璃 Box)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(glassBrush)
                    .border(1.dp, borderBrush, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        JapaneseDateButton(stringResource(R.string.label_date_start), dateFormatter.format(Date(uiState.startDate))) {
                            debounce { startDatePickerDialog.show() }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            JapaneseTextField(value = uiState.amount, onValueChange = { viewModel.updateUiState(amount = it) }, label = stringResource(R.string.label_amount), isNumber = true)
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
                                JapaneseCompactChip(stringResource(resId), uiState.frequency == key) { viewModel.updateUiState(frequency = key) }
                            }
                        }
                    }
                    if (uiState.frequency == "CUSTOM") {
                        JapaneseTextField(value = uiState.customDays, onValueChange = { viewModel.updateUiState(customDays = it) }, label = stringResource(R.string.label_interval_days), isNumber = true)
                    }

                    HorizontalDivider(color = AppTheme.colors.divider, thickness = 1.dp)

                    Text(stringResource(R.string.label_category_name), fontSize = 12.sp, color = AppTheme.colors.textSecondary)

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories, key = { it.id }) { category ->
                            JapaneseCompactChip(
                                label = getSmartCategoryName(category.name),
                                selected = uiState.category == category.name,
                                icon = getIconByKey(category.iconKey)
                            ) { viewModel.updateUiState(category = category.name) }
                        }
                        // [美術] 優化：使用玻璃小圓鈕
                        item { GlassSmallEditButton { debounce { showCategoryManager = true } } }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(subTags, key = { it.id }) { tag ->
                            val displayName = getSmartTagName(tag.name, tag.resourceKey)
                            JapaneseCompactChip(
                                label = displayName,
                                selected = uiState.note == displayName
                            ) {
                                viewModel.updateUiState(note = displayName)
                            }
                        }
                        // [美術] 優化：使用玻璃小圓鈕
                        item { GlassSmallEditButton { debounce { showSubTagManager = true } } }
                    }

                    JapaneseTextField(
                        value = getSmartTagName(uiState.note),
                        onValueChange = { viewModel.updateUiState(note = it) },
                        label = stringResource(R.string.hint_subscription_name)
                    )

                    // [美術] 優化：使用極光漸層按鈕
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
fun JapaneseTextField(value: String, onValueChange: (String) -> Unit, label: String, isNumber: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = AppTheme.colors.textSecondary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = AppTheme.colors.accent,
            unfocusedContainerColor = AppTheme.colors.background.copy(alpha = 0.5f),
            focusedContainerColor = AppTheme.colors.background.copy(alpha = 0.7f),
            focusedTextColor = AppTheme.colors.textPrimary,
            unfocusedTextColor = AppTheme.colors.textPrimary
        ),
        keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        singleLine = true
    )
}

@Composable
fun JapaneseDateButton(label: String, value: String, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable { onClick() }) {
        Text(label, fontSize = 12.sp, color = AppTheme.colors.textSecondary)
        Text(value, fontSize = 14.sp, color = AppTheme.colors.textPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun JapaneseCompactChip(label: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    Surface(
        color = if (selected) AppTheme.colors.accent else AppTheme.colors.background.copy(alpha = 0.5f),
        contentColor = if (selected) Color.White else AppTheme.colors.textSecondary,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            if (icon != null) {
                Icon(icon, null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(label, fontSize = 12.sp)
        }
    }
}

@Composable
fun JapaneseSubscriptionItem(
    item: RecurringExpenseEntity,
    frequencyLabel: String,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppTheme.colors.surface.copy(alpha = 0.7f))
            .padding(20.dp),
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
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, stringResource(R.string.content_desc_delete), tint = AppTheme.colors.textSecondary.copy(alpha = 0.5f))
        }
    }
}