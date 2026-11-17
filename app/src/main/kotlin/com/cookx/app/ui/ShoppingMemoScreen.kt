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
        }
        items(memo.items, key = { it.key }) { item ->
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(value = item.checked, enabled = !busy, role = Role.Checkbox,
                    onValueChange = { check(item.key, it) }).testTag("shopping:${item.key}").padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = item.checked, onCheckedChange = null, enabled = !busy)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.ingredient, style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (item.checked) TextDecoration.LineThrough else null)

                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = recheck, enabled = !busy) { Text("重新检查") }
        }
    }
}
