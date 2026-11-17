package com.cookx.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cookx.app.domain.exampleRecipes
import com.cookx.app.ui.RecipeBrowser
import com.cookx.app.ui.theme.COOKXTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeBrowserTest {
    @get:Rule val compose = createComposeRule()
    @Test fun categoryFilterRestoresAndRowsOnlyShowImageAndName() {
        val restore = StateRestorationTester(compose)
        var opened: String? = null
        restore.setContent {
            COOKXTheme { RecipeBrowser(exampleRecipes("family"), false, {}, { opened = it }) }
        }
        compose.onNodeWithText("番茄炒蛋").assertIsDisplayed()
        compose.onNodeWithText("清炒口蘑").assertIsDisplayed()
        compose.onNodeWithText("示例菜谱").assertDoesNotExist()
        compose.onNodeWithText("上次制作", substring = true).assertDoesNotExist()
        compose.onNodeWithText("3 种食材", substring = true).assertDoesNotExist()
        compose.onNodeWithText("素菜").performClick()
        compose.onNodeWithText("清炒口蘑").assertIsDisplayed()
        compose.onNodeWithText("番茄炒蛋").assertDoesNotExist()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("素菜").assertIsSelected()
        compose.onNodeWithText("清炒口蘑").performClick()
        assertEquals("example-mushrooms", opened)
        compose.onNodeWithText("汤羹").performClick()
        compose.onNodeWithText("这个分类还没有菜谱").assertIsDisplayed()
        compose.onNodeWithText("全部").performClick()
        compose.onNodeWithText("番茄炒蛋").assertIsDisplayed()
    }
}
