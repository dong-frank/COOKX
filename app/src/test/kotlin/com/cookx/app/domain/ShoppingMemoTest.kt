package com.cookx.app.domain

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ShoppingMemoTest {
    private val recipe = exampleRecipes("f").first()
    @Test fun pantryPrefillsOnlyExactAvailableFamilyIngredients() {
        val memo = prepareShoppingMemo(recipe, listOf(
            PantryItem("a", "f", "番茄"), PantryItem("b", "f", "鸡蛋", false),
            PantryItem("c", "other-family", "鸡蛋"), PantryItem("d", "f", "食用油")))
        assertEquals(listOf(true, false, false), memo.items.map { it.checked })
        assertEquals(PreparationSource.PANTRY, memo.items.first().source)
        assertEquals("番茄", shoppingIngredientName("番茄 2 个"))
        assertEquals("牛肉", shoppingIngredientName("牛肉500克"))
        assertEquals("鸡蛋", shoppingIngredientName("鸡蛋"))
        assertNull(shoppingIngredientName("食用油、盐 适量"))
    }
    @Test fun manualChoicesSurviveReopenAndPantryRecheck() {
        val pantry = listOf(PantryItem("a", "f", "番茄"))
        val first = prepareShoppingMemo(recipe, pantry)
        val marked = first.markPrepared(first.items[0].key, false).markPrepared(first.items[1].key, true)
        assertEquals(marked, prepareShoppingMemo(recipe, pantry, marked))
        val rechecked = prepareShoppingMemo(recipe, listOf(PantryItem("b", "f", "鸡蛋")), marked, true)
        assertFalse(rechecked.items[0].checked)
        assertTrue(rechecked.items[1].checked)
        assertEquals(PreparationSource.MANUAL, rechecked.items[0].source)
        assertEquals(PreparationSource.MANUAL, rechecked.items[1].source)
        assertEquals(rechecked, Json.decodeFromString<ShoppingMemo>(Json.encodeToString(rechecked)))
    }
    @Test fun recheckUpdatesAutomaticItemsAndDuplicateRowsStayIndependent() {
        val repeated = recipe.copy(ingredients = listOf("番茄 2 个", "番茄 2 个"))
        val memo = prepareShoppingMemo(repeated, emptyList())
        assertNotEquals(memo.items[0].key, memo.items[1].key)
        val checked = memo.markPrepared(memo.items[0].key, true)
        assertEquals(listOf(true, false), checked.items.map { it.checked })
        val rechecked = prepareShoppingMemo(repeated, listOf(PantryItem("a", "f", "番茄")), checked, true)
        assertTrue(rechecked.items.all { it.checked })
        assertEquals(PreparationSource.PANTRY, rechecked.items[1].source)
        assertThrows(IllegalArgumentException::class.java) { memo.markPrepared("missing", true) }
    }
    @Test fun existingDataWithoutShoppingFieldsRemainsReadable() {
        val state = Json.decodeFromString<KitchenState>("""{"schemaVersion":2,"family":{"id":"f","name":"家"},"favoriteTags":["快手菜"]}""")
        assertTrue(state.shoppingMemos.isEmpty()); assertTrue(state.pantryItems.isEmpty())
        assertEquals(listOf("快手菜"), state.favoriteTags)
        assertEquals("f", state.family!!.id)
    }
}
