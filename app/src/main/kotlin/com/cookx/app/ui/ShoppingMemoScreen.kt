package com.cookx.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.cookx.app.domain.*

@Composable internal fun ShoppingMemoScreen(recipe: Recipe, memo: ShoppingMemo, busy: Boolean,
    check: (String, Boolean) -> Unit, recheck: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("采购便签", style = MaterialTheme.typography.headlineMedium)
            Text(recipe.name, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleMedium)
            Text("已准备 ${memo.items.count { it.checked }} / ${memo.items.size}", Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("勾选表示已准备，不会更改库存或花费。", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
        }
        items(memo.items, key = { it.key }) { item ->
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(Modifier.fillMaxWidth().toggleable(value = item.checked, enabled = !busy, role = Role.Checkbox,
                    onValueChange = { check(item.key, it) }).testTag("shopping:${item.key}").padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = item.checked, onCheckedChange = null, enabled = !busy)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.ingredient, style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (item.checked) TextDecoration.LineThrough else null)
                        Text(when (item.source) {
                            PreparationSource.PANTRY -> "家中已有 · 请核对余量"
                            PreparationSource.MANUAL -> if (item.checked) "手动勾选" else "手动标记待准备"
                            PreparationSource.PENDING -> "待准备"
                        }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = recheck, enabled = !busy) { Text("按家中食材重新检查") }
            Text("在「囤囤鼠」登记家中已有食材后，可重新检查。手动选择会保留；家中有不代表数量足够。",
                Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable internal fun PantryScreen(state: KitchenState, busy: Boolean, add: (String) -> Unit,
    setAvailable: (String, Boolean) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    val pantry = state.pantryItems.filter { it.familyId == state.family?.id }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("囤囤鼠", style = MaterialTheme.typography.headlineMedium)
            Text("家中已有食材", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium)
            Text("登记家里有的食材，用来预选采购便签。这里只记录是否已有，暂不记录数量和采购花费。",
                Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
        }
        item {
            OutlinedTextField(name, { name = it.take(64) }, label = { Text("食材名称，例如：鸡蛋") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = { add(name) }, enabled = name.isNotBlank() && !busy, modifier = Modifier.padding(top = 10.dp)) { Text("登记家中已有") }
            Text("请使用与菜谱相同的食材名称；不会自动匹配替代品。", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall)
        }
        if (pantry.isEmpty()) item { Text("还没有登记食材。采购便签仍可手动勾选。", style = MaterialTheme.typography.bodyMedium) }
        items(pantry, key = { it.id }) { item ->
            Row(Modifier.fillMaxWidth().toggleable(item.available, enabled = !busy, role = Role.Checkbox,
                onValueChange = { setAvailable(item.id, it) }).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = item.available, onCheckedChange = null, enabled = !busy)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(item.name, style = MaterialTheme.typography.bodyLarge)
                    Text(if (item.available) "家中已有 · 数量未记录" else "暂时没有", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
