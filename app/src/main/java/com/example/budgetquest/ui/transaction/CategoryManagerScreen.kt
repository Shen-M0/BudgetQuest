package com.example.budgetquest.ui.transaction

import androidx.compose.foundation.background
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
import com.example.budgetquest.ui.common.CategoryIcon
import com.example.budgetquest.ui.common.getIconByKey

// 日系配色
private val JapaneseBg = Color(0xFFF7F9FC)
private val JapaneseSurface = Color.White
private val JapaneseTextPrimary = Color(0xFF455A64)
private val JapaneseTextSecondary = Color(0xFF90A4AE)
private val JapaneseAccent = Color(0xFF78909C)

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
    var selectedIcon by remember { mutableStateOf(CategoryIcon.FACE) }
    var selectedColor by remember { mutableStateOf(Color(0xFFFFAB91)) }

    val pastelColors = listOf(
        Color(0xFFFFAB91), Color(0xFF90CAF9), Color(0xFFFFF59D),
        Color(0xFFCE93D8), Color(0xFFA5D6A7), Color(0xFFEF9A9A)
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = JapaneseBg),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("管理分類", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = JapaneseTextPrimary)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = JapaneseTextSecondary) }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 新增區塊
                if (isAdding) {
                    Column(
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(JapaneseSurface).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = newName, onValueChange = { newName = it },
                            placeholder = { Text("分類名稱") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = JapaneseBg, focusedContainerColor = JapaneseBg,
                                unfocusedBorderColor = Color.Transparent, focusedBorderColor = JapaneseAccent
                            )
                        )
                        // 圖示選擇
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(CategoryIcon.values()) { iconEnum ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if(selectedIcon == iconEnum) JapaneseAccent else JapaneseBg)
                                        .clickable { selectedIcon = iconEnum },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(iconEnum.icon, null, tint = if(selectedIcon == iconEnum) Color.White else JapaneseTextSecondary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        // 顏色選擇
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(pastelColors) { color ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { selectedColor = color }
                                ) {
                                    if(selectedColor == color) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = {
                                onAddCategory(newName, selectedIcon.key, String.format("#%06X", (0xFFFFFF and selectedColor.toArgb())))
                                isAdding = false
                                newName = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = JapaneseAccent),
                            enabled = newName.isNotBlank()
                        ) { Text("確認新增") }
                    }
                } else {
                    Button(
                        onClick = { isAdding = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = JapaneseSurface, contentColor = JapaneseAccent),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("新增分類") }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 列表
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(JapaneseSurface).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // 顏色點
                                Box(modifier = Modifier.size(8.dp).background(Color(android.graphics.Color.parseColor(item.colorHex)), CircleShape))
                                Spacer(modifier = Modifier.width(12.dp))
                                // 圖示
                                Icon(getIconByKey(item.iconKey), null, tint = JapaneseTextSecondary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(item.name, color = if(item.isVisible) JapaneseTextPrimary else Color.LightGray)
                            }

                            Row {
                                // 隱藏/顯示按鈕
                                IconButton(onClick = { onToggleVisibility(item) }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        if (item.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        null,
                                        tint = if(item.isVisible) JapaneseAccent else Color.LightGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                // 刪除按鈕 (預設項目不能刪除)
                                if (!item.isDefault) {
                                    IconButton(onClick = { onDelete(item) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Delete, null, tint = Color(0xFFEF9A9A), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 標籤管理 Dialog (邏輯類似，簡化版)
@Composable
fun TagManagerDialog(
    tags: List<TagEntity>,
    onDismiss: () -> Unit,
    onAddTag: (String) -> Unit,
    onToggleVisibility: (TagEntity) -> Unit,
    onDelete: (TagEntity) -> Unit
) {
    // [新增] 控制輸入框展開的狀態
    var isAdding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }



    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = JapaneseBg),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("管理備註", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF455A64))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color(0xFF90A4AE)) }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // [修改重點] 根據 isAdding 決定顯示按鈕還是輸入框
                if (isAdding) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(JapaneseSurface)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = { Text("輸入新備註...", color = Color.LightGray) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = JapaneseBg,
                                focusedContainerColor = JapaneseBg,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = JapaneseAccent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 取消按鈕
                            Button(
                                onClick = { isAdding = false; newName = "" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = JapaneseSurface, contentColor = JapaneseAccent),
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) { Text("取消") }

                            // 確認按鈕
                            Button(
                                onClick = {
                                    onAddTag(newName)
                                    newName = ""
                                    isAdding = false // 新增後收起，或保持展開看需求
                                },
                                enabled = newName.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = JapaneseAccent),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("確認") }
                        }
                    }
                } else {
                    // [新增] 預設顯示的新增按鈕
                    Button(
                        onClick = { isAdding = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = JapaneseSurface, contentColor = JapaneseAccent),
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
                    items(tags) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(JapaneseSurface)
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.name, color = if(item.isVisible) Color(0xFF455A64) else Color.LightGray)
                            Row {
                                IconButton(onClick = { onToggleVisibility(item) }, modifier = Modifier.size(32.dp)) {
                                    Icon(if (item.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = if(item.isVisible) JapaneseAccent else Color.LightGray, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onDelete(item) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, null, tint = Color(0xFFEF9A9A), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// [修改] SubTagManagerDialog (訂閱備註) - 邏輯完全相同
@Composable
fun SubTagManagerDialog(
    tags: List<com.example.budgetquest.data.SubscriptionTagEntity>,
    onDismiss: () -> Unit,
    onAddTag: (String) -> Unit,
    onToggleVisibility: (com.example.budgetquest.data.SubscriptionTagEntity) -> Unit,
    onDelete: (com.example.budgetquest.data.SubscriptionTagEntity) -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }


    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = JapaneseBg),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("管理訂閱項目", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF455A64))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color(0xFF90A4AE)) }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isAdding) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(JapaneseSurface)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = { Text("例如: Netflix...", color = Color.LightGray) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = JapaneseBg,
                                focusedContainerColor = JapaneseBg,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = JapaneseAccent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { isAdding = false; newName = "" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = JapaneseSurface, contentColor = JapaneseAccent),
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) { Text("取消") }

                            Button(
                                onClick = { onAddTag(newName); newName = ""; isAdding = false },
                                enabled = newName.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = JapaneseAccent),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("確認") }
                        }
                    }
                } else {
                    Button(
                        onClick = { isAdding = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = JapaneseSurface, contentColor = JapaneseAccent),
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
                    items(tags) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(JapaneseSurface)
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.name, color = if(item.isVisible) Color(0xFF455A64) else Color.LightGray)
                            Row {
                                IconButton(onClick = { onToggleVisibility(item) }, modifier = Modifier.size(32.dp)) {
                                    Icon(if (item.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = if(item.isVisible) JapaneseAccent else Color.LightGray, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onDelete(item) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, null, tint = Color(0xFFEF9A9A), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}