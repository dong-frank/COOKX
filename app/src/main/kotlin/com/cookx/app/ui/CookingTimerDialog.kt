package com.cookx.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import android.os.Build
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.util.Locale

internal fun timerText(seconds: Long) = String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)

@Composable internal fun CookingTimerDialog(defaultSeconds: Int, startCondition: String?, running: Boolean, remaining: Long,
    dismiss: () -> Unit, stop: () -> Unit, start: (Int) -> Unit) {
    var minutes by rememberSaveable { mutableIntStateOf(defaultSeconds / 60) }
    var seconds by rememberSaveable { mutableIntStateOf(defaultSeconds % 60) }
    val duration = minutes.toLong() * 60 + seconds
    AlertDialog(onDismissRequest = dismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerLow, title = { Text("计时器") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(timerText(if (running) remaining else duration),
                style = MaterialTheme.typography.displayMedium, modifier = Modifier.testTag("timerDisplay"))
            if (!running) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimerWheel(minutes, Int.MAX_VALUE / 60, "分", "timerMinutes", Modifier.weight(1f)) { minutes = it }
                    TimerWheel(seconds, 59, "秒", "timerSeconds", Modifier.weight(1f)) { seconds = it }
                }
                startCondition?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }, confirmButton = {
        if (running) TextButton(onClick = stop) { Text("停止计时") }
        else TextButton(onClick = { start(duration.toInt()) }, enabled = duration in 1..Int.MAX_VALUE) { Text("开始计时") }
    }, dismissButton = { TextButton(onClick = dismiss) { Text("关闭") } })
}

@Composable private fun TimerWheel(value: Int, maximum: Int, label: String, tag: String, modifier: Modifier,
    change: (Int) -> Unit) {
    val color = MaterialTheme.colorScheme.primary.toArgb()
    val onChange by rememberUpdatedState(change)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AndroidView(modifier = Modifier.fillMaxWidth().height(160.dp).testTag(tag), factory = { context ->
            NumberPicker(context).apply {
                minValue = 0
                maxValue = maximum
                wrapSelectorWheel = false
                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                contentDescription = label
                setOnValueChangedListener { _, _, next -> onChange(next) }
            }
        }, update = { picker ->
            if (picker.value != value) picker.value = value
            if (Build.VERSION.SDK_INT >= 29) picker.textColor = color
        })
        Text(label, style = MaterialTheme.typography.titleSmall)
    }
}
