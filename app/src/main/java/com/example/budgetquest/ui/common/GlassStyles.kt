package com.example.budgetquest.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

@Composable
fun getGlassBrush(): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            AppTheme.colors.surface.copy(alpha = 0.65f),
            AppTheme.colors.surface.copy(alpha = 0.35f)
        )
    )
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

// --- 基礎容器 (取代重複的 Box 寫法) ---

/**
 * 標準的玻璃擬態卡片容器
 * 自動套用：圓角裁切、玻璃背景、漸層邊框
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    // [新增] 可選的背景色，預設為 null (代表使用原本的玻璃筆刷)
    backgroundColor: Color? = null,
    // [新增] 可選的邊框色，預設為 null (代表使用原本的邊框筆刷)
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    // 1. 決定背景邏輯
    val backgroundModifier = if (backgroundColor != null) {
        Modifier.background(backgroundColor)
    } else {
        // 如果沒有指定顏色，才使用預設的玻璃漸層
        Modifier.background(getGlassBrush())
    }

    // 2. 決定邊框邏輯
    val borderModifier = if (borderColor != null) {
        Modifier.border(1.dp, borderColor, shape)
    } else {
        // 如果沒有指定顏色，才使用預設的邊框漸層
        Modifier.border(1.dp, getBorderBrush(), shape)
    }

    var finalModifier = modifier
        .clip(shape)
        .then(backgroundModifier) // 應用背景
        .then(borderModifier)     // 應用邊框

    if (onClick != null) {
        finalModifier = finalModifier.clickable { onClick() }
    }

    Box(
        modifier = finalModifier,
        content = content
    )
}