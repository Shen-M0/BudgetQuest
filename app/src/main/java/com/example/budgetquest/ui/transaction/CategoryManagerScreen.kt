package com.example.budgetquest.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.budgetquest.data.CategoryEntity
import com.example.budgetquest.data.TagEntity
import com.example.budgetquest.data.SubscriptionTagEntity
import com.example.budgetquest.ui.common.getIconByKey // 確保您有這個 helper function
import com.example.budgetquest.ui.theme.AppTheme

// [新增] 預定義的顏色列表 (Material Colors 500/400 level)
val PRESET_COLORS = listOf(
    "#EF5350", "#EC407A", "#AB47BC", "#7E57C2",
    "#5C6BC0", "#42A5F5", "#29B6F6", "#26C6DA",
    "#26A69A", "#66BB6A", "#9CCC65", "#D4E157",
    "#FFCA28", "#FFA726", "#FF7043", "#8D6E63",
    "#BDBDBD", "#78909C"
)

// [新增] 預定義的圖示 Key 列表 (對應 getIconByKey 的邏輯)
val PRESET_ICONS = listOf(
    "FOOD", "SHOPPING", "TRANSPORT", "HOME",
    "ENTERTAINMENT", "MEDICAL", "EDUCATION", "BILLS",
    "INVESTMENT", "OTHER", "STAR", "TRAVEL"
)

