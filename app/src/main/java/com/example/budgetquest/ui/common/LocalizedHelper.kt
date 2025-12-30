package com.example.budgetquest.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.budgetquest.R

// [修改] 接收 resourceKey (優先) 和 rawName (後備)
@Composable
fun getSmartCategoryName(rawName: String, resourceKey: String? = null): String {
    // 1. 優先使用 resourceKey 判斷
    if (resourceKey != null) {
        return when (resourceKey) {
            "cat_food" -> stringResource(R.string.cat_food)
            "cat_shopping" -> stringResource(R.string.cat_shopping)
            "cat_transport" -> stringResource(R.string.cat_transport)
            "cat_home" -> stringResource(R.string.cat_home)
            "cat_entertainment" -> stringResource(R.string.cat_entertainment)
            "cat_medical" -> stringResource(R.string.cat_medical)
            "cat_education" -> stringResource(R.string.cat_education)
            "cat_bills" -> stringResource(R.string.cat_bills)
            "cat_investment" -> stringResource(R.string.cat_investment)
            "cat_other" -> stringResource(R.string.cat_other)
            "cat_travel" -> stringResource(R.string.cat_travel)
            else -> rawName // 找不到 Key 對應的翻譯，顯示原名
        }
    }

    // 2. 相容舊邏輯 (依賴文字比對，防止舊資料顯示錯誤)
    return when (rawName) {
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
        else -> rawName
    }
}

// [修改] 接收 resourceKey 和 rawName
@Composable
fun getSmartTagName(rawName: String, resourceKey: String? = null): String {
    if (resourceKey != null) {
        return when (resourceKey) {
            "note_lunch" -> stringResource(R.string.note_lunch)
            "note_dinner" -> stringResource(R.string.note_dinner)
            "note_drink" -> stringResource(R.string.note_drink)
            "note_breakfast" -> stringResource(R.string.note_breakfast)
            "note_bus" -> stringResource(R.string.note_bus)
            "note_mrt" -> stringResource(R.string.note_mrt)
            "note_gas" -> stringResource(R.string.note_gas)
            "note_movie" -> stringResource(R.string.note_movie)
            "note_game" -> stringResource(R.string.note_game)
            "note_daily_use" -> stringResource(R.string.note_daily_use)
            else -> rawName
        }
    }

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