package com.example.budgetquest.ui.subscription

import android.app.Activity
import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
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
import com.example.budgetquest.data.RecurringExpenseEntity
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.common.AuroraPrimaryButton
import com.example.budgetquest.ui.common.FluidBoundsTransform
import com.example.budgetquest.ui.common.GlassCard
import com.example.budgetquest.ui.common.GlassChip
import com.example.budgetquest.ui.common.GlassIconButton
import com.example.budgetquest.ui.common.GlassTextField
import com.example.budgetquest.ui.common.ImageUtils
import com.example.budgetquest.ui.common.getIconByKey
import com.example.budgetquest.ui.common.getSmartCategoryName
import com.example.budgetquest.ui.common.getSmartTagName
import com.example.budgetquest.ui.theme.AppTheme
import com.example.budgetquest.ui.transaction.CategoryManagerDialog
import com.example.budgetquest.ui.transaction.PaymentMethodManagerDialog
import com.example.budgetquest.ui.transaction.SubTagManagerDialog
import com.example.budgetquest.ui.settings.CurrencySelectionDialog // [新增] 引用設定頁的 Dialog
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import java.io.File
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
    val list by viewModel.recurringList.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // [新增] 幣別選擇 Dialog 狀態
    var showCurrencyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!Places.isInitialized()) {
            // Places.initialize(context.applicationContext, "YOUR_API_KEY")
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

    // 圖片與地點相關邏輯 (保持不變)
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            val internalPath = ImageUtils.copyImageToInternalStorage(context, uri)
            viewModel.updateImageUri(internalPath)
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { success ->
        if (success && tempCameraUri != null) {
            val internalPath = ImageUtils.copyImageToInternalStorage(context, tempCameraUri!!)
            viewModel.updateImageUri(internalPath)
        }
    }
    val placeLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                val place = Autocomplete.getPlaceFromIntent(intent)
                val locationName = place.name ?: place.address
                if (locationName != null) viewModel.updateUiState(merchant = locationName)
            }
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

    LaunchedEffect(planId, startDate, endDate, editId) {
        viewModel.initialize(planId, startDate, endDate)
        if (editId != -1L) viewModel.loadForEditing(editId)
    }

    val startCalendar = remember { Calendar.getInstance() }
    val endCalendar = remember { Calendar.getInstance() }
    val dateFormatStr = stringResource(R.string.format_date_standard)
    val dateFormatter = remember(dateFormatStr) { SimpleDateFormat(dateFormatStr, Locale.getDefault()) }

    val startDatePickerDialog = remember(context) {
        DatePickerDialog(context, { _, y, m, d ->
            startCalendar.set(y, m, d)
            viewModel.updateUiState(startDate = startCalendar.timeInMillis)
        }, startCalendar.get(Calendar.YEAR), startCalendar.get(Calendar.MONTH), startCalendar.get(Calendar.DAY_OF_MONTH))
    }
    LaunchedEffect(uiState.startDate) { startCalendar.timeInMillis = uiState.startDate; startDatePickerDialog.updateDate(startCalendar.get(Calendar.YEAR), startCalendar.get(Calendar.MONTH), startCalendar.get(Calendar.DAY_OF_MONTH)) }

    val endDatePickerDialog = remember(context) {
        DatePickerDialog(context, { _, y, m, d ->
            endCalendar.set(y, m, d)
            viewModel.updateUiState(endDate = endCalendar.timeInMillis)
        }, endCalendar.get(Calendar.YEAR), endCalendar.get(Calendar.MONTH), endCalendar.get(Calendar.DAY_OF_MONTH))
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
    var isAdvancedExpanded by remember { mutableStateOf(false) }

    // Dialogs
    if (showCategoryManager) {
        val allCategories by viewModel.allCategories.collectAsState()
        CategoryManagerDialog(allCategories, { showCategoryManager = false }, viewModel::addCategory, viewModel::toggleCategoryVisibility, viewModel::deleteCategory)
    }
    if (showSubTagManager) {
        val allSubTags by viewModel.allSubTags.collectAsState()
        SubTagManagerDialog(allSubTags, { showSubTagManager = false }, viewModel::addSubTag, viewModel::toggleSubTagVisibility, viewModel::deleteSubTag)
    }
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text(stringResource(R.string.dialog_image_source_title)) },
            text = { Text(stringResource(R.string.dialog_image_source_msg)) },
            confirmButton = { TextButton(onClick = { showImageSourceDialog = false; galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text(stringResource(R.string.source_gallery)) } },
            dismissButton = { TextButton(onClick = { showImageSourceDialog = false; val uri = ImageUtils.createTempPictureUri(context); tempCameraUri = uri; cameraLauncher.launch(uri) }) { Text(stringResource(R.string.source_camera)) } },
            containerColor = AppTheme.colors.surface
        )
    }

    // 觀察支付方式列表
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

    // [新增] 幣別選擇 Dialog
    if (showCurrencyDialog) {
        CurrencySelectionDialog(
            currentSelection = viewModel.inputCurrency,
            currencyList = viewModel.supportedCurrencies,
            onDismiss = { showCurrencyDialog = false },
            onConfirm = { selected ->
                viewModel.updateInputCurrency(selected)
                showCurrencyDialog = false
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    val title = if (editId != -1L) stringResource(R.string.title_edit_subscription) else stringResource(R.string.title_new_subscription)
                    Text(title, color = AppTheme.colors.textPrimary, fontSize = 18.sp)
                },
                navigationIcon = {
                    GlassIconButton(onClick = { debounce(onBackClick) }, modifier = Modifier.padding(start = 12.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), tint = AppTheme.colors.textPrimary, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = AppTheme.colors.surface.copy(alpha = 0.8f))
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState, snackbar = { data -> Snackbar(snackbarData = data, containerColor = AppTheme.colors.fail, contentColor = Color.White) }) }
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // 區塊 1: 日期與金額 (同一個 Card)
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                        // [修改] 讓日期和金額並排，並整合匯率
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically // 垂直置中
                        ) {
                            GlassDateButton(
                                label = stringResource(R.string.label_date_start),
                                value = dateFormatter.format(Date(uiState.startDate)),
                                onClick = { debounce { startDatePickerDialog.show() } }
                            )

                            // [修改] 金額輸入區塊
                            Box(modifier = Modifier.weight(1f)) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // 幣別按鈕
                                        Surface(
                                            onClick = { showCurrencyDialog = true },
                                            shape = RoundedCornerShape(8.dp),
                                            color = AppTheme.colors.surface.copy(alpha = 0.5f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.divider),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = viewModel.inputCurrency,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AppTheme.colors.textPrimary
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    tint = AppTheme.colors.textSecondary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }

                                        // 輸入框
                                        GlassTextField(
                                            value = uiState.amount,
                                            onValueChange = { viewModel.updateUiState(amount = it) },
                                            label = stringResource(R.string.label_amount),
                                            isNumber = true,
                                            placeholder = "",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // 轉換預覽
                                    if (viewModel.convertedPreview.isNotEmpty()) {
                                        Text(
                                            text = viewModel.convertedPreview,
                                            color = AppTheme.colors.accent,
                                            fontSize = 12.sp,
                                            modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GlassDateButton(
                                label = stringResource(R.string.label_date_end),
                                value = if (uiState.endDate != null) dateFormatter.format(Date(uiState.endDate!!)) else stringResource(R.string.label_date_infinite),
                                onClick = { debounce { endDatePickerDialog.show() } }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(periodsMap.toList()) { (key, resId) ->
                                    GlassChip(label = stringResource(resId), selected = uiState.frequency == key, onClick = { viewModel.updateUiState(frequency = key) })
                                }
                            }
                        }
                        if (uiState.frequency == "CUSTOM") {
                            GlassTextField(value = uiState.customDays, onValueChange = { viewModel.updateUiState(customDays = it) }, label = stringResource(R.string.label_interval_days), isNumber = true, placeholder = "")
                        }
                    }
                }
            }

            // 區塊 2: 分類、備註、名稱 (同一個 Card)
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(R.string.label_category_name), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories, key = { it.id }) { category ->
                                GlassChip(label = getSmartCategoryName(category.name), selected = uiState.category == category.name, icon = getIconByKey(category.iconKey), onClick = { viewModel.updateUiState(category = category.name) })
                            }
                            item { GlassIconButton(onClick = { debounce { showCategoryManager = true } }, size = 32.dp) { Icon(Icons.Default.Add, stringResource(R.string.desc_edit_button), tint = AppTheme.colors.textSecondary, modifier = Modifier.size(16.dp)) } }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(subTags, key = { it.id }) { tag ->
                                val displayName = getSmartTagName(tag.name, tag.resourceKey)
                                GlassChip(label = displayName, selected = uiState.note == displayName, onClick = { viewModel.updateUiState(note = displayName) })
                            }
                            item { GlassIconButton(onClick = { debounce { showSubTagManager = true } }, size = 32.dp) { Icon(Icons.Default.Add, stringResource(R.string.desc_edit_button), tint = AppTheme.colors.textSecondary, modifier = Modifier.size(16.dp)) } }
                        }

                        GlassTextField(value = getSmartTagName(uiState.note), onValueChange = { viewModel.updateUiState(note = it) }, label = stringResource(R.string.hint_subscription_name), placeholder = "")
                    }
                }
            }

            // 區塊 3: 進階選項 (獨立 Card)
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { isAdvancedExpanded = !isAdvancedExpanded }.padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.title_advanced_options), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                            Icon(imageVector = if (isAdvancedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = AppTheme.colors.textSecondary)
                        }

                        AnimatedVisibility(visible = isAdvancedExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 16.dp)) {
                                // A. 圖片
                                Text(stringResource(R.string.label_photo_receipt), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth().height(150.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AppTheme.colors.background.copy(alpha = 0.3f))
                                        .clickable { showImageSourceDialog = true }
                                        .border(1.dp, AppTheme.colors.textSecondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.imageUri.isNullOrEmpty()) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.AddPhotoAlternate, null, tint = AppTheme.colors.textSecondary, modifier = Modifier.size(24.dp))
                                            Text(stringResource(R.string.hint_click_add_photo), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                                        }
                                    } else {
                                        val file = File(uiState.imageUri!!)
                                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))) {
                                            AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(file).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(24.dp).background(AppTheme.colors.background.copy(alpha = 0.8f), CircleShape).clickable { viewModel.updateImageUri(null) }, contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Close, null, tint = AppTheme.colors.textPrimary, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }

                                // B. 支付方式
                                Text(stringResource(R.string.label_payment_method), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(paymentMethods, key = { it.id }) { method ->
                                        GlassChip(
                                            label = method.name,
                                            selected = uiState.paymentMethod == method.name,
                                            onClick = { viewModel.updateUiState(paymentMethod = method.name) }
                                        )
                                    }
                                    item {
                                        GlassIconButton(
                                            onClick = { debounce { showPaymentMethodManager = true } },
                                            size = 32.dp
                                        ) {
                                            Icon(Icons.Default.Add, "Manage", tint = AppTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                // C. 店家/地點
                                Text(stringResource(R.string.label_merchant_location), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    GlassTextField(value = uiState.merchant, onValueChange = { viewModel.updateUiState(merchant = it) }, placeholder = stringResource(R.string.hint_merchant_example), label = null, modifier = Modifier.weight(1f))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    GlassIconButton(onClick = { debounce {
                                        val fields = listOf(Place.Field.NAME, Place.Field.ADDRESS)
                                        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(context)
                                        placeLauncher.launch(intent)
                                    } }, size = 48.dp) {
                                        Icon(Icons.Default.Place, contentDescription = "Search Location", tint = AppTheme.colors.accent)
                                    }
                                }

                                // D. 其他開關
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column { Text(stringResource(R.string.label_exclude_budget), fontSize = 14.sp, color = AppTheme.colors.textPrimary); Text(stringResource(R.string.hint_exclude_budget), fontSize = 11.sp, color = AppTheme.colors.textSecondary) }
                                    Switch(checked = uiState.excludeFromBudget, onCheckedChange = { viewModel.updateUiState(excludeFromBudget = it) }, colors = SwitchDefaults.colors(checkedTrackColor = AppTheme.colors.accent, checkedThumbColor = Color.White, uncheckedTrackColor = AppTheme.colors.background.copy(alpha = 0.5f), uncheckedBorderColor = Color.Transparent))
                                }
                                Text(stringResource(R.string.label_consumption_nature), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GlassChip(
                                        label = stringResource(R.string.label_need_full),
                                        selected = uiState.isNeed == true,
                                        icon = if (uiState.isNeed == true) Icons.Default.Check else null,
                                        onClick = { viewModel.toggleNeedStatus(true) }
                                    )
                                    GlassChip(
                                        label = stringResource(R.string.label_want_full),
                                        selected = uiState.isNeed == false,
                                        icon = if (uiState.isNeed == false) Icons.Default.Check else null,
                                        onClick = { viewModel.toggleNeedStatus(false) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 區塊 4: 儲存按鈕
            item {
                AuroraPrimaryButton(
                    text = if (editId != -1L) stringResource(R.string.btn_save_changes) else stringResource(R.string.btn_add_to_list),
                    onClick = { debounce { viewModel.saveSubscription { onSaveSuccess() } } }
                )
            }

            if (editId == -1L) {
                item {
                    Text(stringResource(R.string.title_subscription_list), color = AppTheme.colors.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
                }

                items(items = list, key = { it.id }) { item ->
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
                        SubscriptionItem(
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

// ... (GlassDateButton & SubscriptionItem)
@Composable
fun GlassDateButton(label: String, value: String, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.clickable { onClick() }, cornerRadius = 16.dp) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(label, fontSize = 12.sp, color = AppTheme.colors.textSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 14.sp, color = AppTheme.colors.textPrimary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SubscriptionItem(item: RecurringExpenseEntity, frequencyLabel: String, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = AppTheme.colors.background.copy(alpha = 0.5f), modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Star, null, tint = AppTheme.colors.accent, modifier = Modifier.size(24.dp)) }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(getSmartTagName(item.note), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppTheme.colors.textPrimary)
                    Text(stringResource(R.string.format_subscription_price, item.amount, frequencyLabel), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                }
            }
        }
    }
}