@Composable
fun CategoryManagerDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onAddCategory: (String, String, String) -> Unit,
    onToggleVisibility: (CategoryEntity) -> Unit,
    onDelete: (CategoryEntity) -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("STAR") }
    var selectedColor by remember { mutableStateOf(PRESET_COLORS[0]) }

    var lastClickTime by remember { mutableLongStateOf(0L) }
    fun debounce(action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 300L) {
            lastClickTime = now
            action()
        }
    }

    val iconList = remember { PRESET_ICONS }
    val colorList = remember { PRESET_COLORS }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.background),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 650.dp) // 高度微調
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // 標題列
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("管理分類", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = AppTheme.colors.textSecondary) }
                }

                Spacer(modifier = Modifier.height(12.dp)) // 間距微調

                // === 新增模式 ===
                if (isAdding) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppTheme.colors.surface)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp) // 間距縮小
                    ) {
                        // 1. 名稱輸入
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = { Text("分類名稱", color = AppTheme.colors.textSecondary, fontSize = 14.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = AppTheme.colors.background,
                                focusedContainerColor = AppTheme.colors.background,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = AppTheme.colors.accent,
                                focusedTextColor = AppTheme.colors.textPrimary,
                                unfocusedTextColor = AppTheme.colors.textPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp), // 字體微調
                            leadingIcon = {
                                Icon(
                                    imageVector = getIconByKey(selectedIcon),
                                    contentDescription = null,
                                    tint = Color(android.graphics.Color.parseColor(selectedColor)),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )

                        // 2. 圖示選擇器 (縮小版)
                        Column {
                            Text("選擇圖示", fontSize = 12.sp, color = AppTheme.colors.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { // 間距縮小
                                items(iconList) { iconKey ->
                                    val isSelected = selectedIcon == iconKey
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp) // [優化] 縮小尺寸 (原 40)
                                            .clip(CircleShape)
                                            .background(if (isSelected) AppTheme.colors.accent.copy(alpha = 0.1f) else Color.Transparent)
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.dp, // 邊框變細
                                                color = if (isSelected) AppTheme.colors.accent else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedIcon = iconKey }
                                    ) {
                                        Icon(
                                            imageVector = getIconByKey(iconKey),
                                            contentDescription = null,
                                            tint = if (isSelected) AppTheme.colors.accent else AppTheme.colors.textSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 3. 顏色選擇器 (縮小版)
                        Column {
                            Text("選擇顏色", fontSize = 12.sp, color = AppTheme.colors.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { // 間距縮小
                                items(colorList) { colorHex ->
                                    val color = Color(android.graphics.Color.parseColor(colorHex))
                                    val isSelected = selectedColor == colorHex
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(28.dp) // [優化] 縮小尺寸 (原 32)
                                            .clip(CircleShape)
                                            .background(color)
                                            .clickable { selectedColor = colorHex }
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 4. 按鈕群組
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    isAdding = false
                                    newName = ""
                                    selectedIcon = "STAR"
                                    selectedColor = PRESET_COLORS[0]
                                },
                                modifier = Modifier.weight(1f).height(40.dp), // 高度縮小
                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.background, contentColor = AppTheme.colors.textPrimary),
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) { Text("取消", fontSize = 14.sp) }

                            Button(
                                onClick = {
                                    debounce {
                                        onAddCategory(newName, selectedIcon, selectedColor)
                                        newName = ""
                                        isAdding = false
                                        selectedIcon = "STAR"
                                        selectedColor = PRESET_COLORS[0]
                                    }
                                },
                                enabled = newName.isNotBlank(),
                                modifier = Modifier.weight(1f).height(40.dp), // 高度縮小
                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accent),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("確認", color = Color.White, fontSize = 14.sp) }
                        }
                    }
                } else {
                    Button(
                        onClick = { isAdding = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.surface, contentColor = AppTheme.colors.accent),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("新增分類")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // === 列表顯示 ===
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppTheme.colors.surface)
                                .padding(horizontal = 12.dp, vertical = 10.dp), // Padding 微調
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp) // 列表圖示縮小
                                        .background(
                                            // [修正] 使用 _ 忽略例外變數，解決警告
                                            try { Color(android.graphics.Color.parseColor(item.colorHex)) } catch (_: Exception) { AppTheme.colors.accent.copy(alpha = 0.2f) },
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getIconByKey(item.iconKey),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(item.name, color = if (item.isVisible) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary, fontSize = 14.sp)
                            }

                            Row {
                                IconButton(onClick = { debounce { onToggleVisibility(item) } }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        if (item.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        null,
                                        tint = if (item.isVisible) AppTheme.colors.accent else AppTheme.colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(onClick = { debounce { onDelete(item) } }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, null, tint = AppTheme.colors.fail, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------
// 以下 TagManagerDialog 和 SubTagManagerDialog
// 維持之前的「防手震」優化版本即可，不需要加入顏色圖示選擇
// (除非您也想讓 Tag 有顏色，否則保持簡單文字即可)
// ----------------------------------------------------------------

@Composable
fun TagManagerDialog(
    tags: List<TagEntity>,
    onDismiss: () -> Unit,
    onAddTag: (String) -> Unit,
    onToggleVisibility: (TagEntity) -> Unit,
    onDelete: (TagEntity) -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    var lastClickTime by remember { mutableLongStateOf(0L) }
    fun debounce(action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 300L) {
            lastClickTime = now
            action()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.background),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("管理備註", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = AppTheme.colors.textSecondary) }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isAdding) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppTheme.colors.surface)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = { Text("輸入新備註...", color = AppTheme.colors.textSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = AppTheme.colors.background,
                                focusedContainerColor = AppTheme.colors.background,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = AppTheme.colors.accent,
                                focusedTextColor = AppTheme.colors.textPrimary,
                                unfocusedTextColor = AppTheme.colors.textPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { isAdding = false; newName = "" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.background, contentColor = AppTheme.colors.textPrimary),
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) { Text("取消") }

                            Button(
                                onClick = {
                                    debounce {
                                        onAddTag(newName)
                                        newName = ""
                                        isAdding = false
                                    }
                                },
                                enabled = newName.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accent),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("確認", color = Color.White) }
                        }
                    }
                } else {
                    Button(
                        onClick = { isAdding = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.surface, contentColor = AppTheme.colors.accent),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("新增備註")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tags, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppTheme.colors.surface)
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.name, color = if(item.isVisible) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary)
                            Row {
                                IconButton(onClick = { debounce { onToggleVisibility(item) } }, modifier = Modifier.size(32.dp)) {
                                    Icon(if (item.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = if(item.isVisible) AppTheme.colors.accent else AppTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { debounce { onDelete(item) } }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, null, tint = AppTheme.colors.fail, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubTagManagerDialog(
    tags: List<SubscriptionTagEntity>,
    onDismiss: () -> Unit,
    onAddTag: (String) -> Unit,
    onToggleVisibility: (SubscriptionTagEntity) -> Unit,
    onDelete: (SubscriptionTagEntity) -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    var lastClickTime by remember { mutableLongStateOf(0L) }
    fun debounce(action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 300L) {
            lastClickTime = now
            action()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.background),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("管理訂閱項目", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = AppTheme.colors.textSecondary) }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isAdding) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppTheme.colors.surface)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = { Text("例如: Netflix...", color = AppTheme.colors.textSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = AppTheme.colors.background,
                                focusedContainerColor = AppTheme.colors.background,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = AppTheme.colors.accent,
                                focusedTextColor = AppTheme.colors.textPrimary,
                                unfocusedTextColor = AppTheme.colors.textPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { isAdding = false; newName = "" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.background, contentColor = AppTheme.colors.textPrimary),
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) { Text("取消") }

                            Button(
                                onClick = {
                                    debounce {
                                        onAddTag(newName)
                                        newName = ""
                                        isAdding = false
                                    }
                                },
                                enabled = newName.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accent),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("確認", color = Color.White) }
                        }
                    }
                } else {
                    Button(
                        onClick = { isAdding = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.surface, contentColor = AppTheme.colors.accent),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("新增項目")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tags, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppTheme.colors.surface)
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.name, color = if(item.isVisible) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary)
                            Row {
                                IconButton(onClick = { debounce { onToggleVisibility(item) } }, modifier = Modifier.size(32.dp)) {
                                    Icon(if (item.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = if(item.isVisible) AppTheme.colors.accent else AppTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { debounce { onDelete(item) } }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, null, tint = AppTheme.colors.fail, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}