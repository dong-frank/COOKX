package com.cookx.app

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KitchenJourneyTest {
    @get:Rule val compose = createEmptyComposeRule()
    @Test fun cleanHouseholdCreatesStructuredRecipeAndPlaysBothPhases() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("cookx_kitchen_v3", Context.MODE_PRIVATE).edit().clear().commit()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitUntil(10_000) { compose.onAllNodesWithText("家庭名称").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("家庭名称").performTextInput("测试厨房")
            compose.onNodeWithText("创建我的家庭").performClick()
            compose.waitUntil(10_000) { compose.onAllNodes(hasText("加入示例菜谱") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("加入示例菜谱").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("番茄炒蛋").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("番茄炒蛋").performClick()
            compose.onNodeWithText("准备食材").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("采购便签").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("shopping:tomato").performClick()
            compose.onNodeWithText("返回").performClick()
            compose.onNodeWithText("开始制作").performClick()
            compose.onNodeWithText("备菜 · 第 1 / 2 步").assertIsDisplayed()
            compose.onNodeWithText("本步食材").assertDoesNotExist()
            compose.onNodeWithText("番茄洗净后切块。").assertIsDisplayed()
            compose.onNodeWithTag("cookingDate").assertDoesNotExist()
            compose.onNodeWithText("下一步").performScrollTo().performClick()
            scenario.recreate()
            compose.onNodeWithText("备菜 · 第 2 / 2 步").assertExists()
            compose.onNodeWithText("下一步").performScrollTo().performClick()
            compose.onNodeWithText("烹饪 · 第 1 / 3 步").assertExists()
            repeat(2) { compose.onNodeWithText("下一步").performScrollTo().performClick() }
            compose.onNodeWithText("完成步骤").performScrollTo().performClick()
            compose.onNodeWithTag("cookingDate").assertExists()
            compose.onNodeWithText("本次制作心得（选填）").performTextInput("下次少放盐")
            compose.onNodeWithText("保存制作记录").performScrollTo().performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("开始制作").fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("下次少放盐"))
            compose.onNodeWithText("下次少放盐").assertIsDisplayed()
            scenario.recreate()
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("下次少放盐"))
            compose.onNodeWithText("下次少放盐").assertIsDisplayed()
        }
    }
}
