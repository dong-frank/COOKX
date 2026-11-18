package com.cookx.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.cookx.app.data.KitchenRepository
import com.cookx.app.domain.Family
import com.cookx.app.domain.KitchenState
import com.cookx.app.domain.newId
import com.cookx.app.ui.StructuredRecipeEditor
import com.cookx.app.ui.theme.COOKXTheme
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class AiRecipeDraftUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun liveDraftFillsEditorAndOnlyPersistsAfterExplicitSave() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("cookxLiveAi") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val family = Family(newId(), "Draft test")
        val store = "cookx_ai_draft_test"
        val repository = KitchenRepository(context, store)
        repository.write(KitchenState(family = family))
        try {
            compose.setContent {
                COOKXTheme {
                    StructuredRecipeEditor(family.id, listOf("凉拌"), false) { draft ->
                        repository.write(repository.read().copy(recipes = listOf(draft)))
                    }
                }
            }
            compose.onNodeWithText("AI 整理").performClick()
            compose.onNodeWithText("粘贴菜谱或输入做法").performTextInput(
                "凉拌黄瓜，2人份。黄瓜1根拍碎。加生抽1勺拌匀，静置5分钟。"
            )
            compose.onNodeWithText("生成草稿").performClick()
            compose.waitUntil(60_000) {
                compose.onAllNodesWithTag("ingredientName:0").fetchSemanticsNodes().isNotEmpty()
            }
            assertTrue(repository.read().recipes.isEmpty())
            compose.onNodeWithText("菜名").performScrollTo().performTextReplacement("已核对的黄瓜")
            compose.onNodeWithText("保存菜谱").performScrollTo().performClick()
            val saved = repository.read().recipes.single()
            assertEquals("已核对的黄瓜", saved.name)
            assertEquals(family.id, saved.familyId)
            assertEquals(2, saved.servings)
            assertTrue(saved.ingredients.any { it.name == "黄瓜" && it.quantityText == "1根" })
            assertTrue(saved.phases.flatMap { it.steps }.any { it.timer?.durationSeconds == 300 })
            assertTrue(saved.phases.flatMap { it.steps }.flatMap { it.ingredients }.isNotEmpty())
        } finally {
            context.deleteSharedPreferences(store)
        }
    }
}
