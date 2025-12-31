package com.example.budgetquest.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.budgetquest.ui.theme.AppTheme
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // 取得螢幕尺寸
    val configuration = LocalConfiguration.current
    val screenHeight = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }
    val screenWidth = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }

    // 定義動畫：無限循環
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    // 第一個光點的移動 (圓周運動)
    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * 3.14159f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing), // 12秒一圈，非常緩慢
            repeatMode = RepeatMode.Restart
        ),
        label = "offset1"
    )

    // 第二個光點的移動 (橢圓運動，反向)
    val offset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * 3.14159f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing), // 15秒一圈
            repeatMode = RepeatMode.Restart
        ),
        label = "offset2"
    )

    // 計算座標
    val x1 = (screenWidth / 2) + (screenWidth / 3) * cos(offset1)
    val y1 = (screenHeight / 3) + (screenHeight / 4) * sin(offset1)

    val x2 = (screenWidth / 2) + (screenWidth / 2.5f) * cos(-offset2) // 反向
    val y2 = (screenHeight / 1.5f) + (screenHeight / 5) * sin(-offset2)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background) // 底色
    ) {
        // 光點 1：使用 Accent 顏色，透明度極低
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AppTheme.colors.accent.copy(alpha = 0.15f), // 中心顏色
                            Color.Transparent // 外圍透明
                        ),
                        center = Offset(x1, y1),
                        radius = screenWidth * 0.8f // 光暈半徑
                    )
                )
        )

        // 光點 2：使用稍不同的顏色 (例如 Success 或 Secondary)，增加層次
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AppTheme.colors.success.copy(alpha = 0.1f), // 另一種顏色的光暈
                            Color.Transparent
                        ),
                        center = Offset(x2, y2),
                        radius = screenWidth * 0.9f
                    )
                )
        )

        // 內容層
        content()
    }
}