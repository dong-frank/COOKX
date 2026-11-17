package com.cookx.app.domain

import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class TagsAndMealsTest {
    @Test fun mealBoundaryHoursAndMidnightUseLocalCalendar() {
        val expected = mapOf(0 to Meal.LATE_NIGHT, 4 to Meal.LATE_NIGHT, 5 to Meal.BREAKFAST,
            9 to Meal.BREAKFAST, 10 to Meal.LUNCH, 14 to Meal.LUNCH, 15 to Meal.DINNER,
            20 to Meal.DINNER, 21 to Meal.LATE_NIGHT, 23 to Meal.LATE_NIGHT)
        expected.forEach { (hour, meal) -> assertEquals("hour=$hour", meal, mealForHour(hour)) }
        val midnightUtc = dateToUtcMillis("2026-11-17")
        assertEquals(MealSelection("2026-11-16", Meal.DINNER), mealAt(midnightUtc, TimeZone.getTimeZone("GMT-08:00")))
        assertEquals(MealSelection("2026-11-17", Meal.BREAKFAST), mealAt(midnightUtc, TimeZone.getTimeZone("GMT+08:00")))
        assertEquals("2026-11-17", utcMillisToDate(midnightUtc))
        assertThrows(Exception::class.java) { dateToUtcMillis("2026-02-30") }
    }
    @Test fun tagNormalizationAndMultipleMembership() {
        assertEquals(listOf("快手菜", "川菜"), normalizeTags(listOf(" 快手菜 ", "", "川菜", "快手菜")))
        assertThrows(IllegalArgumentException::class.java) { normalizeTags(listOf("全部")) }
        val recipe = exampleRecipes("family").first()
        assertTrue(recipe.matchesTag("快手菜")); assertTrue(recipe.matchesTag("家常菜"))
        assertFalse(recipe.matchesTag("未打标签")); assertFalse(recipe.matchesTag("川菜"))
        assertTrue(recipe.copy(tags = emptyList()).matchesTag("未打标签"))
    }
    @Test fun recordOrderingUsesChosenDateAndMealRatherThanSaveTime() {
        val state = KitchenState(records = listOf(
            CookingRecord("new-save", "f", "r", 999, "", "2026-11-16", Meal.DINNER),
            CookingRecord("breakfast", "f", "r", 100, "", "2026-11-17", Meal.BREAKFAST),
            CookingRecord("lunch", "f", "r", 50, "", "2026-11-17", Meal.LUNCH)))
        assertEquals(listOf("lunch", "breakfast", "new-save"), state.recordsFor("r").map { it.id })
        assertEquals("2026-11-17 · 午餐", state.recordsFor("r").first().mealLabel())
    }
    @Test fun emptyTagsAreHiddenAndFavoriteOrderIsPreserved() {
        val recipes = exampleRecipes("f")
        assertEquals(listOf("全部", "素菜", "家常菜"), recipeCategories(recipes, listOf("汤羹", "素菜", "家常菜")))
        assertEquals(listOf("全部"), recipeCategories(emptyList(), listOf("素菜")))
        assertEquals(listOf("全部", "未打标签"), recipeCategories(listOf(recipes.first().copy(tags = emptyList())), listOf("家常菜")))
    }
}
