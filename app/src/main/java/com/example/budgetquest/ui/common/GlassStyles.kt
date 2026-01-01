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
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val glassBrush = getGlassBrush()
    val borderBrush = getBorderBrush()
    val shape = RoundedCornerShape(cornerRadius)

    var finalModifier = modifier
        .clip(shape)
        .background(glassBrush)
        .border(1.dp, borderBrush, shape)

    if (onClick != null) {
        finalModifier = finalModifier.clickable { onClick() }
    }

    Box(
        modifier = finalModifier,
        content = content
    )
}