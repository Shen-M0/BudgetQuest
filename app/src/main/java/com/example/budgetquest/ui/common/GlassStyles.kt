package com.example.budgetquest.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgetquest.R
import com.example.budgetquest.ui.theme.AppTheme

// --- 筆刷定義 ---

// 一般元件使用的玻璃筆刷 (依賴 AppTheme)
// 這是您原本喜歡的風格
@Composable
fun getGlassBrush(): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            AppTheme.colors.surface.copy(alpha = 0.65f),
            AppTheme.colors.surface.copy(alpha = 0.35f)
        )
    )
}

// [修正] Dialog 專用筆刷
@Composable
fun getDialogGlassBrush(): Brush {
    // [關鍵修正] 不再依賴 isSystemInDarkTheme()，而是檢查當前 AppTheme 的表面顏色亮度。
    // 如果亮度 < 0.5，代表目前是深色主題 (無論是系統設定還是 APP 內切換)。
    val isDarkTheme = AppTheme.colors.surface.luminance() < 0.5f

    return if (isDarkTheme) {
        // 深色模式：直接使用標準玻璃筆刷
        // 這樣就能 100% 還原您原本喜歡的深色通透風格
        getGlassBrush()
    } else {
        // 淺色模式：使用高亮白色
        // 這是為了避免 Dialog 在黑色遮罩(Scrim)上看起來灰暗髒髒的
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                Color.White.copy(alpha = 0.85f)
            )
        )
    }
}

@Composable
fun getBorderBrush(): Brush {
    return Brush.linearGradient(
        colors = listOf(
            AppTheme.colors.textPrimary.copy(alpha = 0.25f),
            AppTheme.colors.textPrimary.copy(alpha = 0.10f)
        )
    )
}

@Composable
fun getAuroraGradient(): Brush {
    return Brush.horizontalGradient(
        colors = listOf(
            AppTheme.colors.accent,
            AppTheme.colors.accent.copy(alpha = 0.7f)
        )
    )
}

// --- 文字樣式 ---

@Composable
fun getShadowTextStyle(fontSize: Int, fontWeight: FontWeight = FontWeight.Normal): TextStyle {
    return TextStyle(
        fontSize = fontSize.sp,
        fontWeight = fontWeight,
        color = AppTheme.colors.textPrimary,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.1f),
            offset = Offset(2f, 2f),
            blurRadius = 4f
        )
    )
}

// --- 基礎容器 ---

/**
 * 標準的玻璃擬態卡片容器
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    // [重要] 保留此參數以支援 Dialog 的特殊處理
    customBrush: Brush? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    // 1. 決定背景邏輯 (優先順序：CustomBrush > BackgroundColor > DefaultBrush)
    val backgroundModifier = when {
        customBrush != null -> Modifier.background(customBrush)
        backgroundColor != null -> Modifier.background(backgroundColor)
        else -> Modifier.background(getGlassBrush()) // 預設使用標準風格
    }

    // 2. 決定邊框邏輯
    val borderModifier = if (borderColor != null) {
        Modifier.border(1.dp, borderColor, shape)
    } else {
        Modifier.border(1.dp, getBorderBrush(), shape)
    }

    var finalModifier = modifier
        .clip(shape)
        .then(backgroundModifier)
        .then(borderModifier)

    if (onClick != null) {
        finalModifier = finalModifier.clickable { onClick() }
    }

    Box(
        modifier = finalModifier,
        content = content
    )
}

// --- 業務邏輯元件 (從 SummaryScreen 移入) ---

/**
 * 預算狀態卡片
 * 顯示：剩餘金額、進度條、狀態訊息
 */
@Composable
fun MinimalBudgetCard(
    totalBudget: Int,
    totalSpent: Int,
    message: String,
    modifier: Modifier = Modifier
) {
    val remaining = totalBudget - totalSpent

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // 標題區：金額 + 標籤
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stringResource(R.string.amount_currency_format, remaining),
                    style = getShadowTextStyle(fontSize = 32, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.label_remaining_budget),
                    fontSize = 12.sp,
                    color = AppTheme.colors.textSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 進度條 (需確保 JapaneseProgressBar.kt 存在於同 package)
            JapaneseBudgetProgressBar(
                totalBudget = totalBudget,
                totalSpent = totalSpent,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 狀態訊息
            Text(
                text = message,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = AppTheme.colors.textSecondary
            )
        }
    }
}
