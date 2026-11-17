package com.cookx.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cookx.app.data.KitchenRepository
import com.cookx.app.domain.*
import com.cookx.app.ui.ShoppingMemoScreen
import com.cookx.app.ui.PantryScreen
import com.cookx.app.ui.theme.COOKXTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingMemoUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun memoPersistsChecksAndNeverChangesPantryOrCookingRecords() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = KitchenRepository(context, "cookx_shopping_test")
        val recipe = exampleRecipes("f").first()
        val initial = KitchenState(family = Family("f", "测试家庭"), recipes = listOf(recipe), schemaVersion = 2,
            pantryItems = listOf(PantryItem("egg", "f", "鸡蛋")))
        val memo = prepareShoppingMemo(recipe, initial.pantryItems)
        repository.write(initial.copy(shoppingMemos = listOf(memo)))
        val state = mutableStateOf(repository.read())
        val restore = StateRestorationTester(compose)
        restore.setContent {
            COOKXTheme { Box(Modifier.safeDrawingPadding()) {
                ShoppingMemoScreen(recipe, state.value.shoppingMemos.single(), false,
                    { key, checked ->
                        val next = state.value.copy(shoppingMemos = listOf(state.value.shoppingMemos.single().markPrepared(key, checked)))
                        repository.write(next); state.value = next
                    }, {
                        val next = state.value.copy(shoppingMemos = listOf(prepareShoppingMemo(recipe, state.value.pantryItems, state.value.shoppingMemos.single(), true)))
                        repository.write(next); state.value = next
                    })
            } }
        }
        val tomato = "shopping:${memo.items[0].key}"
        val egg = "shopping:${memo.items[1].key}"
        compose.onNodeWithTag(tomato).assertIsOff()
        compose.onNodeWithTag(egg).assertIsOn()
        compose.onNodeWithTag(tomato).performClick()
        compose.onNodeWithTag(egg).performClick()
        compose.onNodeWithText("重新检查").performScrollTo().performClick()
        compose.runOnIdle { state.value = KitchenRepository(context, "cookx_shopping_test").read() }
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag(tomato).assertIsOn()
        compose.onNodeWithTag(egg).assertIsOff()
        compose.onNodeWithText("已准备 1 / 3").assertExists()
        val saved = KitchenRepository(context, "cookx_shopping_test").read()
        assertEquals(initial.pantryItems, saved.pantryItems)
        assertEquals(initial.records, saved.records)
        assertEquals(initial.recipes, saved.recipes)
    }
    @Test fun pantryPresenceEntryWorksWithKeyboardVisible() {
        val state = mutableStateOf(KitchenState(family = Family("f", "家")))
        compose.setContent {
            COOKXTheme { Box(Modifier.safeDrawingPadding()) {
                PantryScreen(state.value, false, { name -> state.value = state.value.copy(pantryItems = listOf(PantryItem("a", "f", name))) },
                    { _, available -> state.value = state.value.copy(pantryItems = state.value.pantryItems.map { it.copy(available = available) }) })
            } }
        }
        compose.onNodeWithText("食材名称").performTextInput("鸡蛋")
        compose.onNodeWithText("登记家中已有").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("鸡蛋", state.value.pantryItems.single().name) }
        compose.onNode(isToggleable()).performScrollTo().assertIsOn()
    }
}
