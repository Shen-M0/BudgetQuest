package com.example.budgetquest.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.budgetquest.data.PaymentMethodEntity
import com.example.budgetquest.ui.common.GlassCard
import com.example.budgetquest.ui.common.GlassIconButton
import com.example.budgetquest.ui.common.GlassTextField
import com.example.budgetquest.ui.common.getDialogGlassBrush
import com.example.budgetquest.ui.theme.AppTheme

@Composable
fun PaymentMethodManagerDialog(
    paymentMethods: List<PaymentMethodEntity>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onToggleVisibility: (PaymentMethodEntity) -> Unit,
    onDelete: (PaymentMethodEntity) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            customBrush = getDialogGlassBrush()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "管理支付方式",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary
                )

                // 新增區塊
                var newName by remember { mutableStateOf("") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = "新支付方式",
                        placeholder = "",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    GlassIconButton(
                        onClick = {
                            if (newName.isNotBlank()) {
                                onAdd(newName)
                                newName = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, null, tint = AppTheme.colors.textPrimary)
                    }
                }

                HorizontalDivider(color = AppTheme.colors.divider)

                // 列表區塊
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(paymentMethods) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppTheme.colors.background.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.name,
                                color = if (item.isVisible) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary,
                                fontSize = 16.sp
                            )

                            Row {
                                IconButton(onClick = { onToggleVisibility(item) }) {
                                    Icon(
                                        imageVector = if (item.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = AppTheme.colors.textSecondary
                                    )
                                }
                                IconButton(onClick = { onDelete(item) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = AppTheme.colors.fail
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}