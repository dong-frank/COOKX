package com.cookx.app.domain

import kotlinx.serialization.Serializable

const val CURRENT_SCHEMA_VERSION = 3
fun newId(): String = java.util.UUID.randomUUID().toString()

@Serializable data class Family(val id: String, val name: String)
@Serializable data class Recipe(
    val id: String, val familyId: String, val name: String,
    val ingredients: List<RecipeIngredient>,
    val phases: List<RecipePhase> = listOf(RecipePhase(PhaseType.PREP), RecipePhase(PhaseType.COOK)),
    val tags: List<String> = emptyList(), val servings: Int? = null,
    val coverUri: String? = null, val source: RecipeSource? = null,
    val reviewItems: List<ReviewItem> = emptyList(), val isExample: Boolean = false,
)
@Serializable data class CookingRecord(
    val id: String, val familyId: String, val recipeId: String,
    val completedAt: Long, val note: String, val cookingDate: String, val meal: Meal,
)
@Serializable data class KitchenState(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    @kotlinx.serialization.Transient val presetTags: List<String> = emptyList(),
    val family: Family? = null, val recipes: List<Recipe> = emptyList(),
    val records: List<CookingRecord> = emptyList(),
    val customTags: List<String> = emptyList(), val favoriteTags: List<String> = DEFAULT_FAVORITE_TAGS,
    val pantryItems: List<PantryItem> = emptyList(), val shoppingMemos: List<ShoppingMemo> = emptyList(),
)
