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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.budgetquest.R
import com.example.budgetquest.data.CategoryEntity
import com.example.budgetquest.data.TagEntity
import com.example.budgetquest.data.SubscriptionTagEntity
import com.example.budgetquest.ui.common.GlassCard
import com.example.budgetquest.ui.common.GlassIconButton
import com.example.budgetquest.ui.common.GlassTextField
import com.example.budgetquest.ui.common.getIconByKey
import com.example.budgetquest.ui.common.getSmartCategoryName
import com.example.budgetquest.ui.common.getSmartTagName
import com.example.budgetquest.ui.theme.AppTheme

val PRESET_COLORS = listOf(
    "#EF5350", "#EC407A", "#AB47BC", "#7E57C2",
    "#5C6BC0", "#42A5F5", "#29B6F6", "#26C6DA",
    "#26A69A", "#66BB6A", "#9CCC65", "#D4E157",
    "#FFCA28", "#FFA726", "#FF7043", "#8D6E63",
    "#BDBDBD", "#78909C"
)

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
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 650.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.title_manage_categories), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                    GlassIconButton(onClick = onDismiss, size = 32.dp) {
                        Icon(Icons.Default.Close, null, tint = AppTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isAdding) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            // [調整] 降低透明度 (0.5 -> 0.3)，讓背景更通透
                            .background(AppTheme.colors.surface.copy(alpha = 0.3f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = { Text(stringResource(R.string.hint_category_name), color = AppTheme.colors.textSecondary, fontSize = 14.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = AppTheme.colors.background.copy(alpha = 0.3f), // [調整] 降低
                                focusedContainerColor = AppTheme.colors.background.copy(alpha = 0.5f),   // [調整] 降低
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = AppTheme.colors.accent,
                                focusedTextColor = AppTheme.colors.textPrimary,
                                unfocusedTextColor = AppTheme.colors.textPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                            leadingIcon = {
                                Icon(
                                    imageVector = getIconByKey(selectedIcon),
                                    contentDescription = null,
                                    tint = Color(android.graphics.Color.parseColor(selectedColor)),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )

                        Column {
                            Text(stringResource(R.string.label_select_icon), fontSize = 12.sp, color = AppTheme.colors.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(iconList) { iconKey ->
                                    val isSelected = selectedIcon == iconKey
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) AppTheme.colors.accent.copy(alpha = 0.1f) else Color.Transparent)
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.dp,
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

                        Column {
                            Text(stringResource(R.string.label_select_color), fontSize = 12.sp, color = AppTheme.colors.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(colorList) { colorHex ->
                                    val color = Color(android.graphics.Color.parseColor(colorHex))
                                    val isSelected = selectedColor == colorHex
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(28.dp)
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

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    isAdding = false
                                    newName = ""
                                    selectedIcon = "STAR"
                                    selectedColor = PRESET_COLORS[0]
                                },
                                modifier = Modifier.weight(1f).height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.background.copy(alpha = 0.3f), contentColor = AppTheme.colors.textPrimary), // [調整] 降低
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) {
                                Text(stringResource(R.string.action_cancel), fontSize = 14.sp)
                            }

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
                                modifier = Modifier.weight(1f).height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accent),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.action_confirm), color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = { isAdding = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.surface.copy(alpha = 0.4f), contentColor = AppTheme.colors.accent), // [調整] 降低
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_add_category))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories, key = { it.id }) { item ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 12.dp,
                            // [移除] 這裡不需要額外的 onClick，除非需要點擊整行
                        ) {
                            // [重要調整] 這裡原本沒有設定背景，GlassCard 會套用預設的玻璃背景。
                            // 但因為是在 Dialog 內部 (已有背景)，雙重玻璃可能會太厚。
                            // 這裡我們維持 GlassCard 的結構，但因為 GlassCard 預設是半透明，
                            // 在深色 Dialog 上應該會剛好。
                            // 如果您覺得列表項目還是太深，可以考慮使用更輕的背景色覆盖，例如：
                            // Box(modifier = Modifier.background(Color.Transparent)) // 但 GlassCard 預設有背景

                            // 這裡使用 GlassCard 已經是最佳解，因為它比之前手寫的 alpha=0.7f (Row background) 要輕。
                            // GlassCard 的預設 alpha 是 0.65 -> 0.35。

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    // 移除之前手動設定的 background(alpha=0.7f)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .background(
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
                                    Text(getSmartCategoryName(item.name, item.resourceKey), color = if (item.isVisible) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary, fontSize = 14.sp)
                                }

                                Row {
                                    GlassIconButton(onClick = { debounce { onToggleVisibility(item) } }, size = 32.dp) {
                                        Icon(
                                            if (item.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            null,
                                            tint = if (item.isVisible) AppTheme.colors.accent else AppTheme.colors.textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    GlassIconButton(onClick = { debounce { onDelete(item) } }, size = 32.dp) {
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
}

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
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.title_manage_tags), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                    GlassIconButton(onClick = onDismiss, size = 32.dp) { Icon(Icons.Default.Close, null, tint = AppTheme.colors.textSecondary, modifier = Modifier.size(18.dp)) }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isAdding) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            // [調整] 降低透明度 0.5 -> 0.3
                            .background(AppTheme.colors.surface.copy(alpha = 0.3f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = stringResource(R.string.hint_new_tag),
                            label = null
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { isAdding = false; newName = "" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.background.copy(alpha = 0.3f), contentColor = AppTheme.colors.textPrimary), // [調整] 降低
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) {
                                Text(stringResource(R.string.action_cancel))
                            }

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
                            ) {
                                Text(stringResource(R.string.action_confirm), color = Color.White)
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = { isAdding = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.surface.copy(alpha = 0.4f), contentColor = AppTheme.colors.accent), // [調整] 降低
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_add_tag))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tags, key = { it.id }) { item ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 12.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(getSmartTagName(item.name, item.resourceKey), color = if(item.isVisible) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary)
                                Row {
                                    GlassIconButton(onClick = { debounce { onToggleVisibility(item) } }, size = 32.dp) {
                                        Icon(if (item.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = if(item.isVisible) AppTheme.colors.accent else AppTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
                                    }
                                    GlassIconButton(onClick = { debounce { onDelete(item) } }, size = 32.dp) {
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
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.title_manage_subscriptions), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
                    GlassIconButton(onClick = onDismiss, size = 32.dp) { Icon(Icons.Default.Close, null, tint = AppTheme.colors.textSecondary, modifier = Modifier.size(18.dp)) }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isAdding) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            // [調整] 降低透明度 0.5 -> 0.3
                            .background(AppTheme.colors.surface.copy(alpha = 0.3f))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            placeholder = stringResource(R.string.hint_subscription_example),
                            label = null
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { isAdding = false; newName = "" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.background.copy(alpha = 0.3f), contentColor = AppTheme.colors.textPrimary), // [調整] 降低
                                elevation = ButtonDefaults.buttonElevation(0.dp)
                            ) {
                                Text(stringResource(R.string.action_cancel))
                            }

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
                            ) {
                                Text(stringResource(R.string.action_confirm), color = Color.White)
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = { isAdding = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.surface.copy(alpha = 0.4f), contentColor = AppTheme.colors.accent), // [調整] 降低
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_add_subscription))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tags, key = { it.id }) { item ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 12.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(getSmartCategoryName(item.name, item.resourceKey), color = if(item.isVisible) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary)
                                Row {
                                    GlassIconButton(onClick = { debounce { onToggleVisibility(item) } }, size = 32.dp) {
                                        Icon(if (item.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = if(item.isVisible) AppTheme.colors.accent else AppTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
                                    }
                                    GlassIconButton(onClick = { debounce { onDelete(item) } }, size = 32.dp) {
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
}