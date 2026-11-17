package com.cookx.app.domain

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ModelsTest {
    @Test fun recipeInputTrimsAndRejectsEmptySteps() {
        val draft = recipeDraft("family-a", "  家常菜 ", "鸡蛋 2 个\n\n 盐 少许", " 准备 \n炒熟 ")
        assertEquals("家常菜", draft.name)
        assertEquals(listOf("鸡蛋 2 个", "盐 少许"), draft.ingredients)
        assertEquals(listOf("准备", "炒熟"), draft.steps)
        assertEquals("family-a", draft.familyId)
        assertThrows(IllegalArgumentException::class.java) { recipeDraft("family-a", "菜", "盐", " \n") }
    }
    @Test fun lastCookedUsesLatestMatchingRecord() {
        val state = KitchenState(records = listOf(
            CookingRecord("a", "family-a", "recipe-a", 200, ""),
            CookingRecord("b", "family-a", "recipe-b", 500, ""),
            CookingRecord("c", "family-a", "recipe-a", 100, "")))
        assertEquals(200L, state.lastCooked("recipe-a"))
        assertNull(state.lastCooked("missing"))
    }
    @Test fun storedStateRoundTripKeepsOwnershipAndHistory() {
        val state = KitchenState(Family("family-a", "家"), exampleRecipes("family-a"),
            listOf(CookingRecord("record-a", "family-a", "example-tomato-eggs", 123, "少放盐")))
        assertEquals(state, Json.decodeFromString<KitchenState>(Json.encodeToString(state)))
        assertTrue(state.recipes.all { it.familyId == state.family!!.id })
    }
    @Test fun oldRecipeDataDefaultsToUncategorized() {
        val recipe = Json.decodeFromString<Recipe>("""{"id":"old","familyId":"family-a","name":"旧菜谱","ingredients":["盐"],"steps":["炒熟"]}""")
        assertEquals("未分类", recipe.category)
        assertEquals("素菜", recipeDraft("family-a", "菜", "青菜", "炒熟", listOf(" 素菜 ")).tags.single())
    }
}
