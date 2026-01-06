package com.example.budgetquest.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgetquest.ui.theme.AppTheme
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.zIndex


/**
 * 1. 玻璃圓形按鈕 (用於 TopBar 返回、功能選單)
 * 整合了 GlassIconContainer 與 IconButton 的功能
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    icon: @Composable () -> Unit
) {
    val glassBrush = getGlassBrush()
    val borderBrush = getBorderBrush()

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape) // 1. 先裁切 (確保水波紋是圓的)
            .background(glassBrush)
            .border(1.dp, borderBrush, CircleShape)
            .clickable { onClick() }, // 2. 再點擊
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

/**
 * 2. 極光懸浮按鈕 (FAB)
 * 採用 DailyDetailScreen/DashboardScreen 的最新版本 (支援深色模式優化)
 */
@Composable
fun AuroraFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    // 深色模式下降低亮度與透明度
    val gradientColors = if (isDark) {
        listOf(
            AppTheme.colors.accent.copy(alpha = 0.4f),
            AppTheme.colors.accent.copy(alpha = 0.7f)
        )
    } else {
        listOf(
            AppTheme.colors.accent.copy(alpha = 0.8f),
            AppTheme.colors.accent
        )
    }

    val gradientBrush = Brush.linearGradient(colors = gradientColors)
    val shadowColor = AppTheme.colors.accent.copy(alpha = if (isDark) 0.5f else 1f)

    FloatingActionButton(
        onClick = onClick,
        containerColor = Color.Transparent,
        elevation = FloatingActionButtonDefaults.elevation(0.dp),
        modifier = modifier
            .size(56.dp)
            .shadow(
                elevation = if (isDark) 8.dp else 12.dp,
                shape = CircleShape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush, CircleShape)
                .border(1.dp, Color.White.copy(alpha = if (isDark) 0.15f else 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, null, tint = Color.White.copy(alpha = if (isDark) 0.9f else 1f), modifier = Modifier.size(28.dp))
        }
    }
}

/**
 * 3. 極光主要長按鈕 (用於 Save/Start)
 * 採用 PlanSetupScreen 的版本
 */
@Composable
fun AuroraPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = AppTheme.colors.accent, spotColor = AppTheme.colors.accent)
            .clip(RoundedCornerShape(16.dp))
            .background(getAuroraGradient())
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * 4. 通用功能/危險按鈕 (用於 Delete, Backup, Restore)
 * 整合了 GlassSimpleButton 與 GlassDangerButton
 */
@Composable
fun GlassActionTextButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDanger: Boolean = false
) {
    val glassBrush = getGlassBrush()
    val contentColor = if (isDanger) AppTheme.colors.fail else AppTheme.colors.textPrimary

    // 如果是危險按鈕，邊框帶紅色；否則使用標準邊框
    val borderBrush = if (isDanger) {
        Brush.linearGradient(
            colors = listOf(
                AppTheme.colors.fail.copy(alpha = 0.5f),
                AppTheme.colors.fail.copy(alpha = 0.2f)
            )
        )
    } else {
        getBorderBrush()
    }

    Box(
        modifier = modifier
            .height(50.dp) // 統一高度
            .clip(RoundedCornerShape(12.dp))
            .background(glassBrush)
            .border(1.dp, borderBrush, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 5. 詳情頁動作按鈕 (用於 刪除/終止/編輯)
 * 特色：極低透明度的背景 (0.05f)，強調邊框與文字顏色，視覺輕量化，適合並排顯示
 */
@Composable
fun GlassDetailActionButton(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // [修正] 提高透明度：從 0.05f 提升到 0.12f，讓實體感更強，符合您的需求
    val backgroundColor = color.copy(alpha = 0.12f)
    // 邊框也稍微加深一點
    val borderColor = color.copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = color,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun GlassTabRow(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<String>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(AppTheme.colors.surface.copy(alpha = 0.4f))
            .border(1.dp, AppTheme.colors.textSecondary.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
    ) {
        // [修正] 明確使用 maxWidth (這是 BoxWithConstraintsScope 的屬性)
        // 如果您的編譯器報錯，有可能是因為 import 問題，但通常這裡直接用即可
        val totalWidth = this.maxWidth
        val tabWidth = totalWidth / tabs.size

        // ... (其餘動畫邏輯保持不變)

        // [動畫] 計算指示器的 X 軸偏移量
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedTabIndex,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "IndicatorOffset"
        )

        // [中層] 滑動指示器 (Active Indicator)
        // 放在文字層之前繪製，確保它永遠在文字「後面」
        Box(
            modifier = Modifier
                .width(tabWidth)
                .fillMaxHeight()
                .offset(x = indicatorOffset)
                .padding(4.dp) // 內縮一點，製造懸浮感
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = AppTheme.colors.accent // 讓陰影帶有主題色
                )
                .clip(RoundedCornerShape(24.dp))
                .background(
                    // 使用漸層刷色，讓指示器更有質感
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AppTheme.colors.accent,
                            AppTheme.colors.accent.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        // [上層] 文字層 (Tabs)
        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTabIndex == index

                // 文字顏色動畫
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else AppTheme.colors.textSecondary,
                    label = "TextColor"
                )

                // 字重動畫 (選中時變粗)
                val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium

                Box(
                    modifier = Modifier
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null // 移除點擊水波紋，保持乾淨
                        ) { onTabSelected(index) }
                        .zIndex(1f), // 強制提升層級
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = fontWeight,
                        // [關鍵] 這裡不需要陰影，保持文字清晰，因為背景已經有區隔了
                    )
                }
            }
        }
    }
}