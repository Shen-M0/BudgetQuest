package com.example.budgetquest.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.budgetquest.data.CategoryEntity
import com.example.budgetquest.data.TagEntity
import com.example.budgetquest.data.SubscriptionTagEntity
import com.example.budgetquest.ui.theme.AppTheme

@Composable
fun CategoryManagerDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onAddCategory: (String) -> Unit,
    onToggleVisibility: (CategoryEntity) -> Unit,
    onDelete: (CategoryEntity) -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    // [優化] 防手震
    var lastClickTime by remember { mutableLongStateOf(0L) }
    fun debounce(action: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastClickTime > 300L) { // Dialog 操作頻率較高，設為 300ms 即可
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("管理分類", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
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
                            placeholder = { Text("輸入新分類...", color = AppTheme.colors.textSecondary) },
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
                                        onAddCategory(newName)
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
                        Text("新增分類")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // [優化] 使用 id 作為 key，提升列表效能
                    items(categories, key = { it.id }) { item ->
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
                                    Icon(
                                        if (item.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        null,
                                        tint = if(item.isVisible) AppTheme.colors.accent else AppTheme.colors.textSecondary,
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

// 標籤管理 Dialog (結構與上面完全相同，同樣套用防手震與 key)
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
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(AppTheme.colors.surface).padding(16.dp),
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
                    // [優化] 使用 id 作為 key
                    items(tags, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AppTheme.colors.surface).padding(horizontal = 12.dp, vertical = 12.dp),
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

// 訂閱管理 Dialog (結構完全相同，只需套用上面的優化邏輯)
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
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(AppTheme.colors.surface).padding(16.dp),
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
                    // [優化] 使用 id 作為 key
                    items(tags, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AppTheme.colors.surface).padding(horizontal = 12.dp, vertical = 12.dp),
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