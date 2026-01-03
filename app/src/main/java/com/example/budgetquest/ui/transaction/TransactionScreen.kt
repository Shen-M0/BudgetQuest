package com.example.budgetquest.ui.transaction

import android.app.Activity
import android.app.DatePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.budgetquest.R
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.common.AuroraPrimaryButton
import com.example.budgetquest.ui.common.GlassCard
import com.example.budgetquest.ui.common.GlassChip
import com.example.budgetquest.ui.common.GlassIconButton
import com.example.budgetquest.ui.common.GlassTextField
import com.example.budgetquest.ui.common.ImageUtils
import com.example.budgetquest.ui.common.getIconByKey
import com.example.budgetquest.ui.common.getSmartCategoryName
import com.example.budgetquest.ui.common.getSmartTagName
import com.example.budgetquest.ui.theme.AppTheme
import com.google.android.libraries.places.api.Places
// [修正 1] 將 Place 取別名為 GooglePlace，避免與圖標 Icons.Default.Place 衝突
import com.google.android.libraries.places.api.model.Place as GooglePlace
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    // 進階選項狀態
    var isAdvancedExpanded by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current


    // [新增] 觀察支付方式列表
    val paymentMethods by viewModel.visiblePaymentMethods.collectAsState()
    var showPaymentMethodManager by remember { mutableStateOf(false) }

    if (showPaymentMethodManager) {
        val allPaymentMethods by viewModel.allPaymentMethods.collectAsState()
        PaymentMethodManagerDialog(
            paymentMethods = allPaymentMethods,
            onDismiss = { showPaymentMethodManager = false },
            onAdd = viewModel::addPaymentMethod,
            onToggleVisibility = viewModel::togglePaymentMethodVisibility,
            onDelete = viewModel::deletePaymentMethod
        )
    }

    // [新增] 初始化 Google Places SDK
    // 注意：請將 "YOUR_API_KEY_HERE" 替換為您真實的 Google Cloud API Key
    LaunchedEffect(Unit) {
        if (!Places.isInitialized()) {
            // TODO: 請填入您的 Google Places API Key
            Places.initialize(context.applicationContext, "YOUR_API_KEY_HERE")
        }
    }

    // [新增] 地點搜尋啟動器 (Places Autocomplete)
    val placeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                // 使用 Autocomplete 解析資料
                val place = Autocomplete.getPlaceFromIntent(intent)
                val locationName = place.name ?: place.address
                if (locationName != null) {
                    viewModel.updateMerchant(locationName)
                }
            }
        } else if (result.resultCode == AutocompleteActivity.RESULT_ERROR) {
            result.data?.let { intent ->
                val status = Autocomplete.getStatusFromIntent(intent)
                Toast.makeText(context, "搜尋錯誤: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 圖片選擇相關狀態
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // 相簿啟動器
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val internalPath = ImageUtils.copyImageToInternalStorage(context, uri)
            viewModel.updateImageUri(internalPath)
        }
    }

    // 相機啟動器
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            val internalPath = ImageUtils.copyImageToInternalStorage(context, tempCameraUri!!)
            viewModel.updateImageUri(internalPath)
        }
    }

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

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("選擇圖片來源") },
            text = { Text("請選擇要從相簿選取還是開啟相機拍攝。") },
            confirmButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Text("相簿")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    val uri = ImageUtils.createTempPictureUri(context)
                    tempCameraUri = uri
                    cameraLauncher.launch(uri)
                }) {
                    Text("相機")
                }
            },
            containerColor = AppTheme.colors.surface,
            titleContentColor = AppTheme.colors.textPrimary,
            textContentColor = AppTheme.colors.textSecondary
        )
    }

    val calendar = remember { Calendar.getInstance() }
    LaunchedEffect(uiState.date) { calendar.timeInMillis = uiState.date }

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
        datePickerDialog.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
    }

    val dateFormatString = stringResource(R.string.date_format_transaction)
    val dateFormatter = remember(dateFormatString) { SimpleDateFormat(dateFormatString, Locale.getDefault()) }

    LaunchedEffect(expenseId, initialDate) {
        if (expenseId != -1L) {
            viewModel.loadExpense(expenseId)
            isAdvancedExpanded = true
        } else {
            viewModel.reset()
            if (initialDate > 0) viewModel.setDate(initialDate)
            isAdvancedExpanded = false
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(0.dp))

            // 1. 日期與金額卡片
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

            // 2. 分類與備註卡片
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(stringResource(R.string.label_category), fontSize = 12.sp, color = AppTheme.colors.textSecondary)

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories, key = { it.id }) { cat ->
                            GlassChip(
                                label = getSmartCategoryName(cat.name, cat.resourceKey),
                                selected = uiState.category == cat.name,
                                icon = getIconByKey(cat.iconKey),
                                onClick = { viewModel.updateCategory(cat.name) }
                            )
                        }
                        item {
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

                    GlassTextField(
                        value = uiState.note,
                        onValueChange = { viewModel.updateNote(it) },
                        placeholder = stringResource(R.string.hint_note),
                        label = null
                    )
                }
            }

            // 3. 進階選項區塊
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAdvancedExpanded = !isAdvancedExpanded }
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "進階選項",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary
                        )
                        Icon(
                            imageVector = if (isAdvancedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = AppTheme.colors.textSecondary
                        )
                    }

                    AnimatedVisibility(
                        visible = isAdvancedExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // A. 圖片上傳/拍攝
                            Text("照片 / 收據", fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AppTheme.colors.background.copy(alpha = 0.3f))
                                    .clickable { showImageSourceDialog = true }
                                    .border(1.dp, AppTheme.colors.textSecondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.imageUri.isNullOrEmpty()) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.AddPhotoAlternate, null, tint = AppTheme.colors.textSecondary, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("點擊拍攝或選取照片", fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                                    }
                                } else {
                                    val file = File(uiState.imageUri!!)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)) // 深色背景，檢視器風格
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(file)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Selected Image",
                                            contentScale = ContentScale.Fit, // 使用 Fit 確保完整顯示
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // 移除按鈕
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .size(28.dp)
                                                .background(AppTheme.colors.background.copy(alpha = 0.8f), CircleShape)
                                                .clickable { viewModel.updateImageUri(null) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                null,
                                                tint = AppTheme.colors.textPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = AppTheme.colors.divider, thickness = 1.dp)

                            // B. 支付方式
                            Text("支付方式", fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                            // [修正] 使用動態列表
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(paymentMethods, key = { it.id }) { method ->
                                    GlassChip(
                                        label = method.name,
                                        selected = uiState.paymentMethod == method.name,
                                        onClick = { viewModel.updatePaymentMethod(method.name) }
                                    )
                                }
                                // [新增] 管理按鈕
                                item {
                                    GlassIconButton(
                                        onClick = { debounce { showPaymentMethodManager = true } },
                                        size = 32.dp
                                    ) {
                                        Icon(Icons.Default.Add, "Manage", tint = AppTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // C. 店家/地點 [修正 2] 使用 GooglePlace.Field 來避免衝突
                            Text("店家 / 地點", fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GlassTextField(
                                    value = uiState.merchant,
                                    onValueChange = { viewModel.updateMerchant(it) },
                                    placeholder = "例如：7-11, 星巴克...",
                                    label = null,
                                    modifier = Modifier.weight(1f) // 佔據大部分空間
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Google Maps 搜尋按鈕
                                GlassIconButton(
                                    onClick = {
                                        debounce {
                                            // [修正 2] 這裡使用 GooglePlace.Field，因為上面已經取了別名
                                            val fields = listOf(GooglePlace.Field.NAME, GooglePlace.Field.ADDRESS)
                                            // 啟動 Autocomplete Intent
                                            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                                                .build(context)
                                            placeLauncher.launch(intent)
                                        }
                                    },
                                    size = 48.dp // 與 TextField 高度接近
                                ) {
                                    Icon(Icons.Default.Place, contentDescription = "Search Location", tint = AppTheme.colors.accent)
                                }
                            }

                            HorizontalDivider(color = AppTheme.colors.divider, thickness = 1.dp)

                            // D. 不計入預算
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("不計入預算", fontSize = 14.sp, color = AppTheme.colors.textPrimary)
                                    Text("開啟後不會扣除今日可用額度", fontSize = 11.sp, color = AppTheme.colors.textSecondary)
                                }
                                Switch(
                                    checked = uiState.excludeFromBudget,
                                    onCheckedChange = { viewModel.updateExcludeFromBudget(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = AppTheme.colors.accent,
                                        checkedThumbColor = Color.White,
                                        uncheckedTrackColor = AppTheme.colors.background.copy(alpha = 0.5f),
                                        uncheckedBorderColor = Color.Transparent
                                    )
                                )
                            }

                            // E. Need vs Want
                            Text("消費性質", fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // [修正] 判斷是否選中 (uiState.isNeed == true)，並使用 toggle
                                GlassChip(
                                    label = "需要 (Need)",
                                    selected = uiState.isNeed == true,
                                    icon = if (uiState.isNeed == true) Icons.Default.Check else null,
                                    onClick = { viewModel.toggleNeedStatus(true) } // 使用 toggle
                                )
                                // [修正] 判斷是否選中 (uiState.isNeed == false)
                                GlassChip(
                                    label = "想要 (Want)",
                                    selected = uiState.isNeed == false,
                                    icon = if (uiState.isNeed == false) Icons.Default.Check else null,
                                    onClick = { viewModel.toggleNeedStatus(false) } // 使用 toggle
                                )
                            }

                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

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

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}