package com.cookx.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.cookx.app.domain.*
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable internal fun PantryScreen(state: KitchenState, busy: Boolean,
    save: (PantryItem, () -> Unit) -> Unit, saveError: String? = null) {
    var destination by rememberSaveable { mutableStateOf("closed") }
    var panel by remember { mutableStateOf(destination) }
    val frame = remember { Animatable(if (destination == "fridge") 19f else 0f) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var draft by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(destination) {
        panel = "closed"
        val target = if (destination == "fridge") 19f else 0f
        frame.animateTo(target, tween((abs(target - frame.value) / 19f * 850).roundToInt()))
        panel = destination
    }
    val moving = frame.isRunning || panel != destination
    fun change(target: String) { selectedId = null; draft = null; destination = target }
    BackHandler(destination != "closed") {
        when { draft != null -> draft = null; selectedId != null -> selectedId = null; !moving -> change("closed") }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        PantryScene(frame.value.roundToInt(), !moving && !busy, panel == "spices",
            fridge = { change(if (destination == "fridge") "closed" else "fridge") },
            spices = { change(if (destination == "spices") "closed" else "spices") })
        if (panel != "closed") {
            val zone = if (panel == "fridge") PantryZone.FRIDGE else PantryZone.SPICES
            val items = state.pantryIn(zone)
            Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(minOf(260.dp, maxHeight * .45f)).testTag("pantryPanel:${zone.name}"),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp) {
                Column {
                    Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp).width(32.dp).height(4.dp), contentAlignment = Alignment.Center) {
                        Surface(Modifier.fillMaxSize(), shape = RoundedCornerShape(2.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .2f)) {}
                    }
                    val selected = items.find { it.id == selectedId }
                    Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (selected != null) TextButton(onClick = { selectedId = null }, modifier = Modifier.testTag("pantryBackToList")) { Text("‹ 返回") }
                        Spacer(Modifier.weight(1f))
                        FilledTonalIconButton(onClick = {
                            draft = Json.encodeToString(PantryItem(newId(), requireNotNull(state.family).id, "", zone = zone))
                        }, enabled = !busy, modifier = Modifier.testTag("addPantryItem").semantics {
                            contentDescription = if (zone == PantryZone.SPICES) "添加调料" else "添加物品"
                        }) { Text("+", style = MaterialTheme.typography.headlineSmall) }
                    }
                    if (selected == null) LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)) {
                        if (items.isEmpty()) item { Text("暂无物品", Modifier.padding(vertical = 20.dp)) }
                        items(items, key = { it.id }) { item ->
                            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable { selectedId = item.id }
                                .testTag("pantryItem:${item.id}").padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                PantryIcon(item)
                                Text(item.name, Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
                                Text(item.amountLabel(), Modifier.widthIn(max = 120.dp).padding(start = 12.dp), color = MaterialTheme.colorScheme.primary)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .3f))
                        }
                    } else Column(Modifier.padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            PantryIcon(selected)
                            Text(selected.name, Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.titleLarge)
                            Text(selected.amountLabel(), Modifier.widthIn(max = 120.dp).padding(start = 12.dp), color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(onClick = { draft = Json.encodeToString(selected) }, enabled = !busy) { Text("修改") }
                    }
                }
            }
        }
    }
    draft?.let { raw -> PantryItemForm(raw, busy, saveError, dismiss = { draft = null }) { item ->
        save(item) { draft = null; selectedId = null }
    } }
}

@Composable private fun PantryItemForm(raw: String, busy: Boolean, saveError: String?, dismiss: () -> Unit, save: (PantryItem) -> Unit) {
    val initial = remember(raw) { Json.decodeFromString<PantryItem>(raw) }
    var name by rememberSaveable(raw) { mutableStateOf(initial.name) }
    var amount by rememberSaveable(raw) { mutableStateOf(initial.quantityText.orEmpty()) }
    var error by rememberSaveable(raw) { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = { if (!busy) dismiss() }, containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        title = { Text(if (initial.name.isEmpty()) if (initial.zone == PantryZone.SPICES) "添加调料" else "添加物品" else "修改物品") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it; error = null }, label = { Text("名称") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("pantryName"), enabled = !busy)
            OutlinedTextField(amount, { amount = it; error = null }, label = { Text("数量／余量（选填）") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("pantryAmount"), enabled = !busy)
            (error ?: saveError)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } }, confirmButton = { TextButton(onClick = {
            runCatching { normalizedPantryItem(initial.copy(name = name, quantityText = amount)) }
                .onSuccess(save).onFailure { error = it.message }
        }, enabled = !busy) { Text("保存") } }, dismissButton = { TextButton(onClick = dismiss, enabled = !busy) { Text("取消") } })
}

@Composable private fun PantryIcon(item: PantryItem) {
    val symbol = when (item.name) {
        "番茄", "西红柿" -> "🍅"
        "胡萝卜" -> "🥕"
        "牛奶" -> "🥛"
        "鸡蛋" -> "🥚"
        "盐" -> "🧂"
        else -> if (item.zone == PantryZone.SPICES) "料" else "📦"
    }
    Surface(Modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Box(contentAlignment = Alignment.Center) { Text(symbol, style = MaterialTheme.typography.titleLarge) }
    }
}
