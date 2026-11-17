package com.cookx.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cookx.app.domain.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable internal fun MealControls(date: String, meal: Meal, changeDate: (String) -> Unit, changeMeal: (Meal) -> Unit) {
    var showDate by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TextButton(onClick = { showDate = true }, modifier = Modifier.testTag("cookingDate")) { Text("制作日期：$date") }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Meal.entries.forEach { item ->
                FilterChip(selected = meal == item, onClick = { changeMeal(item) }, label = { Text(item.label) }, modifier = Modifier.testTag("meal:${item.name}"))
            }
        }
    }
    if (showDate) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = dateToUtcMillis(date))
        DatePickerDialog(onDismissRequest = { showDate = false }, confirmButton = {
            TextButton(onClick = { picker.selectedDateMillis?.let { changeDate(utcMillisToDate(it)) }; showDate = false },
                enabled = picker.selectedDateMillis != null) { Text("确定日期") }
        }, dismissButton = { TextButton(onClick = { showDate = false }) { Text("取消") } }) { DatePicker(state = picker) }
    }
}
