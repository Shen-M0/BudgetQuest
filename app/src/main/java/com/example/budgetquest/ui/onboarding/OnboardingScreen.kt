package com.example.budgetquest.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush // [新增]
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.budgetquest.R
import com.example.budgetquest.ui.theme.AppTheme
import kotlinx.coroutines.launch

// [美術] 定義玻璃筆刷 (圓形專用)
@Composable
private fun getCircleGlassBrush(): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            AppTheme.colors.surface.copy(alpha = 0.5f),
            AppTheme.colors.surface.copy(alpha = 0.2f)
        )
    )
}

@Composable
private fun getCircleBorderBrush(): Brush {
    return Brush.linearGradient(
        colors = listOf(
            AppTheme.colors.textPrimary.copy(alpha = 0.3f),
            AppTheme.colors.textPrimary.copy(alpha = 0.1f)
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val title1 = stringResource(R.string.onboarding_page1_title)
    val desc1 = stringResource(R.string.onboarding_page1_desc)
    val title2 = stringResource(R.string.onboarding_page2_title)
    val desc2 = stringResource(R.string.onboarding_page2_desc)
    val title3 = stringResource(R.string.onboarding_page3_title)
    val desc3 = stringResource(R.string.onboarding_page3_desc)

    val pages = remember(title1, desc1, title2, desc2, title3, desc3) {
        listOf(
            OnboardingPage(title = title1, description = desc1),
            OnboardingPage(title = title2, description = desc2),
            OnboardingPage(title = title3, description = desc3)
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    var lastClickTime by remember { mutableLongStateOf(0L) }
    fun debounce(action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 500L) {
            lastClickTime = now
            action()
        }
    }

    // [美術] 取得筆刷
    val glassBrush = getCircleGlassBrush()
    val borderBrush = getCircleBorderBrush()

    Column(
        modifier = Modifier
            .fillMaxSize()
            // [美術] 移除背景色，讓底層極光透出來
            // .background(AppTheme.colors.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { index ->
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // [美術] 圓形圖示改為玻璃擬態
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(glassBrush)
                        .border(1.dp, borderBrush, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.accent.copy(alpha = 0.8f) // 加深一點顏色以增加對比
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = pages[index].title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = pages[index].description,
                    fontSize = 16.sp,
                    color = AppTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pages.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) AppTheme.colors.accent else AppTheme.colors.textSecondary.copy(alpha = 0.3f)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(color)
                            .size(10.dp)
                    )
                }
            }

            Button(
                onClick = {
                    debounce {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onFinish()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accent),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp, // [美術] 增加陰影，讓按鈕更立體
                    pressedElevation = 2.dp
                )
            ) {
                if (pagerState.currentPage < pages.size - 1) {
                    Icon(Icons.Default.ArrowForward, null, tint = Color.White)
                } else {
                    Text(stringResource(R.string.btn_get_started), color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Check, null, tint = Color.White)
                }
            }
        }
    }
}

data class OnboardingPage(val title: String, val description: String)