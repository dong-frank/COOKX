package com.cookx.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.cookx.app.data.KitchenRepository
import com.cookx.app.domain.*
import com.cookx.app.ui.PantryScreen
import com.cookx.app.ui.theme.COOKXTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PantrySceneUiTest {
    @get:Rule val compose = createComposeRule()
    private fun waitPanel(zone: PantryZone) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("pantryPanel:${zone.name}").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun waitScene() {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("pantryFridge").fetchSemanticsNodes().any {
            !it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)
        } }
    }
    @Test fun sceneSwitchesFiltersDetailsAndRestoresOpenState() {
        val kitchen = KitchenState(family = Family("f", "家"), pantryItems = listOf(
            PantryItem("t", "f", "番茄", quantityText = "3 个"), PantryItem("zero", "f", "鸡蛋", quantityText = "0"),
            PantryItem("s", "f", "盐", zone = PantryZone.SPICES, quantityText = "半罐")))
        val restore = StateRestorationTester(compose)
        restore.setContent { COOKXTheme { Box(Modifier.safeDrawingPadding()) { PantryScreen(kitchen, false, { _, _ -> }) } } }
        compose.onNodeWithTag("addPantryItem").assertDoesNotExist()
        waitScene()
        compose.onNodeWithTag("pantryFridge").performClick()
        waitPanel(PantryZone.FRIDGE)
        compose.onNodeWithText("3 个").assertIsDisplayed()
        compose.onNodeWithText("鸡蛋").assertDoesNotExist()
        compose.onNodeWithTag("pantryItem:t").performClick()
        compose.onNodeWithTag("pantryBackToList").assertIsDisplayed()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("3 个").assertIsDisplayed()
        compose.onNodeWithTag("pantryBackToList").performClick()
        compose.onNodeWithTag("pantrySpices").performClick()
        waitPanel(PantryZone.SPICES)
        compose.onNodeWithText("盐").assertIsDisplayed()
        compose.onNodeWithText("番茄").assertDoesNotExist()
        compose.onNodeWithTag("pantrySpices").performClick()
        compose.onNodeWithTag("addPantryItem").assertDoesNotExist()
        compose.runOnIdle { assertEquals(3, kitchen.pantryItems.size) }
    }
    @Test fun addWithKeyboardPersistsZoneAndAmountAndEditCanHideZero() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = KitchenRepository(context, "cookx_scene_ui_test")
        val state = mutableStateOf(KitchenState(family = Family("f", "家")))
        val restore = StateRestorationTester(compose)
        restore.setContent { COOKXTheme { Box(Modifier.safeDrawingPadding()) {
            PantryScreen(state.value, false, { item, done ->
                repository.write(state.value.savePantry(item)); state.value = repository.read(); done()
            })
        } } }
        waitScene()
        compose.onNodeWithTag("pantrySpices").performClick()
        waitPanel(PantryZone.SPICES)
        compose.onNodeWithText("暂无物品").assertIsDisplayed()
        compose.onNodeWithTag("addPantryItem").performClick()
        compose.onNodeWithTag("pantryName").performTextInput("盐")
        compose.onNodeWithTag("pantryAmount").performTextInput("半罐")
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("半罐").assertExists()
        compose.onNodeWithText("保存").assertIsDisplayed().performClick()
        compose.onNodeWithText("半罐").assertIsDisplayed()
        val item = repository.read().pantryItems.single()
        assertEquals(PantryZone.SPICES, item.zone)
        compose.onNodeWithTag("pantryItem:${item.id}").performClick()
        compose.onNodeWithText("修改").performClick()
        compose.onNodeWithTag("pantryAmount").performTextReplacement("0")
        compose.onNodeWithText("保存").performClick()
        compose.onNodeWithText("暂无物品").assertIsDisplayed()
        assertEquals("0", repository.read().pantryItems.single().quantityText)
    }
    @Test fun threeBottlePositionsShareEntranceAndBackClosesThePanel() {
        val state = KitchenState(family = Family("f", "家"))
        compose.setContent { COOKXTheme { Box(Modifier.safeDrawingPadding()) { PantryScreen(state, false, { _, _ -> }) } } }
        waitScene()
        listOf(.1f, .5f, .9f).forEach { fraction ->
            compose.onNodeWithTag("pantrySpices").performTouchInput { click(androidx.compose.ui.geometry.Offset(width * fraction, height / 2f)) }
            waitPanel(PantryZone.SPICES)
            androidx.test.espresso.Espresso.pressBack()
            compose.onNodeWithTag("addPantryItem").assertDoesNotExist()
        }
        compose.onNodeWithTag("pantryFridge").performClick()
        waitPanel(PantryZone.FRIDGE)
        compose.onNodeWithTag("addPantryItem").performClick()
        androidx.test.espresso.Espresso.pressBack()
        compose.onNodeWithTag("pantryName").assertDoesNotExist()
        compose.onNodeWithTag("addPantryItem").assertIsDisplayed()
    }
    @Test fun failedSaveKeepsDraftAndCancelDoesNotCreateInventory() {
        val state = KitchenState(family = Family("f", "家"))
        val error = mutableStateOf<String?>(null)
        var attempted: PantryItem? = null
        compose.setContent { COOKXTheme { Box(Modifier.safeDrawingPadding()) {
            PantryScreen(state, false, { item, _ -> attempted = item; error.value = "保存失败，请重试" }, error.value)
        } } }
        waitScene()
        compose.onNodeWithTag("pantryFridge").performClick()
        waitPanel(PantryZone.FRIDGE)
        compose.onNodeWithTag("addPantryItem").performClick()
        compose.onNodeWithTag("pantryName").performTextInput("番茄")
        compose.onNodeWithTag("pantryAmount").performTextInput("3 个")
        compose.onNodeWithText("保存").performClick()
        compose.onNodeWithText("保存失败，请重试").assertIsDisplayed()
        compose.onNodeWithText("3 个").assertExists()
        assertEquals(PantryZone.FRIDGE, attempted!!.zone)
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithText("暂无物品").assertIsDisplayed()
        assertTrue(state.pantryItems.isEmpty())
    }
}
