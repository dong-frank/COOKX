package com.cookx.app.domain

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class PantryTest {
    @Test fun legacyPresenceKeepsItsIdentityWithoutInventingAnAmount() {
        val item = Json.decodeFromString<PantryItem>("""{"id":"i","familyId":"f","name":"鸡蛋","available":true}""")
        assertEquals(PantryZone.FRIDGE, item.zone)
        assertNull(item.quantityText)
        assertTrue(item.inStock())
        assertEquals("未填数量", item.amountLabel())
    }
    @Test fun zeroAndUnavailableItemsAreHiddenAndNotPrechecked() {
        val items = listOf(PantryItem("a", "f", "番茄", quantityText = "0 个"),
            PantryItem("b", "f", "鸡蛋", available = false), PantryItem("c", "f", "盐", zone = PantryZone.SPICES, quantityText = "半罐"),
            PantryItem("d", "other", "番茄", quantityText = "3 个"))
        val state = KitchenState(family = Family("f", "家"), pantryItems = items)
        assertTrue(state.pantryIn(PantryZone.FRIDGE).isEmpty())
        assertEquals(listOf("c"), state.pantryIn(PantryZone.SPICES).map { it.id })
        val memo = prepareShoppingMemo(exampleRecipes("f").first(), items)
        assertFalse(memo.items.single { it.key == "tomato" }.checked)
        assertTrue(memo.items.single { it.key == "salt" }.checked)
        assertTrue(PantryItem("e", "f", "油", quantityText = "0.5 升").inStock())
    }
    @Test fun savingDoesNotMergeNamesAndEditingKeepsStableIdentity() {
        val state = KitchenState(family = Family("f", "家"))
        val item = PantryItem("a", "f", " 盐 ", zone = PantryZone.SPICES, quantityText = " 半罐 ")
        val saved = state.savePantry(item).savePantry(item.copy(id = "b"))
        assertEquals(2, saved.pantryItems.size)
        val edited = saved.savePantry(item.copy(quantityText = "0"))
        assertEquals(1, edited.pantryIn(PantryZone.SPICES).size)
        assertEquals("b", edited.pantryIn(PantryZone.SPICES).single().id)
        assertEquals(2, edited.pantryItems.size)
        assertThrows(IllegalArgumentException::class.java) { state.savePantry(item.copy(familyId = "other")) }
        assertThrows(IllegalArgumentException::class.java) { state.savePantry(item.copy(name = " ")) }
    }
}
