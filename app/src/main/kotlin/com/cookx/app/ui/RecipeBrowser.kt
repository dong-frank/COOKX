package com.cookx.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cookx.app.R
import com.cookx.app.domain.Recipe
import com.cookx.app.domain.DEFAULT_FAVORITE_TAGS
import com.cookx.app.domain.matchesTag
import com.cookx.app.domain.recipeCategories

/** Ordering-menu layout: independent category rail and an image/name-only recipe list. */
@Composable internal fun RecipeBrowser(recipes: List<Recipe>, busy: Boolean,
    examples: () -> Unit, open: (String) -> Unit, favoriteTags: List<String> = DEFAULT_FAVORITE_TAGS) {
    val categories = recipeCategories(recipes, favoriteTags)
    var selected by rememberSaveable { mutableStateOf("全部") }
    val active = selected.takeIf { it in categories } ?: "全部"
    LaunchedEffect(categories) { if (selected !in categories) selected = "全部" }
    val visible = recipes.filter { it.matchesTag(active) }
    val listState = rememberLazyListState()
    LaunchedEffect(active) { listState.scrollToItem(0) }
    Row(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.width(88.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainerLow)
            .testTag("recipeCategories"), contentPadding = PaddingValues(vertical = 10.dp)) {
            items(categories, key = { it }) { category ->
                val isSelected = category == active
                Row(Modifier.fillMaxWidth().heightIn(min = 64.dp)
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { selected = category }),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(3.dp).height(22.dp).background(
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow))
                    Text(category, Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxHeight().testTag("recipeList"), state = listState,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (visible.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(if (recipes.isEmpty()) "还没有菜谱" else "这个标签还没有菜谱", style = MaterialTheme.typography.titleMedium)
                    if (recipes.isEmpty()) OutlinedButton(onClick = examples, enabled = !busy) { Text("加入示例菜谱") }
                }
            }
            items(visible, key = { it.id }) { recipe ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button, onClick = { open(recipe.id) }).padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(when (recipe.id) {
                        "example-tomato-eggs" -> R.drawable.recipe_tomato_eggs
                        "example-mushrooms" -> R.drawable.recipe_mushrooms
                        else -> R.drawable.recipe_placeholder
                    }), contentDescription = null,
                        modifier = Modifier.size(96.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
                    Text(recipe.name, modifier = Modifier.weight(1f).padding(start = 14.dp, end = 2.dp),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
