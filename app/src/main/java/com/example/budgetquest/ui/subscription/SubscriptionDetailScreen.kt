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
import com.example.budgetquest.ui.common.getSmartPaymentName
import com.example.budgetquest.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
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

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTerminateDialog by remember { mutableStateOf(false) }

    if (uiState.id != subscriptionId) return

    // 格式化邏輯移到 UI 層
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    // 1. 週期文字
    val cycleText = when(uiState.frequencyType) {
        "MONTH" -> stringResource(R.string.cycle_monthly)
        "WEEK" -> stringResource(R.string.cycle_weekly)
        "DAY" -> stringResource(R.string.cycle_daily)
        "CUSTOM" -> stringResource(R.string.cycle_custom_days, uiState.customDays)
        else -> stringResource(R.string.cycle_custom)
    }

    // 2. 下次支出文字
    val nextDateText = if (!uiState.isActive) {
        stringResource(R.string.status_ended)
    } else {
        val dateStr = dateFormat.format(Date(uiState.nextDateMillis))
        val daysSuffix = if (uiState.daysLeft == 0L) stringResource(R.string.status_today)
        else stringResource(R.string.status_days_later, uiState.daysLeft)
        "$dateStr ($daysSuffix)"
    }

    // 3. 期間文字
    val startStr = if (uiState.startDateMillis > 0) dateFormat.format(Date(uiState.startDateMillis)) else ""
    val endStr = uiState.endDateMillis?.let { dateFormat.format(Date(it)) } ?: stringResource(R.string.period_infinite)
    val periodText = "$startStr ~ $endStr"


    // 刪除確認
    if (showDeleteDialog) {
        ActionDialog(
            title = stringResource(R.string.dialog_delete_sub_title),
            text = stringResource(R.string.dialog_delete_sub_msg),
            confirmText = stringResource(R.string.action_delete_all),
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
            title = stringResource(R.string.dialog_terminate_sub_title),
            text = stringResource(R.string.dialog_terminate_sub_msg),
            confirmText = stringResource(R.string.action_terminate),
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
                title = { Text(stringResource(R.string.title_subscription_detail), color = AppTheme.colors.textPrimary, fontSize = 18.sp) },
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
                                label = if (uiState.isActive) stringResource(R.string.status_active) else stringResource(R.string.status_ended),
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
                        DetailRow(Icons.Default.Loop, stringResource(R.string.label_cycle), cycleText)
                        DetailRow(Icons.Default.NextPlan, stringResource(R.string.label_next_payment), nextDateText)
                        DetailRow(Icons.Default.DateRange, stringResource(R.string.label_period), periodText)

                        // 顯示支付方式
                        if (uiState.paymentMethod.isNotEmpty()) {
                            // 使用 getSmartPaymentName 取得多語言字串
                            val paymentName = getSmartPaymentName(uiState.paymentMethod)
                            DetailRow(Icons.Default.Payment, stringResource(R.string.label_payment_method_detail), paymentName)
                        }

                        // 顯示性質
                        if (uiState.isNeed != null) {
                            val label = if (uiState.isNeed == true) stringResource(R.string.type_need) else stringResource(R.string.type_want)
                            DetailRow(
                                icon = if (uiState.isNeed == true) Icons.Default.Check else Icons.Default.Star,
                                label = stringResource(R.string.label_consumption_type),
                                value = label
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassDetailActionButton(
                            text = stringResource(R.string.action_delete),
                            icon = Icons.Default.Delete,
                            color = AppTheme.colors.fail,
                            modifier = Modifier.weight(1f),
                            onClick = { showDeleteDialog = true }
                        )

                        if (uiState.isActive) {
                            GlassDetailActionButton(
                                text = stringResource(R.string.action_terminate),
                                icon = Icons.Default.Stop,
                                color = Color(0xFFFFB74D),
                                modifier = Modifier.weight(1f),
                                onClick = { showTerminateDialog = true }
                            )
                        }

                        GlassDetailActionButton(
                            text = stringResource(R.string.action_edit),
                            icon = Icons.Default.Edit,
                            color = AppTheme.colors.accent,
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

// ... (ActionDialog, ButtonContent, DetailRow, StatusChip 保持不變) ...
@Composable
private fun ActionDialog(title: String, text: String, confirmText: String, confirmColor: Color, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, null, tint = confirmColor) },
        title = { Text(title, color = AppTheme.colors.textPrimary, fontWeight = FontWeight.Bold) },
        text = { Text(text, color = AppTheme.colors.textSecondary) },
        confirmButton = { TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = confirmColor)) { Text(confirmText, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.textSecondary)) { Text(stringResource(R.string.action_cancel)) } },
        containerColor = AppTheme.colors.surface,
        shape = RoundedCornerShape(20.dp)
    )
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