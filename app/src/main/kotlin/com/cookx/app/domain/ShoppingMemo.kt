package com.cookx.app.domain

import kotlinx.serialization.Serializable

@Serializable data class PantryItem(val id: String, val familyId: String, val name: String, val available: Boolean = true)
@Serializable enum class PreparationSource { PANTRY, MANUAL, PENDING }
@Serializable data class ShoppingMemoItem(
    val key: String, val ingredient: String, val checked: Boolean = false,
    val source: PreparationSource = PreparationSource.PENDING,
)
@Serializable data class ShoppingMemo(val familyId: String, val recipeId: String, val items: List<ShoppingMemoItem>)

fun prepareShoppingMemo(recipe: Recipe, pantry: List<PantryItem>, previous: ShoppingMemo? = null,
    recheckPantry: Boolean = false): ShoppingMemo {
    require(previous == null || previous.recipeId == recipe.id && previous.familyId == recipe.familyId)
    val available = pantry.filter { it.familyId == recipe.familyId && it.available }.map { it.name.trim() }.toSet()
    val items = recipe.ingredients.map { ingredient ->
        val key = ingredient.id
        val old = previous?.items?.find { it.key == key }
        if (old != null && (!recheckPantry || old.source == PreparationSource.MANUAL)) old.copy(ingredient = ingredient.displayText()) else {
            val atHome = ingredient.name.trim() in available
            ShoppingMemoItem(key, ingredient.displayText(), atHome, if (atHome) PreparationSource.PANTRY else PreparationSource.PENDING)
        }
    }
    return ShoppingMemo(recipe.familyId, recipe.id, items)
}

fun ShoppingMemo.markPrepared(key: String, checked: Boolean): ShoppingMemo {
    require(items.any { it.key == key }) { "找不到这项食材，请重新打开便签" }
    return copy(items = items.map { if (it.key == key) it.copy(checked = checked, source = PreparationSource.MANUAL) else it })
}
