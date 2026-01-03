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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.R
import com.example.budgetquest.ui.AppViewModelProvider
import com.example.budgetquest.ui.common.FluidBoundsTransform
import com.example.budgetquest.ui.common.GlassCard
import com.example.budgetquest.ui.common.GlassDetailActionButton
import com.example.budgetquest.ui.common.GlassIconButton
import com.example.budgetquest.ui.common.getSmartCategoryName
import com.example.budgetquest.ui.theme.AppTheme

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

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTerminateDialog by remember { mutableStateOf(false) }

    if (uiState.id != subscriptionId) return

    // 刪除確認
    if (showDeleteDialog) {
        ActionDialog(
            title = "確認刪除",
            // [用語修正]
            text = "您確定要刪除此固定支出項目嗎？\n注意：這將會一併刪除所有透過此規則自動產生的歷史支出紀錄，且無法復原。",
            confirmText = "全部刪除",
            confirmColor = AppTheme.colors.fail,
            onConfirm = {
                viewModel.deleteSubscription(subscriptionId) {
                    showDeleteDialog = false
                    onDeleteSuccess()
                }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    // 終止確認
    if (showTerminateDialog) {
        ActionDialog(
            title = "確認終止",
            // [用語修正]
            text = "您確定要停止此固定支出嗎？\n這將設定結束日期為今天，之後將不再自動記錄，但「過去的紀錄」會被保留。",
            confirmText = "終止支出",
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
                // [用語修正] 扣款詳情 -> 固定支出詳情
                title = { Text("固定支出詳情", color = AppTheme.colors.textPrimary, fontSize = 18.sp) },
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
                        state = rememberSharedContentState(key = "sub_${subscriptionId}"),
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

                    // Details: [用語修正]
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        DetailRow(Icons.Default.Loop, "支出週期", uiState.cycleText)
                        DetailRow(Icons.Default.NextPlan, "下次支出", uiState.nextDateText)
                        DetailRow(Icons.Default.DateRange, "支出期間", uiState.periodText)

                        // [新增/修正] 真正的智慧顯示：只有資料庫有值才顯示
                        // 因為 SubscriptionDetailViewModel 需要先把 Entity 的這些欄位 expose 出來
                        // 假設 ViewModel 的 uiState 已經加了 paymentMethod (String) 和 isNeed (Boolean?)
                        // (請記得去 SubscriptionDetailUiState 補上這兩個欄位)

                        /* 在 SubscriptionDetailViewModel.kt 的 SubscriptionDetailUiState 加入:
                           val paymentMethod: String = "",
                           val isNeed: Boolean? = null
                           並在 loadSubscription 中賦值
                        */

                        // 顯示支付方式 (如果不為空)
                        if (uiState.paymentMethod.isNotEmpty()) {
                            DetailRow(Icons.Default.Payment, "支付方式", uiState.paymentMethod)
                        }

                        // 顯示性質 (如果不為 null)
                        if (uiState.isNeed != null) {
                            val label = if (uiState.isNeed == true) "需要 (Need)" else "想要 (Want)"
                            // 這裡可以用 DetailRow 或 StatusChip，看您喜歡哪種排版，這裡示範 DetailRow
                            DetailRow(
                                icon = if (uiState.isNeed == true) Icons.Default.Check else Icons.Default.Star,
                                label = "消費性質",
                                value = label
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassDetailActionButton(
                            text = "刪除",
                            icon = Icons.Default.Delete,
                            color = AppTheme.colors.fail,
                            modifier = Modifier.weight(1f),
                            onClick = { showDeleteDialog = true }
                        )

                        if (uiState.isActive) {
                            GlassDetailActionButton(
                                text = "終止",
                                icon = Icons.Default.Stop,
                                color = Color(0xFFFFB74D),
                                modifier = Modifier.weight(1f),
                                onClick = { showTerminateDialog = true }
                            )
                        }

                        GlassDetailActionButton(
                            text = "編輯",
                            icon = Icons.Default.Edit,
                            color = AppTheme.colors.accent, // 或 textPrimary
                            modifier = Modifier.weight(1f),
                            onClick = { onEditClick(subscriptionId) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ... (Helper components 保持不變) ...
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