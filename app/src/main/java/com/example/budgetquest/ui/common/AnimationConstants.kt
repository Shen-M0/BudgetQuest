package com.example.budgetquest.ui.common

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

/**
 * 定義全域共用的轉場動畫參數
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val FluidBoundsTransform = BoundsTransform { _, _ ->
    // [修正] 改用標準的 tween，因為 arcTween 不是標準 API
    // 這裡設定 400ms 的持續時間與緩動曲線，讓轉場看起來流暢
    tween(durationMillis = 400, easing = FastOutSlowInEasing)
}