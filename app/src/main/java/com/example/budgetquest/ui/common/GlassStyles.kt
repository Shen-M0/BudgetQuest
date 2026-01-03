package com.example.budgetquest.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgetquest.ui.theme.AppTheme

// --- 筆刷定義 ---

// 一般元件使用的玻璃筆刷 (依賴 AppTheme)
@Composable
fun getGlassBrush(): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            AppTheme.colors.surface.copy(alpha = 0.65f),
            AppTheme.colors.surface.copy(alpha = 0.35f)
        )
    )
}

// [修正] Dialog 專用筆刷：確保深色模式正確，淺色模式夠亮
@Composable
fun getDialogGlassBrush(): Brush {
    val isDark = isSystemInDarkTheme()
    return if (isDark) {
        // 深色模式：強制使用深色底 (Color(0xFF121212))，避免因 Dialog 抓不到 AppTheme 而變白
        // 參數與 getGlassBrush 保持一致 (0.65f -> 0.35f)，還原您原本喜歡的風格
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF121212).copy(alpha = 0.65f),
                Color(0xFF121212).copy(alpha = 0.35f)
            )
        )
    } else {
        // 淺色模式：使用高亮白色，對抗遮罩變暗
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
        else -> Modifier.background(getGlassBrush()) // 預設情況
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