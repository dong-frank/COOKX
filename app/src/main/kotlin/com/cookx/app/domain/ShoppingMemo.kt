package com.cookx.app.domain

import kotlinx.serialization.Serializable

@Serializable data class PantryItem(val id: String, val familyId: String, val name: String, val available: Boolean = true)
@Serializable enum class PreparationSource { PANTRY, MANUAL, PENDING }
@Serializable data class ShoppingMemoItem(
    val key: String, val ingredient: String, val checked: Boolean = false,
    val source: PreparationSource = PreparationSource.PENDING,
)
@Serializable data class ShoppingMemo(val familyId: String, val recipeId: String, val items: List<ShoppingMemoItem>)

/** Parse only a recognizable amount suffix; never guess aliases or split mixed ingredients. */
fun shoppingIngredientName(line: String): String? {
    val value = line.trim()
    if (value.isEmpty() || value.any { it in "、,，;/；" }) return null
    val quantity = Regex("^(.+?)\\s*(?:[0-9]+(?:\\.[0-9]+)?\\s*(?:千克|公斤|毫升|克|斤|两|个|只|枚|瓣|根|把|袋|盒|勺|匙|杯|kg|ml|g|l)|适量|少许)\\s*$", RegexOption.IGNORE_CASE)
    return (quantity.matchEntire(value)?.groupValues?.get(1) ?: value).trim().takeIf { it.isNotEmpty() }
}

fun prepareShoppingMemo(recipe: Recipe, pantry: List<PantryItem>, previous: ShoppingMemo? = null,
    recheckPantry: Boolean = false): ShoppingMemo {
    require(previous == null || previous.recipeId == recipe.id && previous.familyId == recipe.familyId)
    val available = pantry.filter { it.familyId == recipe.familyId && it.available }.map { it.name.trim() }.toSet()
    val items = recipe.ingredients.mapIndexed { index, ingredient ->
        val key = "$index:$ingredient"
        val old = previous?.items?.find { it.key == key }
        if (old != null && (!recheckPantry || old.source == PreparationSource.MANUAL)) old else {
            val atHome = shoppingIngredientName(ingredient)?.let { it in available } == true
            ShoppingMemoItem(key, ingredient, atHome, if (atHome) PreparationSource.PANTRY else PreparationSource.PENDING)
        }
    }
    return ShoppingMemo(recipe.familyId, recipe.id, items)
}

fun ShoppingMemo.markPrepared(key: String, checked: Boolean): ShoppingMemo {
    require(items.any { it.key == key }) { "找不到这项食材，请重新打开便签" }
    return copy(items = items.map { if (it.key == key) it.copy(checked = checked, source = PreparationSource.MANUAL) else it })
}
