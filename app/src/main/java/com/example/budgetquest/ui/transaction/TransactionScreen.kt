package com.example.budgetquest.ui.transaction

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.budgetquest.BuildConfig
import com.example.budgetquest.R
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.common.AuroraPrimaryButton
import com.example.budgetquest.ui.common.GlassCard
import com.example.budgetquest.ui.common.GlassChip
import com.example.budgetquest.ui.common.GlassIconButton
import com.example.budgetquest.ui.common.GlassSwitch
import com.example.budgetquest.ui.common.GlassTextField
import com.example.budgetquest.ui.common.ImageUtils
import com.example.budgetquest.ui.common.getIconByKey
import com.example.budgetquest.ui.common.getSmartCategoryName
import com.example.budgetquest.ui.common.getSmartPaymentName
import com.example.budgetquest.ui.common.getSmartTagName
import com.example.budgetquest.ui.theme.AppTheme
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.google.android.libraries.places.api.model.Place as GooglePlace

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

    var isAdvancedExpanded by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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

    // Google Places 初始化
    LaunchedEffect(Unit) {
        if (!Places.isInitialized()) {
            val apiKey = BuildConfig.GOOGLE_MAPS_API_KEY
            if (apiKey.isNotBlank()) {
                try {
                    Places.initialize(context.applicationContext, apiKey)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                android.util.Log.e("BudgetQuest", "API Key is empty! Please check local.properties")
            }
        }
    }

    // [新增] FusedLocationProviderClient 用於獲取位置
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // [新增] 邏輯：獲取當前位置並轉為地址
    fun getCurrentLocation() {
        try {
            // 再次檢查權限 (雖然在呼叫前會檢查，但 IDE 還是會警告)
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(context, context.getString(R.string.msg_locating), Toast.LENGTH_SHORT).show()

                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val geocoder = Geocoder(context, Locale.getDefault())
                                // 取得 1 筆地址結果
                                @Suppress("DEPRECATION") // 為了相容性使用舊 API，新版 API 需 Tiramisu 以上
                                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                                if (!addresses.isNullOrEmpty()) {
                                    val address = addresses[0]
                                    // 優先使用地標名稱 (FeatureName)，如果沒有則使用地址線
                                    // 為了更精確顯示店家，通常 geocoder 只會給出地址，
                                    // 若要精確店家通常需要 Places API 的 Current Place，但 Geocoder 比較省錢且簡單
                                    val resultName = address.getAddressLine(0) // 完整地址

                                    withContext(Dispatchers.Main) {
                                        viewModel.updateMerchant(resultName)
                                        Toast.makeText(context, context.getString(R.string.msg_location_found), Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, context.getString(R.string.error_location_not_found), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, context.getString(R.string.error_geocoder_failed), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.error_location_unavailable), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // [新增] 權限請求 Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (isGranted) {
            getCurrentLocation()
        } else {
            Toast.makeText(context, context.getString(R.string.error_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    val errorSearchMessage = stringResource(R.string.error_search_failed)

    // Places Autocomplete Launcher
    val placeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { intent ->
                val place = Autocomplete.getPlaceFromIntent(intent)
                val locationName = place.name ?: place.address
                if (locationName != null) {
                    viewModel.updateMerchant(locationName)
                }
            }
        } else if (result.resultCode == AutocompleteActivity.RESULT_ERROR) {
            result.data?.let { intent ->
                val status = Autocomplete.getStatusFromIntent(intent)
                Toast.makeText(context, "$errorSearchMessage: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 圖片相關
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val internalPath = ImageUtils.copyImageToInternalStorage(context, uri)
            viewModel.updateImageUri(internalPath)
        }
    }

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
            title = { Text(stringResource(R.string.dialog_image_source_title)) },
            text = { Text(stringResource(R.string.dialog_image_source_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Text(stringResource(R.string.source_gallery))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    val uri = ImageUtils.createTempPictureUri(context)
                    tempCameraUri = uri
                    cameraLauncher.launch(uri)
                }) {
                    Text(stringResource(R.string.source_camera))
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

            // 1. 日期與金額
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

            // 2. 分類與備註
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

            // 3. 進階選項
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
                            text = stringResource(R.string.title_advanced_options),
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
                            // A. 圖片
                            Text(stringResource(R.string.label_photo_receipt), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
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
                                        Text(stringResource(R.string.hint_add_photo), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                                    }
                                } else {
                                    val file = File(uiState.imageUri!!)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f))
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(file)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = stringResource(R.string.desc_selected_image),
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )

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
                            Text(stringResource(R.string.label_payment_method), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(paymentMethods, key = { it.id }) { method ->
                                    GlassChip(
                                        label = getSmartPaymentName(method.name, method.resourceKey),
                                        selected = uiState.paymentMethod == method.name,
                                        onClick = { viewModel.updatePaymentMethod(method.name) }
                                    )
                                }
                                item {
                                    GlassIconButton(
                                        onClick = { debounce { showPaymentMethodManager = true } },
                                        size = 32.dp
                                    ) {
                                        Icon(Icons.Default.Add, stringResource(R.string.desc_manage_button), tint = AppTheme.colors.textSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // C. 店家/地點 (地圖搜尋邏輯修正)
                            Text(stringResource(R.string.label_merchant_location), fontSize = 12.sp, color = AppTheme.colors.textSecondary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GlassTextField(
                                    value = uiState.merchant,
                                    onValueChange = { viewModel.updateMerchant(it) },
                                    placeholder = stringResource(R.string.hint_merchant),
                                    label = null,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                GlassIconButton(
                                    onClick = {
                                        debounce {
                                            // [關鍵修正] 判斷輸入框內容
                                            if (uiState.merchant.isBlank()) {
                                                // 情況 1：輸入框為空 -> 請求權限 -> 自動定位
                                                val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                                val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                                                if (hasFine || hasCoarse) {
                                                    getCurrentLocation()
                                                } else {
                                                    locationPermissionLauncher.launch(
                                                        arrayOf(
                                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                                        )
                                                    )
                                                }
                                            } else {
                                                // 情況 2：輸入框有值 -> 開啟搜尋頁面 (帶入預填文字)
                                                val fields = listOf(GooglePlace.Field.NAME, GooglePlace.Field.ADDRESS)
                                                val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                                                    .setInitialQuery(uiState.merchant) // 預填文字
                                                    .build(context)
                                                placeLauncher.launch(intent)
                                            }
                                        }
                                    },
                                    size = 48.dp
                                ) {
                                    // 根據是否有輸入文字，改變 Icon 讓使用者有預期心理
                                    val icon = if (uiState.merchant.isBlank()) Icons.Default.MyLocation else Icons.Default.Search
                                    Icon(icon, contentDescription = stringResource(R.string.desc_search_location), tint = AppTheme.colors.accent)
                                }
                            }

                            HorizontalDivider(color = AppTheme.colors.divider, thickness = 1.dp)

                            // D. 不計入預算
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.updateExcludeFromBudget(!uiState.excludeFromBudget) }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(stringResource(R.string.label_exclude_budget), fontSize = 14.sp, color = AppTheme.colors.textPrimary)
                                    Text(stringResource(R.string.hint_exclude_budget), fontSize = 11.sp, color = AppTheme.colors.textSecondary)
                                }

                                GlassSwitch(
                                    checked = uiState.excludeFromBudget,
                                    onCheckedChange = { viewModel.updateExcludeFromBudget(it) }
                                )
                            }

                            // E. Need vs Want
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