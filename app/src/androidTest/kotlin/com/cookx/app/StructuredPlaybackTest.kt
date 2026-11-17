package com.cookx.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
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
        compose.onNodeWithText("土豆 1个").assertExists()
        compose.onNodeWithText("计时 120 秒").performScrollTo().performClick()
        compose.onNodeWithText("下一步").performScrollTo().performClick()
        compose.onNodeWithText("烹饪 · 第 1 / 1 步").assertExists()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("取消计时").assertExists()
        compose.onNodeWithTag("cookingDate").assertDoesNotExist()
        compose.onNodeWithText("取消计时").performScrollTo().performClick()
        compose.onNodeWithText("取消计时").assertDoesNotExist()
    }
}
