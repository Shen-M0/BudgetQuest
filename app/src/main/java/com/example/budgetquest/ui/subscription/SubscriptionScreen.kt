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

    // 圖片與地點相關邏輯
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
            title = { Text("選擇圖片來源") },
            text = { Text("請選擇要從相簿選取還是開啟相機拍攝。") },
            confirmButton = { TextButton(onClick = { showImageSourceDialog = false; galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("相簿") } },
            dismissButton = { TextButton(onClick = { showImageSourceDialog = false; val uri = ImageUtils.createTempPictureUri(context); tempCameraUri = uri; cameraLauncher.launch(uri) }) { Text("相機") } },
            containerColor = AppTheme.colors.surface
        )
    }

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


    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    val title = if (editId != -1L) "編輯固定支出" else "固定支出"
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

        // [關鍵修改] 使用單一 LazyColumn 取代原本的 Column + LazyColumn
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
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlassDateButton(
                                label = stringResource(R.string.label_date_start),
                                value = dateFormatter.format(Date(uiState.startDate)),
                                onClick = { debounce { startDatePickerDialog.show() } }
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                GlassTextField(value = uiState.amount, onValueChange = { viewModel.updateUiState(amount = it) }, label = stringResource(R.string.label_amount), isNumber = true, placeholder = "")
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
                            Text("進階選項", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                            Icon(imageVector = if (isAdvancedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = AppTheme.colors.textSecondary)
                        }

                        AnimatedVisibility(visible = isAdvancedExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 16.dp)) {
                                // A. 圖片
                                Text("照片 / 收據", fontSize = 12.sp, color = AppTheme.colors.textSecondary)
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
                                            Text("點擊附加照片", fontSize = 12.sp, color = AppTheme.colors.textSecondary)
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
                                Text("支付方式", fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                                // [修正] 使用動態列表
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(paymentMethods, key = { it.id }) { method ->
                                        GlassChip(
                                            label = method.name,
                                            selected = uiState.paymentMethod == method.name,
                                            onClick = { viewModel.updateUiState(paymentMethod = method.name) } // ViewModel 內已實作 Toggle
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

                                // C. 店家/地點
                                Text("店家 / 地點", fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    GlassTextField(value = uiState.merchant, onValueChange = { viewModel.updateUiState(merchant = it) }, placeholder = "例如：Netflix, Spotify...", label = null, modifier = Modifier.weight(1f))
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
                                    Column { Text("不計入預算", fontSize = 14.sp, color = AppTheme.colors.textPrimary); Text("開啟後不會扣除今日額度", fontSize = 11.sp, color = AppTheme.colors.textSecondary) }
                                    Switch(checked = uiState.excludeFromBudget, onCheckedChange = { viewModel.updateUiState(excludeFromBudget = it) }, colors = SwitchDefaults.colors(checkedTrackColor = AppTheme.colors.accent, checkedThumbColor = Color.White, uncheckedTrackColor = AppTheme.colors.background.copy(alpha = 0.5f), uncheckedBorderColor = Color.Transparent))
                                }
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
            }

            // 區塊 4: 儲存按鈕
            item {
                AuroraPrimaryButton(
                    text = if (editId != -1L) "儲存變更" else stringResource(R.string.btn_add_to_list),
                    onClick = { debounce { viewModel.saveSubscription { onSaveSuccess() } } }
                )
            }

            // [修改] 只有在「非編輯模式 (editId == -1)」時，才顯示下方的列表
            if (editId == -1L) {
                // 區塊 5: 標題與列表
                item {
                    Text("固定支出項目", color = AppTheme.colors.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
                }

                items(items = list, key = { it.id }) { item ->
                    // ... (列表內容保持不變) ...
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

// ... 下方的 GlassDateButton 與 SubscriptionItem 保持不變 (請保留上一次回答中的實作) ...
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