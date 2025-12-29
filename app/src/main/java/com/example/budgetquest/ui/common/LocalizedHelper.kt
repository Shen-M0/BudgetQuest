package com.example.budgetquest.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.budgetquest.R

@Composable
fun getSmartCategoryName(rawName: String): String {
    return when (rawName) {
        // 映射資料庫中的 "原始中文名稱" 到 "多語言資源"
        "飲食" -> stringResource(R.string.cat_food)
        "購物" -> stringResource(R.string.cat_shopping)
        "交通" -> stringResource(R.string.cat_transport)
        "居家" -> stringResource(R.string.cat_home)
        "娛樂" -> stringResource(R.string.cat_entertainment)
        "醫療" -> stringResource(R.string.cat_medical)
        "教育" -> stringResource(R.string.cat_education)
        "帳單" -> stringResource(R.string.cat_bills)
        "投資" -> stringResource(R.string.cat_investment)
        "其他" -> stringResource(R.string.cat_other)
        "旅遊" -> stringResource(R.string.cat_travel)
        else -> rawName // 如果是使用者自訂的名稱，就直接顯示
    }
}

@Composable
fun getSmartTagName(rawName: String): String {
    return when (rawName) {
        "午餐" -> stringResource(R.string.note_lunch)
        "晚餐" -> stringResource(R.string.note_dinner)
        "飲料" -> stringResource(R.string.note_drink)
        "早餐" -> stringResource(R.string.note_breakfast)
        "公車" -> stringResource(R.string.note_bus)
        "捷運" -> stringResource(R.string.note_mrt)
        "加油" -> stringResource(R.string.note_gas)
        "電影" -> stringResource(R.string.note_movie)
        "遊戲" -> stringResource(R.string.note_game)
        "日用品" -> stringResource(R.string.note_daily_use)
        else -> rawName
    }
}