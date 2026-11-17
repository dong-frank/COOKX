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

    @Test fun familyRecipeCookingAndPersistence() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("cookx_kitchen_v1", Context.MODE_PRIVATE).edit().clear().commit()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitUntil(10_000) { compose.onAllNodesWithText("家庭名称").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("家庭名称").performTextInput("演示小厨房")
            compose.onNodeWithText("创建我的家庭").performClick()
            compose.waitUntil(10_000) { compose.onAllNodes(hasText("加入示例菜谱") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("加入示例菜谱").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("番茄炒蛋").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("番茄炒蛋").performClick()
            compose.onNodeWithText("开始制作").performClick()
            compose.onNodeWithText("第 1 / 3 步").assertIsDisplayed()
            compose.onNodeWithText("计时 1 分钟").performScrollTo().performClick()
            compose.onNodeWithText("下一步").performScrollTo().performClick()
            scenario.recreate()
            compose.onNodeWithText("第 2 / 3 步").assertIsDisplayed()
            compose.onNodeWithText("取消计时").assertExists()
            compose.onNodeWithText("下一步").performScrollTo().performClick()
            compose.onNodeWithText("完成步骤").performScrollTo().performClick()
            compose.onNodeWithText("本次制作心得（选填）").performTextInput("下次少放盐")
            compose.onNodeWithText("保存制作记录").performScrollTo().performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("开始制作").fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("下次少放盐"))
            compose.onNodeWithText("下次少放盐").assertIsDisplayed()
            scenario.recreate()
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("下次少放盐"))
            compose.onNodeWithText("下次少放盐").assertIsDisplayed()
            compose.onNodeWithText("返回").performClick()
            compose.onNodeWithTag("recipeList").assertIsDisplayed()
            compose.onNodeWithText("2 道菜 · 1 次制作记录").assertDoesNotExist()
            compose.onNodeWithText("新增菜谱").performClick()
            compose.onNodeWithText("保存菜谱").performScrollTo().performClick()
            compose.onNodeWithText("请填写菜名").assertIsDisplayed()
            compose.onNodeWithText("菜名").performScrollTo().performTextInput("蒜香青菜")
            compose.onNodeWithText("食材与用量，每行一种").performScrollTo().performTextInput("青菜 1 把\n蒜 2 瓣")
            compose.onNodeWithText("制作步骤，每行一步").performScrollTo().performTextInput("洗净切好\n炒熟出锅")
            compose.onNodeWithText("保存菜谱").performScrollTo().performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithText("蒜香青菜").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("recipeList").performScrollToNode(hasText("蒜香青菜"))
            compose.onNodeWithText("蒜香青菜").assertIsDisplayed()
        }
    }
}
