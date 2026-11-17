package com.cookx.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.cookx.app.data.PresetRepository
import com.cookx.app.domain.*
import com.cookx.app.ui.TemplateFields
import com.cookx.app.ui.updateTemplateValue
import com.cookx.app.ui.theme.COOKXTheme
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TemplateFieldsUiTest {
    @get:Rule val compose = createComposeRule()
    private val templates get() = PresetRepository(InstrumentationRegistry.getInstrumentation().targetContext).templates
    @Test fun optionalActionsAndAmountsRestoreAndResolveOnlyWhenEnabled() {
        val template = templates.single { it.id == "broccoli-blanch" }
        val ingredients = listOf(RecipeIngredient("b", "西兰花"), RecipeIngredient("w", "水"), RecipeIngredient("s", "盐"))
        var current: Map<String, TemplateValue> = emptyMap()
        val restore = StateRestorationTester(compose)
        restore.setContent { COOKXTheme {
            var raw by rememberSaveable { mutableStateOf(Json.encodeToString(mapOf("broccoli" to TemplateValue(ingredients = listOf(StepIngredientUse("b"))), "water" to TemplateValue(ingredients = listOf(StepIngredientUse("w")))))) }
            val values = Json.decodeFromString<Map<String, TemplateValue>>(raw)
            SideEffect { current = values }
            Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState())) {
                TemplateFields(template, values, ingredients) { id, value -> raw = Json.encodeToString(updateTemplateValue(template, values, id, value)) }
            }
        } }
        compose.onNodeWithTag("templateSelect:salt").assertDoesNotExist()
        compose.onNodeWithTag("templateFlag:addSalt").performScrollTo().performClick()
        compose.onNodeWithTag("templateSelect:salt").performScrollTo().performClick()
        compose.onNodeWithTag("templateIngredient:salt:盐").performClick()
        compose.onNodeWithTag("templateAmount:salt:盐").performScrollTo().performTextInput("少许")
        compose.onNodeWithTag("templateText:durationMinutes").performScrollTo().performTextInput("1.5")
        compose.onNodeWithTag("templateText:timerStart").performScrollTo().performTextInput("水再次沸腾后")
        compose.onNodeWithTag("templateFlag:cool").performScrollTo().performClick()
        restore.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            val step = applyOperationTemplate(template, current, ingredients)
            assertEquals(StepTimer(90, "水再次沸腾后"), step.timer)
            assertTrue(step.description.contains("盐 少许") && step.description.contains("过凉"))
        }
        compose.onNodeWithTag("templateFlag:addSalt").performScrollTo().performClick()
        compose.onNodeWithTag("templateSelect:salt").assertDoesNotExist()
        compose.runOnIdle {
            val step = applyOperationTemplate(template, current, ingredients)
            assertFalse(step.ingredients.any { it.ingredientId == "s" })
            assertFalse(step.description.contains("加盐"))
        }
    }
    @Test fun aromaticsCanBeReorderedWithoutLosingSelectedIngredients() {
        val template = templates.single { it.id == "aromatics-saute" }
        val ingredients = listOf(RecipeIngredient("g", "姜"), RecipeIngredient("c", "葱"), RecipeIngredient("o", "油"))
        var current: Map<String, TemplateValue> = emptyMap()
        compose.setContent { COOKXTheme {
            var values by remember { mutableStateOf(mapOf("oil" to TemplateValue(ingredients = listOf(StepIngredientUse("o"))))) }
            SideEffect { current = values }
            Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState())) {
                TemplateFields(template, values, ingredients) { id, value -> values = updateTemplateValue(template, values, id, value) }
            }
        } }
        compose.onNodeWithTag("templateUse:aromatics:姜").performScrollTo().performClick()
        compose.onNodeWithTag("templateUse:aromatics:葱").performScrollTo().performClick()
        compose.onNodeWithText("按顺序下").performScrollTo().performClick()
        compose.onNodeWithTag("templateOrderUp:c").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(applyOperationTemplate(template, current, ingredients).description.contains("葱 → 姜")) }
        compose.onNodeWithTag("templateUse:aromatics:葱").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(listOf("g"), current.getValue("additionOrder").order)
            assertFalse(applyOperationTemplate(template, current, ingredients).ingredients.any { it.ingredientId == "c" })
        }
    }
}
