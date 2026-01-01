package com.example.budgetquest.ui.transaction

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
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
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.common.AuroraPrimaryButton // [新增]
import com.example.budgetquest.ui.common.GlassCard // [新增]
import com.example.budgetquest.ui.common.GlassChip // [新增]
import com.example.budgetquest.ui.common.GlassIconButton // [新增]
import com.example.budgetquest.ui.common.GlassTextField // [新增]
import com.example.budgetquest.ui.common.getIconByKey
import com.example.budgetquest.ui.common.getSmartCategoryName
import com.example.budgetquest.ui.common.getSmartTagName
import com.example.budgetquest.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// [移除] getGlassBrush, getBorderBrush (已移至 common)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    initialDate: Long,
    expenseId: Long,
    onBackClick: () -> Unit,
    onSaveSuccess: (Long) -> Unit,
    viewModel: TransactionViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.visibleCategories.collectAsState()
    val tags by viewModel.visibleTags.collectAsState()

    var showCategoryManager by remember { mutableStateOf(false) }
    var showTagManager by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.errorMessageId) {
        uiState.errorMessageId?.let { errorId ->
            snackbarHostState.showSnackbar(
                message = context.getString(errorId),
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    var lastClickTime by remember { mutableLongStateOf(0L) }
    fun debounce(action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 500L) {
            lastClickTime = now
            action()
        }
    }

    if (showCategoryManager) {
        val allCategories by viewModel.allCategories.collectAsState()
        CategoryManagerDialog(
            categories = allCategories,
            onDismiss = { showCategoryManager = false },
            onAddCategory = viewModel::addCategory,
            onToggleVisibility = viewModel::toggleCategoryVisibility,
            onDelete = viewModel::deleteCategory
        )
    }

    if (showTagManager) {
        val allTags by viewModel.allTags.collectAsState()
        TagManagerDialog(
            tags = allTags,
            onDismiss = { showTagManager = false },
            onAddTag = viewModel::addTag,
            onToggleVisibility = viewModel::toggleTagVisibility,
            onDelete = viewModel::deleteTag
        )
    }


    val calendar = remember { Calendar.getInstance() }

    LaunchedEffect(uiState.date) {
        calendar.timeInMillis = uiState.date
    }

    val datePickerDialog = remember(context) {
        DatePickerDialog(
            context,
            { _, y, m, d ->
                calendar.set(y, m, d)
                viewModel.setDate(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    LaunchedEffect(uiState.date) {
        datePickerDialog.updateDate(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    val dateFormatString = stringResource(R.string.date_format_transaction)
    val dateFormatter = remember(dateFormatString) { SimpleDateFormat(dateFormatString, Locale.getDefault()) }

    LaunchedEffect(expenseId, initialDate) {
        if (expenseId != -1L) {
            viewModel.loadExpense(expenseId)
        } else {
            viewModel.reset()
            if (initialDate > 0) viewModel.setDate(initialDate)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (expenseId == -1L) stringResource(R.string.title_add_transaction)
                        else stringResource(R.string.title_edit_transaction),
                        color = AppTheme.colors.textPrimary,
                        fontSize = 18.sp
                    )
                },
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
            modifier = Modifier
                .padding(innerPadding)
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // [優化] 使用 GlassCard (日期與金額卡片)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.clickable { debounce { datePickerDialog.show() } },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, stringResource(R.string.action_select_date), tint = AppTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(dateFormatter.format(Date(uiState.date)), color = AppTheme.colors.textSecondary, fontSize = 14.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$", fontSize = 24.sp, color = AppTheme.colors.textSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))

                        // [優化] 使用 GlassTextField (需要特製大字體，這裡先用 GlassTextField 看看效果，如果需要透明背景大字體，可以自定義參數)
                        // 因為 GlassTextField 預設有邊框和背景，這裡可能不太適合放在 GlassCard 裡面 (會有雙重框)
                        // 如果您想要原來的 "透明輸入框" 效果，我們可以保留原有的 BasicTextField 邏輯，但為了統一性，
                        // 我們可以考慮讓 GlassTextField 支援透明模式，或者將其重構為更通用的 Input 元件。

                        // 考慮到這是金額輸入，通常需要醒目。我們可以保留原有的 JapaneseTransparentInput 邏輯，
                        // 但既然要重構，建議使用我們在 common 中定義的標準輸入框，讓其看起來像是一個明確的輸入區域。

                        GlassTextField(
                            value = uiState.amount,
                            onValueChange = { viewModel.updateAmount(it) },
                            placeholder = stringResource(R.string.hint_amount),
                            isNumber = true,
                            label = null
                        )
                    }
                }
            }

            // [優化] 使用 GlassCard (分類與備註卡片)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(stringResource(R.string.label_category), fontSize = 12.sp, color = AppTheme.colors.textSecondary)

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories, key = { it.id }) { cat ->
                            // [優化] 使用 GlassChip
                            GlassChip(
                                label = getSmartCategoryName(cat.name, cat.resourceKey),
                                selected = uiState.category == cat.name,
                                icon = getIconByKey(cat.iconKey),
                                onClick = { viewModel.updateCategory(cat.name) }
                            )
                        }
                        item {
                            // [優化] 使用 GlassIconButton (小尺寸)
                            GlassIconButton(onClick = { debounce { showCategoryManager = true } }, size = 32.dp) {
                                Icon(Icons.Default.Add, stringResource(R.string.desc_edit_button), tint = AppTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    HorizontalDivider(color = AppTheme.colors.divider, thickness = 1.dp)

                    Text(stringResource(R.string.label_note), fontSize = 12.sp, color = AppTheme.colors.textSecondary)

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tags, key = { it.id }) { tag ->
                            val displayName = getSmartTagName(tag.name, tag.resourceKey)
                            // [優化] 使用 GlassChip
                            GlassChip(
                                label = displayName,
                                selected = uiState.note == displayName,
                                onClick = { viewModel.updateNote(displayName) }
                            )
                        }
                        item {
                            GlassIconButton(onClick = { debounce { showTagManager = true } }, size = 32.dp) {
                                Icon(Icons.Default.Add, stringResource(R.string.desc_edit_button), tint = AppTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // [優化] 使用 GlassTextField
                    GlassTextField(
                        value = uiState.note,
                        onValueChange = { viewModel.updateNote(it) },
                        placeholder = stringResource(R.string.hint_note),
                        label = null
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // [優化] 使用 AuroraPrimaryButton
            AuroraPrimaryButton(
                text = stringResource(R.string.btn_save),
                onClick = {
                    debounce {
                        viewModel.saveExpense {
                            onSaveSuccess(uiState.date)
                        }
                    }
                }
            )
        }
    }
}

// [移除] JapaneseTransparentInput, JapaneseCompactChip, EditButton (已改用共通元件)