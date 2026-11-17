package com.cookx.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cookx.app.domain.*
import com.cookx.app.data.PresetRepository
import androidx.test.platform.app.InstrumentationRegistry
import com.cookx.app.ui.StructuredRecipeEditor
import com.cookx.app.ui.theme.COOKXTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StructuredEditorUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun customStepWithIngredientAndTimerSurvivesRestoration() {
        var saved: Recipe? = null
        val restore = StateRestorationTester(compose)
        restore.setContent { COOKXTheme { Box(Modifier.safeDrawingPadding()) {
            StructuredRecipeEditor("f", PresetRepository(InstrumentationRegistry.getInstrumentation().targetContext).tags.map { it.name }, false) { saved = it }
        } } }
        compose.onNodeWithText("菜名").performTextInput("洗番茄")
        compose.onNodeWithText("添加食材").performScrollTo().performClick()
        compose.onNodeWithTag("ingredientName:0").performScrollTo().performTextInput("番茄")
        compose.onNodeWithTag("ingredientAmount:0").performScrollTo().performTextInput("2个")
        compose.onNodeWithTag("addStep:PREP").performScrollTo().performClick()
        compose.onNodeWithText("步骤标题").performTextInput("清洗番茄")
        compose.onNodeWithText("步骤说明").performTextInput("用清水洗净番茄。")
        compose.onNodeWithText("其他").performScrollTo().performClick()
        compose.onNodeWithTag("operation:RINSE").performClick()
        // Locate the single ingredient checkbox through its semantics.
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.onNodeWithText("番茄本步用量（选填）").performScrollTo().performTextInput("2个")
        compose.onNodeWithText("计时秒数（选填）").performScrollTo().performTextInput("60")
        compose.onNodeWithText("计时启动条件").performScrollTo().performTextReplacement("开始清洗后")
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("保存步骤").performClick()
        compose.onNodeWithText("保存菜谱").performScrollTo().performClick()
        compose.runOnIdle {
            val recipe = requireNotNull(saved)
            validateRecipe(recipe)
            assertEquals("洗番茄", recipe.name)
            assertTrue(recipe.phases.last().steps.isEmpty())
            val step = recipe.phases.first().steps.single()
            assertEquals(OperationType.RINSE, step.operationType)
            assertEquals(recipe.ingredients.single().id, step.ingredients.single().ingredientId)
            assertEquals(StepTimer(60, "开始清洗后"), step.timer)
        }
    }
    @Test fun standardTemplateBindsIngredientAndSavesSnapshot() {
        var saved: Recipe? = null
        compose.setContent { COOKXTheme { Box(Modifier.safeDrawingPadding()) {
            StructuredRecipeEditor("f", PresetRepository(InstrumentationRegistry.getInstrumentation().targetContext).tags.map { it.name }, false) { saved = it }
        } } }
        compose.onNodeWithText("菜名").performTextInput("滚刀土豆")
        compose.onNodeWithText("添加食材").performScrollTo().performClick()
        compose.onNodeWithTag("ingredientName:0").performScrollTo().performTextInput("土豆")
        compose.onNodeWithTag("addStep:PREP").performScrollTo().performClick()
        compose.onNodeWithText("标准操作").performClick()
        compose.onNodeWithText("切滚刀块").performClick()
        compose.onNodeWithTag("templateSelect:main").performScrollTo().performClick()
        compose.onNodeWithTag("templateIngredient:main:土豆").performClick()
        compose.onNodeWithText("保存步骤").performClick()
        compose.onNodeWithText("保存菜谱").performScrollTo().performClick()
        compose.runOnIdle {
            val recipe = requireNotNull(saved)
            val step = recipe.phases.first().steps.single()
            validateRecipe(recipe)
            assertEquals(StepKind.TEMPLATE, step.kind)
            assertEquals("cut-roll", step.templateRef!!.id)
            assertEquals(recipe.ingredients.single().id, step.ingredients.single().ingredientId)
            assertTrue(step.description.contains("沿食材长轴"))
            assertTrue(step.templateRef!!.values.getValue("main").ingredients.isNotEmpty())
        }
    }
}
