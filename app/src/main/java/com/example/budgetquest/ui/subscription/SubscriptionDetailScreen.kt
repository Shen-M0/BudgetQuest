package com.example.budgetquest.ui.subscription

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.R
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.common.FluidBoundsTransform
import com.example.budgetquest.ui.common.GlassCard
import com.example.budgetquest.ui.common.GlassIconButton
import com.example.budgetquest.ui.common.getSmartCategoryName
import com.example.budgetquest.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionDetailScreen(
    subscriptionId: Long,
    onBackClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteSuccess: () -> Unit,
    viewModel: SubscriptionDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    LaunchedEffect(subscriptionId) {
        viewModel.loadSubscription(subscriptionId)
    }

    val uiState by viewModel.uiState.collectAsState()

    // 控制對話框
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTerminateDialog by remember { mutableStateOf(false) }

    if (uiState.id != subscriptionId) return

    val context = LocalContext.current

    // 計算下次扣款日
    val nextDueDateString = remember(uiState.dayOfMonth, uiState.isActive) {
        if (!uiState.isActive) "已結束"
        else {
            val cal = Calendar.getInstance()
            val today = cal.get(Calendar.DAY_OF_MONTH)
            if (today > uiState.dayOfMonth) {
                cal.add(Calendar.MONTH, 1)
            }
            cal.set(Calendar.DAY_OF_MONTH, uiState.dayOfMonth)
            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            val dateStr = sdf.format(cal.time)

            // 計算剩餘天數
            val diff = cal.timeInMillis - System.currentTimeMillis()
            val daysLeft = (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
            "$dateStr (還有 $daysLeft 天)"
        }
    }

    // 刪除確認
    if (showDeleteDialog) {
        ActionDialog(
            title = "確認刪除",
            text = "您確定要完全刪除此訂閱嗎？歷史紀錄將無法追溯。",
            confirmText = "刪除",
            confirmColor = AppTheme.colors.fail,
            onConfirm = {
                viewModel.deleteSubscription(subscriptionId)
                showDeleteDialog = false
                onDeleteSuccess()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    // 終止確認
    if (showTerminateDialog) {
        ActionDialog(
            title = "確認終止",
            text = "您確定要停止此訂閱嗎？\n這將設定結束日期為今天，但會保留過去的紀錄。",
            confirmText = "終止訂閱",
            confirmColor = AppTheme.colors.accent,
            onConfirm = {
                viewModel.terminateSubscription(subscriptionId)
                showTerminateDialog = false
            },
            onDismiss = { showTerminateDialog = false }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("訂閱詳情", color = AppTheme.colors.textPrimary, fontSize = 18.sp) },
                navigationIcon = {
                    GlassIconButton(onClick = onBackClick, modifier = Modifier.padding(start = 12.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), tint = AppTheme.colors.textPrimary, modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = AppTheme.colors.surface.copy(alpha = 0.8f))
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
                        state = rememberSharedContentState(key = "sub_${subscriptionId}"), // 注意這裡的 Key
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = FluidBoundsTransform
                    )
                }
            }

            GlassCard(modifier = cardModifier, cornerRadius = 24.dp) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = getSmartCategoryName(uiState.category),
                                fontSize = 14.sp,
                                color = AppTheme.colors.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            // 狀態標籤
                            StatusChip(
                                label = if (uiState.isActive) "進行中" else "已結束",
                                color = if (uiState.isActive) AppTheme.colors.success else AppTheme.colors.textSecondary,
                                icon = if (uiState.isActive) Icons.Default.CheckCircle else Icons.Default.Cancel
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.name,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.textPrimary,
                                lineHeight = 32.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.amount_negative_format, uiState.amount),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AppTheme.colors.textPrimary,
                                maxLines = 1
                            )
                        }
                    }

                    HorizontalDivider(color = AppTheme.colors.divider, thickness = 1.dp)

                    // Details
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        DetailRow(Icons.Default.DateRange, "扣款週期", "每月 ${uiState.dayOfMonth} 號")
                        DetailRow(Icons.Default.NextPlan, "下次扣款", nextDueDateString)

                        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                        val startStr = dateFormat.format(java.util.Date(uiState.startDate))
                        val endStr = uiState.endDate?.let { dateFormat.format(java.util.Date(it)) } ?: "至今"
                        DetailRow(Icons.Default.History, "訂閱期間", "$startStr ~ $endStr")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // 1. 刪除 (紅)
                        GlassCard(
                            modifier = Modifier.weight(1f),
                            cornerRadius = 16.dp,
                            backgroundColor = AppTheme.colors.fail.copy(alpha = 0.15f),
                            borderColor = AppTheme.colors.fail.copy(alpha = 0.3f),
                            onClick = { showDeleteDialog = true }
                        ) {
                            ButtonContent(Icons.Default.Delete, "刪除", AppTheme.colors.fail)
                        }

                        // 2. 終止 (橘/黃) - 只有進行中才顯示
                        if (uiState.isActive) {
                            GlassCard(
                                modifier = Modifier.weight(1f),
                                cornerRadius = 16.dp,
                                backgroundColor = Color(0xFFFFB74D).copy(alpha = 0.15f),
                                borderColor = Color(0xFFFFB74D).copy(alpha = 0.3f),
                                onClick = { showTerminateDialog = true }
                            ) {
                                ButtonContent(Icons.Default.Stop, "終止", Color(0xFFFFB74D))
                            }
                        }

                        // 3. 編輯 (藍/Accent)
                        // 3. 編輯 (藍/Accent)
                        GlassCard(
                            modifier = Modifier.weight(1f),
                            cornerRadius = 16.dp,
                            backgroundColor = AppTheme.colors.accent.copy(alpha = 0.15f),
                            borderColor = AppTheme.colors.accent.copy(alpha = 0.3f),
                            // [修正] 這裡只需要呼叫傳進來的函式，把 ID 傳出去就好
                            onClick = { onEditClick(subscriptionId) }
                        ) {
                            ButtonContent(Icons.Default.Edit, "編輯", AppTheme.colors.accent)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ActionDialog(title: String, text: String, confirmText: String, confirmColor: Color, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, null, tint = confirmColor) },
        title = { Text(title, color = AppTheme.colors.textPrimary, fontWeight = FontWeight.Bold) },
        text = { Text(text, color = AppTheme.colors.textSecondary) },
        confirmButton = { TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = confirmColor)) { Text(confirmText, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.textSecondary)) { Text("取消") } },
        containerColor = AppTheme.colors.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun ButtonContent(icon: ImageVector, text: String, color: Color) {
    Row(
        modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = AppTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text("$label：", fontSize = 14.sp, color = AppTheme.colors.textSecondary)
        Spacer(modifier = Modifier.width(4.dp))
        Text(value, fontSize = 15.sp, color = AppTheme.colors.textPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatusChip(label: String, color: Color, icon: ImageVector? = null) {
    Surface(color = color.copy(alpha = 0.12f), contentColor = color, shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(icon, null, modifier = Modifier.size(12.dp)); Spacer(modifier = Modifier.width(4.dp)) }
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}