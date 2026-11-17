package com.cookx.app.domain

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

val DEFAULT_FAVORITE_TAGS = listOf("快手菜", "拿手菜", "川菜", "粤菜", "宴客必备", "高难度")
fun normalizeTags(tags: List<String>): List<String> = tags.map(String::trim).filter(String::isNotEmpty).distinct().also {
    require(it.none { name -> name in listOf("全部", "未打标签") }) { "这个名称保留给系统入口，请换一个标签名" }
    require(it.all { name -> name.length <= 20 }) { "标签名称最多 20 个字" }
}
fun KitchenState.availableTags() = (presetTags + customTags + recipes.flatMap { it.tags }).distinct()
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
fun CookingRecord.selection(): MealSelection = MealSelection(cookingDate, meal)
fun CookingRecord.mealLabel(): String = selection().let { "${it.date} · ${it.meal.label}" }
fun KitchenState.recordsFor(recipeId: String) = records.filter { it.recipeId == recipeId }
    .sortedWith(compareByDescending<CookingRecord> { it.selection().date }
        .thenByDescending { it.selection().meal.ordinal }.thenByDescending { it.completedAt })

fun recipeCategories(recipes: List<Recipe>, favoriteTags: List<String>): List<String> =
    (listOf("全部") + favoriteTags.filter { tag -> recipes.any { it.matchesTag(tag) } } +
        listOf("未打标签").filter { recipes.any { recipe -> recipe.tags.isEmpty() } }).distinct()
