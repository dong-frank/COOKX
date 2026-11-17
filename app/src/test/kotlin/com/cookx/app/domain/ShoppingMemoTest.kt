package com.cookx.app.domain

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ShoppingMemoTest {
    private val recipe = exampleRecipes("f").first()
    @Test fun pantryUsesStructuredNameAndSameHouseholdOnly() {
        val memo = prepareShoppingMemo(recipe, listOf(PantryItem("a", "f", "番茄"), PantryItem("b", "other", "鸡蛋")))
        assertEquals(listOf(true, false, false, false), memo.items.map { it.checked })
        assertEquals("tomato", memo.items.first().key)
        assertEquals("番茄 2个", memo.items.first().ingredient)
    }
    @Test fun manualChoicesPersistAndPantryRecheckNeverOverwritesThem() {
        val memo = prepareShoppingMemo(recipe, listOf(PantryItem("a", "f", "番茄")))
            .markPrepared("tomato", false).markPrepared("egg", true)
        val rechecked = prepareShoppingMemo(recipe, listOf(PantryItem("b", "f", "鸡蛋")), memo, true)
        assertFalse(rechecked.items[0].checked); assertTrue(rechecked.items[1].checked)
        assertEquals(rechecked, Json.decodeFromString<ShoppingMemo>(Json.encodeToString(rechecked)))
    }
    @Test fun reorderOrAmountChangesKeepChecksBoundToIngredientId() {
        val memo = prepareShoppingMemo(recipe, emptyList()).markPrepared("tomato", true)
        val changed = recipe.copy(ingredients = recipe.ingredients.reversed().map { if (it.id == "tomato") it.copy(quantityText = "4个") else it })
        val reopened = prepareShoppingMemo(changed, emptyList(), memo)
        assertTrue(reopened.items.single { it.key == "tomato" }.checked)
        assertEquals("番茄 4个", reopened.items.single { it.key == "tomato" }.ingredient)
        assertEquals(recipe.ingredients.map { it.id }.reversed(), reopened.items.map { it.key })
    }
}
