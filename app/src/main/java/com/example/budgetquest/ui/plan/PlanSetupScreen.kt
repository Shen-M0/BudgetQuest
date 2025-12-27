package com.example.budgetquest.ui.plan

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.budgetquest.ui.AppViewModelProvider
import java.text.SimpleDateFormat
import java.util.*

// 日系配色
private val JapaneseBg = Color(0xFFF7F9FC)
private val JapaneseSurface = Color.White
private val JapaneseTextPrimary = Color(0xFF455A64)
private val JapaneseTextSecondary = Color(0xFF90A4AE)
private val JapaneseAccent = Color(0xFF78909C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanSetupScreen(
    planId: Int?,
    onPlanSaved: () -> Unit,
    onBackClick: () -> Unit = {},
    viewModel: PlanViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState = viewModel.formState

    // [新增] 取得錯誤訊息
    val errorMessage = viewModel.errorMessage

    val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

    val calculatedDailyLimit = remember(uiState) {
        val budget = uiState.totalBudget.toIntOrNull() ?: 0
        val savings = uiState.targetSavings.toIntOrNull() ?: 0
        val diff = uiState.endDate - uiState.startDate
        val days = (diff / (1000 * 60 * 60 * 24)).toInt() + 1
        val safeDays = if (days > 0) days else 1
        if (budget >= savings) (budget - savings) / safeDays else 0
    }

    Scaffold(
        containerColor = JapaneseBg,
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.id == 0) "建立新計畫" else "編輯計畫", color = JapaneseTextPrimary, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = JapaneseTextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JapaneseBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).padding(20.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // [新增] 計畫名稱輸入卡片
            Card(
                colors = CardDefaults.cardColors(containerColor = JapaneseSurface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // 這裡的 JapaneseInputRow 稍微改一下，因為計畫名稱不是數字
                    Text("計畫名稱", fontSize = 12.sp, color = JapaneseTextSecondary)
                    OutlinedTextField(
                        value = uiState.planName,
                        onValueChange = { viewModel.updateFormState(planName = it) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, color = JapaneseTextPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        ),
                        singleLine = true,
                        placeholder = { Text("我的存錢計畫", fontSize = 20.sp, color = Color.LightGray) }
                    )
                }
            }

            // 日期選擇區塊 (左右兩塊大卡片)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                JapaneseBigDateCard("開始日期", uiState.startDate, Modifier.weight(1f)) { viewModel.updateFormState(startDate = it) }
                JapaneseBigDateCard("結束日期", uiState.endDate, Modifier.weight(1f)) { viewModel.updateFormState(endDate = it) }
            }

            // 金額輸入
            Card(
                colors = CardDefaults.cardColors(containerColor = JapaneseSurface),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    JapaneseInputRow("總預算", uiState.totalBudget) { viewModel.updateFormState(totalBudget = it) }
                    HorizontalDivider(color = JapaneseBg, thickness = 1.dp)
                    JapaneseInputRow("目標存錢", uiState.targetSavings) { viewModel.updateFormState(targetSavings = it) }
                }
            }

            // 試算結果
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1)), // 極淡的綠色
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("每日可用", fontSize = 14.sp, color = JapaneseTextSecondary)
                        Text("$ $calculatedDailyLimit", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00695C))
                    }
                    Icon(Icons.Default.DateRange, null, tint = Color(0xFF80CBC4), modifier = Modifier.size(40.dp))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // [新增] 顯示錯誤訊息 (放在按鈕上方)
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = { viewModel.savePlanWithCallback { success ->
                    if (success) onPlanSaved()
                } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JapaneseAccent),
                shape = RoundedCornerShape(16.dp),
                enabled = (uiState.totalBudget.isNotBlank() && calculatedDailyLimit > 0)
            ) {
                Text("開始計畫", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun JapaneseBigDateCard(label: String, dateMillis: Long, modifier: Modifier, onDateSelected: (Long) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
    val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
    val datePickerDialog = DatePickerDialog(context, { _, y, m, d -> calendar.set(y, m, d); onDateSelected(calendar.timeInMillis) }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

    Card(
        colors = CardDefaults.cardColors(containerColor = JapaneseSurface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = modifier.clickable { datePickerDialog.show() }
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 12.sp, color = JapaneseTextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(dateFormat.format(calendar.time), fontSize = 24.sp, fontWeight = FontWeight.Medium, color = JapaneseTextPrimary)
        }
    }
}

@Composable
fun JapaneseInputRow(label: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(label, fontSize = 12.sp, color = JapaneseTextSecondary)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, color = JapaneseTextPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            placeholder = { Text("0", fontSize = 20.sp, color = Color.LightGray) }
        )
    }
}