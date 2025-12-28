package com.example.budgetquest.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// 定義顏色結構
data class BudgetQuestColors(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val success: Color,
    val fail: Color,
    val divider: Color
)

// 淺色主題 (原本的配色)
val LightColorPalette = BudgetQuestColors(
    background = Color(0xFFF7F9FC),
    surface = Color.White,
    textPrimary = Color(0xFF455A64),
    textSecondary = Color(0xFF90A4AE),
    accent = Color(0xFF78909C),
    success = Color(0xFFA5D6A7), // 淺綠
    fail = Color(0xFFEF9A9A),    // 淺紅
    divider = Color(0xFFECEFF1)
)

// [修改重點] 深色模式優化
val DarkColorPalette = BudgetQuestColors(
    background = Color(0xFF121212),       // 極深灰背景
    surface = Color(0xFF252525),          // 卡片背景 (稍微亮一點，形成層次)
    textPrimary = Color(0xFFE0E0E0),      // 柔和的白色 (不要純白)
    textSecondary = Color(0xFFA0A0A0),    // 中灰色
    accent = Color(0xFF90A4AE),           // 藍灰色

    // [關鍵修改] 降低飽和度，讓深色模式下的紅綠燈不刺眼
    success = Color(0xFF81C784),          // 柔和的粉綠色 (Material Green 300)
    fail = Color(0xFFE57373),             // 柔和的粉紅色 (Material Red 300)

    divider = Color(0xFF333333)           // 深灰色分隔線
)

// 提供給全域使用的 Local 變數
val LocalBudgetQuestColors = staticCompositionLocalOf { LightColorPalette }

// 主題 Composable
@Composable
fun BudgetQuestTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorPalette else LightColorPalette

    CompositionLocalProvider(
        LocalBudgetQuestColors provides colors,
        content = content
    )
}

object AppTheme {
    val colors: BudgetQuestColors
        @Composable
        get() = LocalBudgetQuestColors.current
}