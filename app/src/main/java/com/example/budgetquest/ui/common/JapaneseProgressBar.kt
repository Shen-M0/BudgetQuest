package com.example.budgetquest.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.budgetquest.ui.theme.AppTheme

@Composable
fun JapaneseBudgetProgressBar(
    totalBudget: Int,
    totalSpent: Int,
    modifier: Modifier = Modifier
) {
    // 1. 計算邏輯
    val safeBudget = if (totalBudget <= 0) 1 else totalBudget
    // 花費比例 (0.0 ~ 1.0)
    val spendRatio = (totalSpent.toFloat() / safeBudget).coerceIn(0f, 1f)

    // 交界點位置：因為紅色從右邊長出來，所以交界點是 (1.0 - 花費比例)
    // 例如：花了 30%，紅色佔右邊 30%，交界點就在 0.7 的位置
    val targetSplitPoint = 1f - spendRatio

    // 2. 動畫：平滑移動交界點
    val animatedSplit by animateFloatAsState(
        targetValue = targetSplitPoint,
        animationSpec = tween(1000),
        label = "splitPoint"
    )

    // 3. 定義顏色 (日系極光色)
    val greenLight = Color(0xFFA5D6A7) // 左端：清新的亮綠
    val greenDark  = Color(0xFF66BB6A) // 交界：深綠
    val redDark    = Color(0xFFE57373) // 交界：深紅
    val redLight   = Color(0xFFEF9A9A) // 右端：柔和的亮紅

    // 4. [關鍵優化] 動態漸層筆刷
    // 我們不切斷它，而是建立一個包含 4 個節點的漸層
    // 透過計算 blurRadius，讓紅綠交界處有一段模糊緩衝區
    val brush = remember(animatedSplit) {
        // 模糊半徑：約佔總寬度的 5%，讓交界處柔和暈開
        val blurRadius = 0.05f

        // 計算四個顏色的停靠點 (Color Stops)
        val stopGreenEnd = (animatedSplit - blurRadius).coerceIn(0f, 1f)
        val stopRedStart = (animatedSplit + blurRadius).coerceIn(0f, 1f)

        Brush.horizontalGradient(
            colorStops = arrayOf(
                0.0f to greenLight,      // 0% 起點
                stopGreenEnd to greenDark, // 綠色結束前變深
                stopRedStart to redDark,   // 紅色開始時較深
                1.0f to redLight         // 100% 終點
            )
        )
    }

    // 5. 繪製容器
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(CircleShape)
            // 外框：極細的半透明線，增加精緻度
            .border(0.5.dp, AppTheme.colors.textSecondary.copy(alpha = 0.2f), CircleShape)
            // 底色：極淡的背景，防呆用
            .background(AppTheme.colors.surface.copy(alpha = 0.1f))
    ) {
        // 唯一的進度條，填滿整個容器，由 Brush 決定哪裡綠、哪裡紅
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
        )
    }
}