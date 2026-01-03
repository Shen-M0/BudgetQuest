package com.example.budgetquest.ui.transaction

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.budgetquest.R
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.common.FluidBoundsTransform
import com.example.budgetquest.ui.common.GlassCard
import com.example.budgetquest.ui.common.GlassDetailActionButton
import com.example.budgetquest.ui.common.GlassIconButton
import com.example.budgetquest.ui.common.getSmartCategoryName
import com.example.budgetquest.ui.common.getSmartNote
import com.example.budgetquest.ui.theme.AppTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    expenseId: Long,
    onBackClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteClick: () -> Unit,
    viewModel: TransactionViewModel = viewModel(factory = AppViewModelProvider.Factory),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    LaunchedEffect(expenseId) {
        viewModel.loadExpense(expenseId)
    }

    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (uiState.id != expenseId) return

    val dateFormatString = stringResource(R.string.date_format_transaction)
    val dateFormatter = remember(dateFormatString) { SimpleDateFormat(dateFormatString, Locale.getDefault()) }
    val context = LocalContext.current

    // 刪除確認對話框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = AppTheme.colors.fail) },
            title = { Text(text = "確認刪除", color = AppTheme.colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(text = "您確定要刪除這筆消費紀錄嗎？此操作無法復原。", color = AppTheme.colors.textSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClick()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.fail)
                ) {
                    Text("刪除", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.textSecondary)
                ) {
                    Text("取消")
                }
            },
            containerColor = AppTheme.colors.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_transaction_detail), color = AppTheme.colors.textPrimary, fontSize = 18.sp) },
                navigationIcon = {
                    GlassIconButton(
                        onClick = onBackClick,
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
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            var cardModifier = Modifier.fillMaxWidth()

            if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    cardModifier = cardModifier.sharedElement(
                        state = rememberSharedContentState(key = "trans_${expenseId}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = FluidBoundsTransform
                    )
                }
            }

            GlassCard(
                modifier = cardModifier,
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. 標題區塊：分類、備註與金額
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = getSmartCategoryName(uiState.category),
                            fontSize = 14.sp,
                            color = AppTheme.colors.textSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = getSmartNote(uiState.note),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.textPrimary,
                                lineHeight = 32.sp,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = stringResource(R.string.amount_negative_format, uiState.amount.toIntOrNull() ?: 0),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AppTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Visible
                            )
                        }
                    }

                    HorizontalDivider(color = AppTheme.colors.divider, thickness = 1.dp)

                    // 2. 詳細資訊列表
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DetailRow(icon = Icons.Default.DateRange, label = "日期", value = dateFormatter.format(Date(uiState.date)))

                        // 店家/地點：右側加入地圖搜尋按鈕
                        if (uiState.merchant.isNotEmpty()) {
                            DetailRow(
                                icon = Icons.Default.LocationOn,
                                label = "店家",
                                value = uiState.merchant,
                                actionIcon = Icons.Default.Place,
                                onActionClick = {
                                    val merchantName = uiState.merchant
                                    // geo:0,0?q=查詢字串
                                    // 這個 Intent 會自動處理：
                                    // 1. 如果 merchantName 是店名 (如 "7-11") -> 搜尋附近店家
                                    // 2. 如果 merchantName 是地址 (如 "台北市信義路...") -> 標記該地址
                                    val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(merchantName)}")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                    mapIntent.setPackage("com.google.android.apps.maps")

                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (e: Exception) {
                                        try {
                                            val webIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                            context.startActivity(webIntent)
                                        } catch (e2: Exception) {
                                            Toast.makeText(context, "無法開啟地圖應用程式", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }

                        // [修正] 智慧顯示
                        if (uiState.paymentMethod.isNotEmpty()) {
                            DetailRow(icon = Icons.Default.Payment, label = "支付", value = uiState.paymentMethod)
                        }

                        if (uiState.isNeed != null || uiState.excludeFromBudget) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (uiState.isNeed != null) {
                                    StatusChip(
                                        label = if (uiState.isNeed == true) "需要 (Need)" else "想要 (Want)",
                                        color = if (uiState.isNeed == true) AppTheme.colors.success else AppTheme.colors.accent,
                                        icon = if (uiState.isNeed == true) Icons.Default.Check else null
                                    )
                                }
                                if (uiState.excludeFromBudget) {
                                    StatusChip(label = "不計入預算", color = AppTheme.colors.textSecondary)
                                }
                            }
                        }
                    }

                    // 3. 圖片區域
                    if (!uiState.imageUri.isNullOrEmpty()) {
                        val file = File(uiState.imageUri!!)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 150.dp, max = 400.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(file)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "附圖",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. 底部動作按鈕
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GlassDetailActionButton(
                            text = "刪除",
                            icon = Icons.Default.Delete,
                            color = AppTheme.colors.fail,
                            modifier = Modifier.weight(1f),
                            onClick = { showDeleteDialog = true }
                        )

                        GlassDetailActionButton(
                            text = "編輯",
                            icon = Icons.Default.Edit,
                            color = AppTheme.colors.accent,
                            modifier = Modifier.weight(1f),
                            onClick = { onEditClick(expenseId) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Helper components
@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    actionIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = AppTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text("$label：", fontSize = 14.sp, color = AppTheme.colors.textSecondary)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            color = AppTheme.colors.textPrimary,
            fontWeight = FontWeight.Medium
        )

        if (actionIcon != null && onActionClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = actionIcon,
                contentDescription = null,
                tint = AppTheme.colors.accent,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable { onActionClick() }
            )
        }
    }
}

@Composable
private fun StatusChip(label: String, color: Color, icon: ImageVector? = null) {
    Surface(
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}