package com.cookx.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cookx.app.domain.*

internal fun updateTemplateValue(template: OperationTemplate, values: Map<String, TemplateValue>, id: String, value: TemplateValue): Map<String, TemplateValue> {
    val next = values.toMutableMap().apply { put(id, value) }
    template.parameters.filter { it.ingredientListParameter == id }.forEach { order ->
        val selected = value.ingredients.map { it.ingredientId }
        val previous = next[order.id] ?: TemplateValue()
        next[order.id] = previous.copy(order = previous.order.filter { it in selected } + selected.filterNot { it in previous.order })
    }
    return next
}

@Composable internal fun TemplateFields(template: OperationTemplate, values: Map<String, TemplateValue>,
    ingredients: List<RecipeIngredient>, update: (String, TemplateValue) -> Unit) {
    template.parameters.filter { it.isEnabled(values) }.forEach { parameter -> key(parameter.id) {
        val value = values[parameter.id] ?: TemplateValue(text = parameter.defaultText.orEmpty())
        when (parameter.type) {
            "boolean" -> Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(value.enabled, { update(parameter.id, value.copy(enabled = it)) }, Modifier.testTag("templateFlag:${parameter.id}"))
                Text(parameter.name, Modifier.padding(start = 10.dp))
            }
            "ingredient-use", "ingredient-use-list" -> {
                Text(parameter.name, style = MaterialTheme.typography.titleSmall)
                if (parameter.type == "ingredient-use") {
                    var open by remember { mutableStateOf(false) }
                    val selected = value.ingredients.singleOrNull()
                    Box {
                        OutlinedButton(onClick = { open = true }, modifier = Modifier.testTag("templateSelect:${parameter.id}")) {
                            Text(ingredients.find { it.id == selected?.ingredientId }?.name ?: "选择食材")
                        }
                        DropdownMenu(open, { open = false }) {
                            DropdownMenuItem(text = { Text("不选择") }, onClick = { update(parameter.id, value.copy(ingredients = emptyList())); open = false })
                            ingredients.forEach { ingredient -> DropdownMenuItem(text = { Text(ingredient.name) },
                                modifier = Modifier.testTag("templateIngredient:${parameter.id}:${ingredient.name}"), onClick = {
                                    update(parameter.id, value.copy(ingredients = listOf(StepIngredientUse(ingredient.id)))); open = false
                                }) }
                        }
                    }
                } else ingredients.forEach { ingredient ->
                    val selected = value.ingredients.any { it.ingredientId == ingredient.id }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(selected, { checked -> update(parameter.id, value.copy(ingredients = if (checked) value.ingredients + StepIngredientUse(ingredient.id) else value.ingredients.filterNot { it.ingredientId == ingredient.id })) },
                            Modifier.testTag("templateUse:${parameter.id}:${ingredient.name}"))
                        Text(ingredient.name)
                    }
                }
                value.ingredients.forEach { use ->
                    val name = ingredients.find { it.id == use.ingredientId }?.name.orEmpty()
                    OutlinedTextField(use.quantityText.orEmpty(), { text -> update(parameter.id, value.copy(ingredients = value.ingredients.map {
                        if (it.ingredientId == use.ingredientId) it.copy(quantityText = text.ifBlank { null }) else it
                    })) }, label = { Text("${name}本步用量${if (parameter.quantityRequired) "" else "（选填）"}") },
                        singleLine = true, modifier = Modifier.fillMaxWidth().testTag("templateAmount:${parameter.id}:$name"))
                }
            }
            "ingredient-order" -> {
                Text(parameter.name, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!value.ordered, { update(parameter.id, value.copy(ordered = false)) }, label = { Text("一起下") })
                    FilterChip(value.ordered, { update(parameter.id, value.copy(ordered = true)) }, label = { Text("按顺序下") })
                }
                if (value.ordered) value.order.forEachIndexed { index, id ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(ingredients.find { it.id == id }?.name.orEmpty(), Modifier.weight(1f))
                        TextButton(onClick = { update(parameter.id, value.copy(order = value.order.toMutableList().apply { add(index - 1, removeAt(index)) })) }, enabled = index > 0,
                            modifier = Modifier.testTag("templateOrderUp:$id")) { Text("上移") }
                        TextButton(onClick = { update(parameter.id, value.copy(order = value.order.toMutableList().apply { add(index + 1, removeAt(index)) })) }, enabled = index < value.order.lastIndex) { Text("下移") }
                    }
                }
            }
            else -> OutlinedTextField(value.text, { update(parameter.id, value.copy(text = it)) },
                label = { Text(parameter.name + if (parameter.type == "duration") "（分钟，选填）" else if (!(parameter.required || parameter.requiredWhenEnabled || parameter.requiredWhen?.matches(values) == true)) "（选填）" else "") },
                singleLine = true, modifier = Modifier.fillMaxWidth().testTag("templateText:${parameter.id}"))
        }
    } }
}
