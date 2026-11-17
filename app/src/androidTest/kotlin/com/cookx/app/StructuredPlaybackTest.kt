package com.cookx.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import org.junit.Assert.assertNotEquals
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cookx.app.domain.*
import com.cookx.app.ui.Cook
import com.cookx.app.ui.CookingScreen
import com.cookx.app.ui.theme.COOKXTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StructuredPlaybackTest {
    @get:Rule val compose = createComposeRule()
    @Test fun configuredTimerContinuesAcrossPhaseAndRestoration() {
        val recipe = Recipe("r", "f", "测试菜", listOf(RecipeIngredient("i", "土豆", "1个")), listOf(
            RecipePhase(PhaseType.PREP, listOf(RecipeStep("p", "清洗", "清洗土豆。", operationType = OperationType.RINSE,
                ingredients = listOf(StepIngredientUse("i", "1个")), timer = StepTimer(120, "开始清洗后")))),
            RecipePhase(PhaseType.COOK, listOf(RecipeStep("c", "制作", "按做法制作。"))),
        ))
        val restore = StateRestorationTester(compose)
        restore.setContent { COOKXTheme { Box(Modifier.safeDrawingPadding()) {
            CookingScreen(recipe, Cook("r", "2026-11-17", Meal.LUNCH), false) { _, _ -> }
        } } }
        compose.onNodeWithText("本步食材").assertDoesNotExist()
        compose.onNodeWithText("土豆 1个").assertDoesNotExist()
        compose.onNodeWithText("清洗土豆。").assertIsDisplayed()
        compose.onNodeWithTag("openCookingTimer").performClick()
        compose.onNodeWithTag("timerDisplay").assertTextEquals("02:00")
        compose.onNodeWithText("开始计时").performClick()
        compose.onNodeWithText("下一页").assertIsDisplayed().performClick()
        compose.onNodeWithText("烹饪 · 第 1 / 1 步").assertExists()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("openCookingTimer").performClick()
        compose.onNodeWithText("停止计时").assertExists()
        compose.onNodeWithTag("cookingDate").assertDoesNotExist()
        compose.onNodeWithText("停止计时").performClick()
        compose.onNodeWithTag("timerDisplay").assertTextEquals("01:00")
        compose.onNodeWithText("关闭").performClick()
    }
    @Test fun defaultTimerOpensIdleAndFooterStaysVisibleForLongInstructions() {
        val original = exampleRecipes("f").first()
        val recipe = original.copy(phases = listOf(RecipePhase(PhaseType.PREP, listOf(
            original.phases.first().steps.first().copy(description = "清洗番茄后切块。\n".repeat(100))
        )), RecipePhase(PhaseType.COOK)))
        val restore = StateRestorationTester(compose)
        restore.setContent { COOKXTheme { Box(Modifier.safeDrawingPadding()) {
            CookingScreen(recipe, Cook(recipe.id), false) { _, _ -> }
        } } }
        compose.onNodeWithText("上一页").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("下一页").assertIsDisplayed()
        compose.onNodeWithTag("openCookingTimer").assertIsDisplayed().performClick()
        compose.onNodeWithTag("timerDisplay").assertTextEquals("01:00")
        compose.onNodeWithText("停止计时").assertDoesNotExist()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("timerDisplay").assertTextEquals("01:00")
        compose.onNodeWithText("开始计时").assertIsDisplayed()
        compose.onNodeWithTag("timerSeconds").performTouchInput { swipeUp() }
        compose.waitForIdle()
        val selected = compose.onNodeWithTag("timerDisplay").fetchSemanticsNode().config[SemanticsProperties.Text].single().text
        assertNotEquals("01:00", selected)
        compose.onNodeWithText("停止计时").assertDoesNotExist()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("timerDisplay").assertTextEquals(selected)
        compose.onNodeWithText("开始计时").assertIsDisplayed().performClick()
        compose.onNodeWithTag("openCookingTimer").performClick()
        compose.onNodeWithText("停止计时").assertIsDisplayed()
        compose.onNodeWithText("关闭").performClick()
        compose.onNodeWithText("下一页").assertIsDisplayed().performClick()
        compose.onNodeWithText("下一页").assertIsNotEnabled()
        compose.onNodeWithText("上一页").performClick()
        compose.onNodeWithTag("openCookingTimer").assertIsDisplayed()
    }
}
