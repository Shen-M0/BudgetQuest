package com.example.budgetquest.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
    val configuration = LocalConfiguration.current
    val screenHeight = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }
    val screenWidth = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }

    // [優化 1] 智慧判斷：是深色還是淺色背景？
    // 透過計算背景色的亮度 (Luminance) 來決定光暈的濃度
    val isLightBackground = AppTheme.colors.background.luminance() > 0.5f

    // [優化 2] 調整濃度
    // 淺色模式：背景很亮，需要較重的 Alpha (0.4 / 0.3) 才能顯色
    // 深色模式：背景很暗，光暈稍微亮一點 (0.25 / 0.2) 就很有極光感
    val primaryAlpha = if (isLightBackground) 0.4f else 0.25f
    val secondaryAlpha = if (isLightBackground) 0.3f else 0.2f

    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    // [優化 3] 調整速度
    // 將週期縮短到 8秒 / 11秒 (原本是 12/15)，讓流動感更明顯一點
    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * 3.14159f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset1"
    )

    val offset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * 3.14159f,
        animationSpec = infiniteRepeatable(
            animation = tween(11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset2"
    )

    // 計算軌跡 (維持原本的橢圓運動)
    val x1 = (screenWidth / 2) + (screenWidth / 3) * cos(offset1)
    val y1 = (screenHeight / 3) + (screenHeight / 4) * sin(offset1)

    val x2 = (screenWidth / 2) + (screenWidth / 2.5f) * cos(-offset2)
    val y2 = (screenHeight / 1.5f) + (screenHeight / 5) * sin(-offset2)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        // 光點 1 (Accent Color)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AppTheme.colors.accent.copy(alpha = primaryAlpha),
                            Color.Transparent
                        ),
                        center = Offset(x1, y1),
                        radius = screenWidth * 0.9f //稍微加大半徑，讓暈染更自然
                    )
                )
        )

        // 光點 2 (Success Color - 綠色系在淺色模式下很清爽)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AppTheme.colors.success.copy(alpha = secondaryAlpha),
                            Color.Transparent
                        ),
                        center = Offset(x2, y2),
                        radius = screenWidth * 1.0f
                    )
                )
        )

        // 內容層
        content()
    }
}