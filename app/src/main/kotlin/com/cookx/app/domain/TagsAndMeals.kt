package com.cookx.app.domain

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

val PRESET_TAGS = listOf("快手菜", "川菜", "粤菜", "宴客必备", "拿手菜", "高难度", "家常菜", "素菜", "汤羹", "主食")
val DEFAULT_FAVORITE_TAGS = listOf("快手菜", "拿手菜", "川菜", "粤菜", "宴客必备", "高难度")
fun normalizeTags(tags: List<String>): List<String> = tags.map(String::trim).filter(String::isNotEmpty).distinct().also {
    require(it.none { name -> name in listOf("全部", "未打标签") }) { "这个名称保留给系统入口，请换一个标签名" }
    require(it.all { name -> name.length <= 20 }) { "标签名称最多 20 个字" }
}
fun KitchenState.availableTags() = (PRESET_TAGS + customTags + recipes.flatMap { it.tags }).distinct()
fun Recipe.matchesTag(tag: String) = when (tag) {
    "全部" -> true
    "未打标签" -> tags.isEmpty()
    else -> tag in tags
}

@Serializable enum class Meal(val label: String) {
    BREAKFAST("早餐"), LUNCH("午餐"), DINNER("晚餐"), LATE_NIGHT("夜宵")
}
@Serializable data class MealSelection(val date: String, val meal: Meal)
fun mealForHour(hour: Int): Meal {
    require(hour in 0..23)
    return when (hour) { in 5..9 -> Meal.BREAKFAST; in 10..14 -> Meal.LUNCH; in 15..20 -> Meal.DINNER; else -> Meal.LATE_NIGHT }
}
fun mealAt(timestamp: Long, zone: TimeZone = TimeZone.getDefault()): MealSelection {
    val calendar = Calendar.getInstance(zone).apply { timeInMillis = timestamp }
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { timeZone = zone }.format(Date(timestamp))
    return MealSelection(date, mealForHour(calendar.get(Calendar.HOUR_OF_DAY)))
}
fun dateToUtcMillis(date: String): Long {
    require(Regex("\\d{4}-\\d{2}-\\d{2}").matches(date)) { "请选择有效日期" }
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false; timeZone = TimeZone.getTimeZone("UTC") }
    return requireNotNull(format.parse(date)).time
}
fun utcMillisToDate(value: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
    .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(value))
fun CookingRecord.selection(): MealSelection =
    if (cookingDate != null && meal != null) MealSelection(cookingDate, meal) else mealAt(completedAt)
fun CookingRecord.mealLabel(): String = selection().let { "${it.date} · ${it.meal.label}" }
fun KitchenState.recordsFor(recipeId: String) = records.filter { it.recipeId == recipeId }
    .sortedWith(compareByDescending<CookingRecord> { it.selection().date }
        .thenByDescending { it.selection().meal.ordinal }.thenByDescending { it.completedAt })

/** Pure, idempotent migration; the repository keeps the original JSON before persisting it. */
fun migrateKitchen(state: KitchenState, zone: TimeZone = TimeZone.getDefault()): KitchenState {
    if (state.schemaVersion >= 2) return state
    val legacyTags = state.recipes.map { it.category.trim() }.filter { it.isNotEmpty() && it != "未分类" }.distinct()
    return state.copy(
        recipes = state.recipes.map { recipe -> recipe.copy(category = "未分类",
            tags = (recipe.tags + recipe.category.takeIf { it.isNotBlank() && it != "未分类" }.let { listOfNotNull(it) }).distinct()) },
        records = state.records.map { record ->
            if (record.cookingDate != null && record.meal != null) record else {
                val selected = mealAt(record.completedAt, zone)
                record.copy(cookingDate = selected.date, meal = selected.meal, mealInferred = true)
            }
        },
        customTags = (state.customTags + legacyTags.filter { it !in PRESET_TAGS }).distinct(),
        favoriteTags = (state.favoriteTags + legacyTags).distinct(), schemaVersion = 2,
    )
}
