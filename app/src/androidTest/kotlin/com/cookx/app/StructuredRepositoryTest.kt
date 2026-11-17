package com.cookx.app

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cookx.app.data.KitchenRepository
import com.cookx.app.data.PresetRepository
import com.cookx.app.domain.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StructuredRepositoryTest {
    @Test fun structuredStatePersistsAndInvalidWritesDoNotReplaceGoodData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = KitchenRepository(context, "cookx_structured_test")
        val state = KitchenState(presetTags = PresetRepository(context).tags.map { it.name }, family = Family("f", "测试家庭"), recipes = exampleRecipes("f"),
            records = listOf(CookingRecord("record", "f", "example-tomato-eggs", 1, "心得", "2026-11-17", Meal.LUNCH)))
        repository.write(state)
        assertEquals(state, KitchenRepository(context, "cookx_structured_test").read())
        assertThrows(IllegalArgumentException::class.java) { repository.write(state.copy(schemaVersion = 2)) }
        assertThrows(IllegalArgumentException::class.java) { repository.write(state.copy(recipes = emptyList())) }
        assertEquals(state, repository.read())
    }
    @Test fun emptyStoreIsCleanAndDoesNotReadLegacyStore() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("cookx_structured_empty_test", 0).edit().clear().commit()
        val clean = KitchenRepository(context, "cookx_structured_empty_test").read()
        assertNull(clean.family)
        assertTrue(clean.recipes.isEmpty() && clean.records.isEmpty())
        assertEquals(33, clean.presetTags.size)
    }
}
