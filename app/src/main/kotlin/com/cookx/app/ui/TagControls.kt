package com.cookx.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cookx.app.domain.*

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun TagSelector(available: List<String>, selected: List<String>, change: (List<String>) -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Text("菜谱标签（可多选）", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (available + selected).distinct().forEach { tag ->
            FilterChip(selected = tag in selected, onClick = {
                change(if (tag in selected) selected - tag else selected + tag)
            }, label = { Text(tag) })
        }
    }
    OutlinedTextField(input, { input = it }, label = { Text("自定义标签") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedButton(onClick = {
        runCatching { normalizeTags(listOf(input)).singleOrNull() ?: error("请填写标签名") }
            .onSuccess { change((selected + it).distinct()); input = ""; error = null }
            .onFailure { error = it.message }
    }) { Text("创建并添加") }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}

@Composable internal fun RecipeTagsEditor(recipe: Recipe, available: List<String>, busy: Boolean, save: (List<String>) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(ArrayList(recipe.tags)) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(recipe.name, style = MaterialTheme.typography.headlineMedium)
        TagSelector(available, selected) { selected = ArrayList(it) }
        Button(onClick = { save(selected) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("保存标签") }
    }
}

@Composable internal fun FamilyTagSettings(state: KitchenState, busy: Boolean, create: (String) -> Unit, save: (List<String>) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("家庭常用标签", style = MaterialTheme.typography.headlineMedium)
            Text("勾选要显示在首页左侧的标签，用上移、下移调整顺序。", Modifier.padding(top = 12.dp))
        }
        items(state.favoriteTags, key = { "favorite:$it" }) { tag ->
            val index = state.favoriteTags.indexOf(tag)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tag, Modifier.weight(1f).padding(top = 12.dp))
                TextButton(onClick = { save(state.favoriteTags.toMutableList().apply { add(index - 1, removeAt(index)) }) },
                    enabled = !busy && index > 0, modifier = Modifier.testTag("moveUp:$tag")) { Text("上移") }
                TextButton(onClick = { save(state.favoriteTags.toMutableList().apply { add(index + 1, removeAt(index)) }) },
                    enabled = !busy && index < state.favoriteTags.lastIndex, modifier = Modifier.testTag("moveDown:$tag")) { Text("下移") }
            }
        }
        item { HorizontalDivider(); Text("选择首页常用标签", Modifier.padding(top = 16.dp), style = MaterialTheme.typography.titleMedium) }
        items(state.availableTags(), key = { "available:$it" }) { tag ->
            Row {
                Checkbox(checked = tag in state.favoriteTags, enabled = !busy, modifier = Modifier.testTag("favorite:$tag"),
                    onCheckedChange = { checked -> save(if (checked) state.favoriteTags + tag else state.favoriteTags - tag) })
                Text(tag, Modifier.padding(top = 12.dp))
            }
        }
        item {
            OutlinedTextField(name, { name = it }, label = { Text("家庭自定义标签") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = { create(name) }, enabled = name.isNotBlank() && !busy, modifier = Modifier.padding(top = 12.dp)) { Text("创建标签") }
        }
    }
}
