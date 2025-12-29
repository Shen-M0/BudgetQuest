package com.example.budgetquest.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgetquest.R
import com.example.budgetquest.ui.theme.AppTheme

enum class CoachMarkPosition { Top, Bottom, Auto }

data class CoachMarkTarget(
    val coordinates: LayoutCoordinates?,
    val title: String,
    val description: String,
    val isCircle: Boolean = true,
    val position: CoachMarkPosition = CoachMarkPosition.Auto,
    val extraHeight: Dp = 0.dp
)

@Composable
fun CoachMarkOverlay(
    target: CoachMarkTarget?,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    if (target?.coordinates == null || !target.coordinates.isAttached) return

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val primaryColor = AppTheme.colors.accent
    val textColor = Color.White

    // [提取] 點擊提示文字
    val skipHintText = stringResource(R.string.coach_mark_skip_hint)

    var lastClickTime by remember { mutableLongStateOf(0L) }

    val position = target.coordinates.positionInRoot()
    val size = target.coordinates.size
    val padding = 8.dp.value
    val extraHeightPx = with(density) { target.extraHeight.toPx() }

    val targetRect = Rect(
        offset = position,
        size = Size(
            size.width.toFloat(),
            size.height.toFloat() + extraHeightPx
        )
    ).inflate(padding)

    val center = targetRect.center

    val textLayoutResult = remember(target.title, target.description) {
        textMeasurer.measure(
            text = "${target.title}\n${target.description}",
            style = TextStyle(
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp
            )
        )
    }

    val skipTextResult = remember(skipHintText) { // 記得加入 key
        textMeasurer.measure(
            text = skipHintText,
            style = TextStyle(color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime > 500) {
                    lastClickTime = currentTime
                    onNext()
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawContext.canvas.nativeCanvas.saveLayer(null, null)

            drawRect(color = Color.Black.copy(alpha = 0.7f))

            if (target.isCircle) {
                drawCircle(
                    color = Color.Transparent,
                    radius = targetRect.width / 2,
                    center = center,
                    blendMode = BlendMode.Clear
                )
                drawCircle(
                    color = primaryColor,
                    radius = targetRect.width / 2 + 4f,
                    center = center,
                    style = Stroke(width = 4f)
                )
            } else {
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = targetRect.topLeft,
                    size = targetRect.size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                    blendMode = BlendMode.Clear
                )
                drawRoundRect(
                    color = primaryColor,
                    topLeft = targetRect.topLeft.minus(Offset(2f, 2f)),
                    size = Size(targetRect.width + 4f, targetRect.height + 4f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                    style = Stroke(width = 4f)
                )
            }

            drawContext.canvas.nativeCanvas.restore()

            val showBelow = when (target.position) {
                CoachMarkPosition.Bottom -> true
                CoachMarkPosition.Top -> false
                CoachMarkPosition.Auto -> center.y < this.size.height / 2
            }

            val textPadding = 32.dp.toPx()
            val arrowLength = 60.dp.toPx()
            val canvasWidth = this.size.width
            val textWidth = textLayoutResult.size.width

            val minX = textPadding
            val maxX = canvasWidth - textWidth - textPadding
            val textX = if (maxX > minX) {
                (center.x - textWidth / 2).coerceIn(minX, maxX)
            } else {
                minX
            }

            val textY = if (showBelow) {
                targetRect.bottom + arrowLength
            } else {
                targetRect.top - arrowLength - textLayoutResult.size.height
            }

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(textX, textY)
            )

            val path = Path().apply {
                if (showBelow) {
                    moveTo(center.x, targetRect.bottom + 10f)
                    lineTo(center.x, targetRect.bottom + arrowLength - 10f)
                    moveTo(center.x, targetRect.bottom + 10f)
                    lineTo(center.x - 15f, targetRect.bottom + 30f)
                    moveTo(center.x, targetRect.bottom + 10f)
                    lineTo(center.x + 15f, targetRect.bottom + 30f)
                } else {
                    moveTo(center.x, targetRect.top - 10f)
                    lineTo(center.x, targetRect.top - arrowLength + 10f)
                    moveTo(center.x, targetRect.top - 10f)
                    lineTo(center.x - 15f, targetRect.top - 30f)
                    moveTo(center.x, targetRect.top - 10f)
                    lineTo(center.x + 15f, targetRect.top - 30f)
                }
            }
            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(width = 3f)
            )

            drawText(
                textLayoutResult = skipTextResult,
                topLeft = Offset(
                    (canvasWidth - skipTextResult.size.width) / 2f,
                    this.size.height - 90.dp.toPx()
                )
            )
        }
    }
}