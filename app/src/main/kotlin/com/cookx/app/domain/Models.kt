package com.cookx.app.domain

import kotlinx.serialization.Serializable

@Serializable data class Family(val id: String, val name: String)
@Serializable data class Recipe(
    val id: String, val familyId: String, val name: String,
    val ingredients: List<String>, val steps: List<String>, val isExample: Boolean = false,
    val category: String = "未分类", // Legacy field, consumed only by migration.
    val tags: List<String> = emptyList(),
)
@Serializable data class CookingRecord(
    val id: String, val familyId: String, val recipeId: String,
    val completedAt: Long, val note: String,
    val cookingDate: String? = null, val meal: Meal? = null, val mealInferred: Boolean = false,
)
@Serializable data class KitchenState(
    val family: Family? = null, val recipes: List<Recipe> = emptyList(),
    val records: List<CookingRecord> = emptyList(),
    val customTags: List<String> = emptyList(),
    val favoriteTags: List<String> = DEFAULT_FAVORITE_TAGS,
    val schemaVersion: Int = 1,
) {
    fun lastCooked(recipeId: String): Long? = records.filter { it.recipeId == recipeId }
        .maxOfOrNull { it.completedAt }
}

fun recipeDraft(familyId: String, name: String, ingredients: String, steps: String, tags: List<String> = emptyList()): Recipe {
    fun lines(value: String) = value.lines().map(String::trim).filter(String::isNotEmpty)
    require(name.isNotBlank()) { "请填写菜名" }
    require(lines(ingredients).isNotEmpty()) { "请至少填写一种食材" }
    require(lines(steps).isNotEmpty()) { "请至少填写一个步骤" }
    return Recipe(java.util.UUID.randomUUID().toString(), familyId, name.trim(), lines(ingredients), lines(steps), tags = normalizeTags(tags))
}

fun exampleRecipes(familyId: String) = listOf(
    Recipe("example-tomato-eggs", familyId, "番茄炒蛋",
        listOf("番茄 2 个", "鸡蛋 3 个", "食用油、盐 适量"),
        listOf("番茄洗净切块，鸡蛋打散。", "热锅加油，倒入蛋液，凝固后盛出。", "炒软番茄，加入鸡蛋翻炒，用盐调味后盛出。"), true, tags = listOf("家常菜", "快手菜")),
    Recipe("example-mushrooms", familyId, "清炒口蘑",
        listOf("口蘑 250 克", "蒜 2 瓣", "食用油、盐 适量"),
        listOf("口蘑洗净切片，蒜切碎。", "热锅加油，炒香蒜末。", "加入口蘑炒熟，按口味加盐后盛出。"), true, tags = listOf("素菜", "快手菜")),
)
