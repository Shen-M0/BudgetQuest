package com.example.budgetquest.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.budgetquest.R

@Composable
fun getSmartCategoryName(rawName: String, resourceKey: String? = null): String {
    // 1. 優先使用 resourceKey (如果有存的話)
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
            else -> rawName
        }
    }

    // 2. 多語言文字反查 (支援 繁中/英文/日文/簡中)
    // 只要 rawName 符合任一種語言的預設值，就回傳當前語言的 stringResource
    return when (rawName) {
        "飲食", "Food", "食事", "饮食" -> stringResource(R.string.cat_food)
        "購物", "Shopping", "買い物", "购物" -> stringResource(R.string.cat_shopping)
        "交通", "Transport", "交通" -> stringResource(R.string.cat_transport) // 日/簡/繁 交通相同
        "居家", "Home", "住まい", "居家" -> stringResource(R.string.cat_home)
        "娛樂", "Entertainment", "娯楽", "娱乐" -> stringResource(R.string.cat_entertainment)
        "醫療", "Medical", "医療", "医疗" -> stringResource(R.string.cat_medical)
        "教育", "Education", "教育" -> stringResource(R.string.cat_education) // 日/簡/繁 教育相同
        "帳單", "Bills", "請求書", "账单" -> stringResource(R.string.cat_bills)
        "投資", "Investment", "投資", "投资" -> stringResource(R.string.cat_investment)
        "其他", "Other", "その他", "其他" -> stringResource(R.string.cat_other)
        "旅遊", "Travel", "旅行", "旅游" -> stringResource(R.string.cat_travel)
        else -> rawName
    }
}

@Composable
fun getSmartTagName(rawName: String, resourceKey: String? = null): String {
    // 1. 優先使用 resourceKey
    if (resourceKey != null) {
        return when (resourceKey) {
            // 一般備註
            "note_breakfast" -> stringResource(R.string.note_breakfast)
            "note_lunch" -> stringResource(R.string.note_lunch)
            "note_dinner" -> stringResource(R.string.note_dinner)
            "note_drink" -> stringResource(R.string.note_drink)
            "note_bus" -> stringResource(R.string.note_bus)
            "note_mrt" -> stringResource(R.string.note_mrt)
            "note_gas" -> stringResource(R.string.note_gas)
            "note_movie" -> stringResource(R.string.note_movie)
            "note_game" -> stringResource(R.string.note_game)
            "note_daily_use" -> stringResource(R.string.note_daily_use)

            // 固定扣款/訂閱
            "sub_rent" -> stringResource(R.string.sub_rent)
            "sub_phone" -> stringResource(R.string.sub_phone)
            "sub_internet" -> stringResource(R.string.sub_internet)
            "sub_utilities" -> stringResource(R.string.sub_utilities)
            "sub_management_fee" -> stringResource(R.string.sub_management_fee)
            "sub_insurance" -> stringResource(R.string.sub_insurance)
            "sub_gym" -> stringResource(R.string.sub_gym)
            "sub_netflix" -> stringResource(R.string.sub_netflix)
            "sub_spotify" -> stringResource(R.string.sub_spotify)
            "sub_youtube_premium" -> stringResource(R.string.sub_youtube_premium)
            "sub_youtube_music" -> stringResource(R.string.sub_youtube_music)
            "sub_disney" -> stringResource(R.string.sub_disney)
            "sub_icloud" -> stringResource(R.string.sub_icloud)
            "sub_google_one" -> stringResource(R.string.sub_google_one)
            "sub_chatgpt" -> stringResource(R.string.sub_chatgpt)
            else -> rawName
        }
    }

    // 2. 多語言文字反查 (支援 繁中/英文/日文/簡中)
    return when (rawName) {
        // --- 一般備註 ---
        "早餐", "Breakfast", "朝食", "早餐" -> stringResource(R.string.note_breakfast)
        "午餐", "Lunch", "昼食", "午餐" -> stringResource(R.string.note_lunch)
        "晚餐", "Dinner", "夕食", "晚餐" -> stringResource(R.string.note_dinner)
        "飲料", "Drinks", "飲み物", "饮料" -> stringResource(R.string.note_drink)
        "公車", "Bus", "バス", "公交车" -> stringResource(R.string.note_bus)
        "捷運", "MRT", "地下鉄", "地铁" -> stringResource(R.string.note_mrt)
        "加油", "Gas", "ガソリン", "加油" -> stringResource(R.string.note_gas)
        "電影", "Movie", "映画", "电影" -> stringResource(R.string.note_movie)
        "遊戲", "Game", "ゲーム", "游戏" -> stringResource(R.string.note_game)
        "日用品", "Daily Necessities", "日用品", "日用品" -> stringResource(R.string.note_daily_use)

        // --- 固定扣款/訂閱 ---
        "房租", "Rent", "家賃", "房租" -> stringResource(R.string.sub_rent)
        "電話費", "Phone Bill", "携帯代", "话费" -> stringResource(R.string.sub_phone)
        "網路費", "Internet", "ネット回線", "网费" -> stringResource(R.string.sub_internet)
        "水電瓦斯", "Utilities", "光熱費", "水电煤" -> stringResource(R.string.sub_utilities)
        "管理費", "HOA Fee", "管理費", "物业费" -> stringResource(R.string.sub_management_fee)
        "保險", "Insurance", "保険", "保险" -> stringResource(R.string.sub_insurance)
        "健身房", "Gym", "ジム", "健身房" -> stringResource(R.string.sub_gym)

        // 數位服務 (通常各國語言都一樣，但為了保險起見還是列出)
        "Netflix" -> stringResource(R.string.sub_netflix)
        "Spotify" -> stringResource(R.string.sub_spotify)
        "YouTube Premium" -> stringResource(R.string.sub_youtube_premium)
        "YouTube Music" -> stringResource(R.string.sub_youtube_music)
        "Disney+" -> stringResource(R.string.sub_disney)
        "iCloud" -> stringResource(R.string.sub_icloud)
        "Google One" -> stringResource(R.string.sub_google_one)
        "ChatGPT" -> stringResource(R.string.sub_chatgpt)

        else -> rawName
    }
}