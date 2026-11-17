package com.cookx.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.cookx.app.domain.*
import com.cookx.app.ui.*
import com.cookx.app.ui.theme.COOKXTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CookingResultUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun resultKeepsNewAndDraftUntilSavedAndUsesExistingFooter() {
        val base = exampleRecipes("f").first()
        val recipe = base.copy(phases = listOf(RecipePhase(PhaseType.PREP, listOf(base.phases.first().steps.first())), RecipePhase(PhaseType.COOK)))
        var note: String? = null
        val busy = mutableStateOf(false)
        val first = mutableStateOf(true)
        val restore = StateRestorationTester(compose)
        restore.setContent { COOKXTheme { Box(Modifier.safeDrawingPadding()) {
            CookingScreen(recipe, Cook(recipe.id, "2026-11-17", Meal.DINNER), busy.value, first.value) { value, _ -> note = value }
        } } }
        compose.onNodeWithText("下一页").performClick()
        compose.onNodeWithText("出锅啦").assertIsDisplayed()
        compose.onNodeWithText(recipe.name).assertIsDisplayed()
        compose.onNodeWithTag("firstCookingBadge").assertIsDisplayed()
        compose.onNodeWithText("又完成了一道家的味道").assertDoesNotExist()
        compose.onNodeWithText("返回步骤").assertDoesNotExist()
        compose.onNodeWithText("下一页").assertIsNotEnabled()
        compose.onNodeWithText("本次制作心得（选填）").performScrollTo().performTextInput("下次少放盐")
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("下次少放盐").assertExists()
        compose.onNodeWithText("上一页").performClick()
        compose.onNodeWithText("下一页").performClick()
        compose.onNodeWithTag("firstCookingBadge").assertExists()
        compose.onNodeWithText("下次少放盐").assertExists()
        compose.runOnIdle { busy.value = true }
        compose.onNodeWithText("保存制作记录").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { busy.value = false }
        compose.onNodeWithText("保存制作记录").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("下次少放盐", note); first.value = false }
        compose.onNodeWithTag("firstCookingBadge").assertDoesNotExist()
    }
    @Test fun repeatCookingCardShowsLongNameWithoutNew() {
        val recipe = exampleRecipes("f").last().copy(name = "蒜香口蘑与时令蔬菜拼盘")
        compose.setContent { COOKXTheme {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                Box(Modifier.safeDrawingPadding().width(280.dp)) { CookingResultCard(recipe, false) }
            }
        } }
        compose.onNodeWithText(recipe.name).assertIsDisplayed()
        compose.onNodeWithTag("firstCookingBadge").assertDoesNotExist()
    }
}
