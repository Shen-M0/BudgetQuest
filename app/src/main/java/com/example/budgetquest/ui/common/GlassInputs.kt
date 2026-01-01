package com.example.budgetquest.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgetquest.ui.theme.AppTheme

/**
 * 玻璃風格輸入框
 * 取代 JapaneseTextField
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    label: String? = null,
    isNumber: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (label != null) {
            Text(label, fontSize = 12.sp, color = AppTheme.colors.textSecondary, modifier = Modifier.padding(bottom = 4.dp))
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = AppTheme.colors.textSecondary.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 18.sp,
                color = AppTheme.colors.textPrimary
            ),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppTheme.colors.accent,
                unfocusedBorderColor = Color.Transparent, // 無焦點時隱藏邊框，靠背景色
                focusedContainerColor = AppTheme.colors.background.copy(alpha = 0.7f),
                unfocusedContainerColor = AppTheme.colors.background.copy(alpha = 0.5f),
                focusedTextColor = AppTheme.colors.textPrimary,
                unfocusedTextColor = AppTheme.colors.textPrimary
            ),
            keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
            singleLine = true
        )
    }
}

/**
 * 玻璃風格選擇標籤 (Chip)
 * 取代 JapaneseCompactChip
 */
@Composable
fun GlassChip(
    label: String,
    selected: Boolean,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val glassBrush = getGlassBrush()
    val borderBrush = getBorderBrush()

    // 選中時：強調色背景；未選中時：玻璃背景+邊框
    val backgroundModifier = if (selected) {
        Modifier.background(AppTheme.colors.accent, RoundedCornerShape(8.dp))
    } else {
        Modifier
            .background(glassBrush, RoundedCornerShape(8.dp))
            .border(1.dp, borderBrush, RoundedCornerShape(8.dp))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .then(backgroundModifier)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = if (selected) Color.White else AppTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                label,
                fontSize = 12.sp,
                color = if (selected) Color.White else AppTheme.colors.textSecondary
            )
        }
    }
}