package com.cookx.app

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cookx.app.data.KitchenRepository
import com.cookx.app.domain.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryMigrationTest {
    @Test fun migrationBacksUpOldJsonAndNewFieldsPersist() {
        // An isolated preference file: leave the user's installed kitchen intact.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("cookx_migration_test", 0)
        val raw = """{"family":{"id":"f","name":"测试家庭"},"recipes":[{"id":"r","familyId":"f","name":"旧菜","ingredients":["盐"],"steps":["炒熟"],"category":"周末菜"}],"records":[]}"""
        assertTrue(preferences.edit().clear().putString("state", raw).commit())
        val repository = KitchenRepository(context, "cookx_migration_test")
        val migrated = repository.read()
        assertEquals(raw, preferences.getString("state_backup_before_tags_meals", null))
        assertEquals(listOf("周末菜"), migrated.recipes.single().tags)
        val updated = migrated.copy(favoriteTags = listOf("川菜", "快手菜"), customTags = listOf("周末菜"),
            records = listOf(CookingRecord("record", "f", "r", 1, "心得", "2026-11-16", Meal.LUNCH)))
        repository.write(updated)
        assertEquals(updated, KitchenRepository(context, "cookx_migration_test").read())
        assertEquals(raw, preferences.getString("state_backup_before_tags_meals", null))
    }
}
