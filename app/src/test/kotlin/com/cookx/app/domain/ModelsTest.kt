package com.cookx.app.domain

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ModelsTest {
    @Test fun cleanStateHasNoImplicitHouseholdOrExamples() {
        val state = KitchenState()
        validateKitchen(state)
        assertEquals(3, state.schemaVersion)
        assertNull(state.family)
        assertTrue(state.recipes.isEmpty() && state.records.isEmpty() && state.pantryItems.isEmpty() && state.shoppingMemos.isEmpty())
    }
    @Test fun structuredRecipeRoundTripPreservesSourceAndUnknownAmounts() {
        val recipe = exampleRecipes("f").first().copy(source = RecipeSource("xiaohongshu", "https://example.com/note", "原帖"),
            reviewItems = listOf(ReviewItem("MISSING_AMOUNT", "油量需确认", listOf("oil"))))
        val missing = recipe.copy(ingredients = recipe.ingredients.map { if (it.id == "oil") it.copy(quantityText = null) else it })
        assertNull(missing.ingredients.single { it.id == "oil" }.amountText())
        assertEquals(missing, Json.decodeFromString<Recipe>(Json.encodeToString(missing)))
        validateRecipe(missing)
    }
    @Test fun brokenReferencesDuplicateIdsAndInvalidTimersAreRejected() {
        val recipe = exampleRecipes("f").first()
        val first = recipe.orderedSteps().first().step
        assertThrows(IllegalArgumentException::class.java) { validateRecipe(recipe.copy(ingredients = recipe.ingredients + recipe.ingredients.first())) }
        assertThrows(IllegalArgumentException::class.java) { validateStep(first.copy(ingredients = listOf(StepIngredientUse("missing"))), recipe.ingredients) }
        assertThrows(IllegalArgumentException::class.java) { validateStep(first.copy(timer = StepTimer(0)), recipe.ingredients) }
        assertThrows(IllegalArgumentException::class.java) { validateRecipe(recipe.copy(phases = recipe.phases.reversed())) }
        assertThrows(IllegalArgumentException::class.java) { validateRecipe(recipe.copy(ingredients = recipe.ingredients.map { it.copy(quantity = Double.NaN) })) }
    }
    @Test fun phaseOrderAndEditingKeepStableStepIdentity() {
        val recipe = exampleRecipes("f").first()
        assertEquals(listOf(PhaseType.PREP, PhaseType.PREP, PhaseType.COOK, PhaseType.COOK, PhaseType.COOK), recipe.orderedSteps().map { it.phase })
        val first = recipe.orderedSteps().first().step
        val edited = recipe.putStep(PhaseType.PREP, first.copy(title = "新标题"))
        assertEquals(recipe.orderedSteps().map { it.step.id }, edited.orderedSteps().map { it.step.id })
        val moved = edited.putStep(PhaseType.COOK, first)
        assertEquals(1, moved.orderedSteps().count { it.step.id == first.id })
        assertEquals(first.id, moved.phases.last().steps.last().id)
        validateRecipe(moved)
        validateRecipe(recipe.copy(phases = listOf(RecipePhase(PhaseType.PREP), RecipePhase(PhaseType.COOK, recipe.phases.last().steps))))
    }
}
