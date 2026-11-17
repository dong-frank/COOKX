package com.cookx.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale

internal fun timerText(seconds: Long) = String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)

@Composable internal fun CookingTimerDialog(defaultSeconds: Int, startCondition: String?, running: Boolean, remaining: Long,
    dismiss: () -> Unit, stop: () -> Unit, start: (Int) -> Unit) {
    var minutes by rememberSaveable { mutableStateOf((defaultSeconds / 60).toString()) }
    var seconds by rememberSaveable { mutableStateOf((defaultSeconds % 60).toString()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val duration = minutes.toLongOrNull()?.let { min -> seconds.toLongOrNull()?.let { sec ->
        (min * 60 + sec).takeIf { min in 0..35_791_393 && sec in 0..59 && it in 1..Int.MAX_VALUE }
    } }
    AlertDialog(onDismissRequest = dismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerLow, title = { Text("计时器") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(timerText(if (running) remaining else duration ?: defaultSeconds.toLong()),
                style = MaterialTheme.typography.displayMedium, modifier = Modifier.testTag("timerDisplay"))
            if (!running) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(minutes, { minutes = it; error = null }, label = { Text("分") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                        modifier = Modifier.weight(1f).testTag("timerMinutes"))
                    OutlinedTextField(seconds, { seconds = it; error = null }, label = { Text("秒") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                        modifier = Modifier.weight(1f).testTag("timerSeconds"))
                }
                startCondition?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = {
        if (running) TextButton(onClick = stop) { Text("停止计时") }
        else TextButton(onClick = {
            if (duration == null) error = "请填写有效时长，秒数为 0–59"
            else start(duration.toInt())
        }) { Text("开始计时") }
    }, dismissButton = { TextButton(onClick = dismiss) { Text("关闭") } })
}
