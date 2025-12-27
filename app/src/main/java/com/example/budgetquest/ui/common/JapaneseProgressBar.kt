package com.example.budgetquest.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// 日系配色
private val SafeGreen = Color(0xFFA5D6A7) // 淺綠
private val WarningRedLight = Color(0xFFEF9A9A) // 淺紅
private val WarningRedDeep = Color(0xFFC62828) // 深紅

@Composable
fun JapaneseBudgetProgressBar(
    totalBudget: Int,
    totalSpent: Int,
    modifier: Modifier = Modifier
) {
    // [防護] 避免除以零
    val safeBudget = if (totalBudget <= 0) 1 else totalBudget

    // 計算花費比例 (0.0 ~ 1.0)
    val spendRatio = (totalSpent.toFloat() / safeBudget).coerceIn(0f, 1f)
    val safeRatio = (1f - spendRatio).coerceAtLeast(0f)

    // 動畫：紅色長度
    val animatedRedWeight by animateFloatAsState(
        targetValue = spendRatio.coerceAtMost(1f),
        animationSpec = tween(1000)
    )

    // 動畫：綠色長度
    val animatedGreenWeight by animateFloatAsState(
        targetValue = safeRatio,
        animationSpec = tween(1000)
    )

    // 動畫：紅色顏色深度 (花越多越深)
    val animatedRedColor by animateColorAsState(
        targetValue = if (spendRatio > 0.8f) WarningRedDeep else WarningRedLight,
        animationSpec = tween(1000)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(50)), // 全圓角
        horizontalArrangement = Arrangement.End // 從右邊開始堆疊
    ) {
        // 綠色部分 (剩餘) - 在左邊
        if (animatedGreenWeight > 0.01f) {
            Box(
                modifier = Modifier
                    .weight(animatedGreenWeight)
                    .fillMaxHeight()
                    .background(SafeGreen)
            )
        }

        // 紅色部分 (已花) - 在右邊
        if (animatedRedWeight > 0.01f) {
            Box(
                modifier = Modifier
                    .weight(animatedRedWeight)
                    .fillMaxHeight()
                    .background(animatedRedColor)
            )
        }
    }
}