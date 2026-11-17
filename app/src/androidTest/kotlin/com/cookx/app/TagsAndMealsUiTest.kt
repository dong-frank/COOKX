package com.cookx.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cookx.app.domain.*
import com.cookx.app.data.PresetRepository
import androidx.test.platform.app.InstrumentationRegistry
import com.cookx.app.ui.*
import com.cookx.app.ui.theme.COOKXTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TagsAndMealsUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun multiTagsCanBeAddedAndRemovedWithoutChangingOtherRecipes() {
        var saved: List<String>? = null
        compose.setContent {
            COOKXTheme { Box(Modifier.safeDrawingPadding()) {
                RecipeTagsEditor(exampleRecipes("f").first(), PresetRepository(InstrumentationRegistry.getInstrumentation().targetContext).tags.map { it.name }, false) { saved = it }
            } }
        }
        compose.onNodeWithText("川菜").performScrollTo().performClick()
        compose.onNodeWithText("快手菜").performScrollTo().performClick()
        compose.onNodeWithText("自定义标签").performScrollTo().performTextInput("周末大餐")
        compose.onNodeWithText("创建并添加").performScrollTo().performClick()
        compose.onNodeWithText("保存标签").performScrollTo().performClick()
        assertEquals(listOf("家常菜", "川菜", "周末大餐"), saved)
        assertEquals(listOf("素菜", "快手菜"), exampleRecipes("f")[1].tags)
    }
    @Test fun familyCanReorderAndHideFavoriteTags() {
        val kitchen = mutableStateOf(KitchenState(favoriteTags = listOf("快手菜", "川菜"), presetTags = PresetRepository(InstrumentationRegistry.getInstrumentation().targetContext).tags.map { it.name }))
        compose.setContent {
            COOKXTheme { Box(Modifier.safeDrawingPadding()) {
                FamilyTagSettings(kitchen.value, false, {}, { kitchen.value = kitchen.value.copy(favoriteTags = it) })
            } }
        }
        compose.onNodeWithTag("moveUp:川菜").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("川菜", "快手菜"), kitchen.value.favoriteTags) }
        compose.onNode(hasScrollAction()).performScrollToNode(hasTestTag("favorite:川菜"))
        compose.onNodeWithTag("favorite:川菜").performClick()
        compose.runOnIdle { assertEquals(listOf("快手菜"), kitchen.value.favoriteTags) }
    }
    @Test fun chosenMealAndDateSurviveStepsAndStateRestoration() {
        val restore = StateRestorationTester(compose)
        var saved: MealSelection? = null
        val recipe = exampleRecipes("f").first()
        restore.setContent {
            COOKXTheme { Box(Modifier.safeDrawingPadding()) {
                CookingScreen(recipe, Cook(recipe.id, "2026-11-16", Meal.DINNER), false) { _, selection -> saved = selection }
            } }
        }
        compose.onNodeWithTag("cookingDate").assertDoesNotExist()
        compose.onNodeWithTag("meal:DINNER").assertDoesNotExist()
        compose.onNodeWithText("下一步").performScrollTo().performClick()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("备菜 · 第 2 / 2 步").assertExists()
        compose.onNodeWithTag("cookingDate").assertDoesNotExist()
        repeat(3) { compose.onNodeWithText("下一步").performScrollTo().performClick() }
        compose.onNodeWithText("完成步骤").performScrollTo().performClick()
        compose.onNodeWithText("制作日期：2026-11-16").assertExists()
        compose.onNodeWithTag("meal:DINNER").assertIsSelected()
        compose.onNodeWithTag("meal:BREAKFAST").performScrollTo().performClick()
        compose.onNodeWithTag("cookingDate").performScrollTo().performClick()
        compose.onNodeWithText("Tuesday, November 17, 2026", substring = true, useUnmergedTree = true).performClick()
        compose.onNodeWithText("确定日期").performClick()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("制作日期：2026-11-17").assertExists()
        compose.onNodeWithTag("meal:BREAKFAST").assertIsSelected()
        compose.onNodeWithText("回看最后一步").performScrollTo().performClick()
        compose.onNodeWithTag("cookingDate").assertDoesNotExist()
        compose.onNodeWithTag("meal:BREAKFAST").assertDoesNotExist()
        compose.onNodeWithText("完成步骤").performScrollTo().performClick()
        compose.onNodeWithText("制作日期：2026-11-17").assertExists()
        compose.onNodeWithText("保存制作记录").performScrollTo().performClick()
        assertEquals(MealSelection("2026-11-17", Meal.BREAKFAST), saved)
    }
}
