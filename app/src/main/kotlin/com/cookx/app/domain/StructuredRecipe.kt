package com.cookx.app.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class RecipeIngredient(
    val id: String, val name: String, val quantityText: String? = null,
    val quantity: Double? = null, val unit: String? = null,
    val evidence: List<EvidenceRef> = emptyList(),
) {
    fun amountText(): String? = quantityText?.takeIf { it.isNotBlank() }
        ?: quantity?.let { (if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()) + unit.orEmpty() }
    fun displayText() = listOfNotNull(name, amountText()).joinToString(" ")
}
@Serializable enum class PhaseType(val label: String) {
    @SerialName("prep") PREP("备菜"), @SerialName("cook") COOK("烹饪")
}
@Serializable enum class StepKind(val label: String) {
    @SerialName("custom") CUSTOM("自定义"), @SerialName("template") TEMPLATE("标准操作")
}
@Serializable enum class OperationType(val label: String) {
    @SerialName("rinse") RINSE("清洗"), @SerialName("peel") PEEL("去皮"),
    @SerialName("cut") CUT("切块"), @SerialName("slice") SLICE("切片"),
    @SerialName("shred") SHRED("切丝"), @SerialName("dice") DICE("切丁"),
    @SerialName("mix") MIX("搅拌"), @SerialName("blanch") BLANCH("焯水"), @SerialName("marinate") MARINATE("腌制"),
    @SerialName("stir_fry") STIR_FRY("翻炒"), @SerialName("saute") SAUTE("炒香"),
    @SerialName("simmer") SIMMER("炖煮"), @SerialName("season") SEASON("调味"),
    @SerialName("plate") PLATE("装盘"), @SerialName("other") OTHER("其他"),
}
@Serializable data class RecipePhase(val type: PhaseType, val steps: List<RecipeStep> = emptyList())
@Serializable data class StepIngredientUse(
    val ingredientId: String, val quantityText: String? = null,
    val quantity: Double? = null, val unit: String? = null,
) {
    fun displayText(ingredients: List<RecipeIngredient>): String {
        val name = ingredients.single { it.id == ingredientId }.name
        return RecipeIngredient(ingredientId, name, quantityText, quantity, unit).displayText()
    }
}
@Serializable data class StepParameters(
    val heat: String? = null, val temperature: String? = null,
    val cutSize: String? = null, val endCondition: String? = null,
)
@Serializable data class StepTimer(val durationSeconds: Int, val startCondition: String = "手动开始")
@Serializable data class TemplateReference(val id: String, val version: Int, val bindings: Map<String, String>, val values: Map<String, TemplateValue> = emptyMap())
@Serializable data class RecipeStep(
    val id: String, val title: String, val description: String,
    val kind: StepKind = StepKind.CUSTOM, val operationType: OperationType = OperationType.OTHER,
    val ingredients: List<StepIngredientUse> = emptyList(), val parameters: StepParameters = StepParameters(),
    val timer: StepTimer? = null, val imageUri: String? = null,
    val templateRef: TemplateReference? = null, val evidence: List<EvidenceRef> = emptyList(),
)
@Serializable data class RecipeSource(val platform: String, val url: String, val title: String? = null,
    val author: String? = null, val noteId: String? = null)
@Serializable enum class EvidenceType { BODY, IMAGE }
@Serializable data class EvidenceRef(val type: EvidenceType, val index: Int, val quote: String? = null)
@Serializable data class ReviewItem(val code: String, val message: String,
    val ingredientIds: List<String> = emptyList(), val stepIds: List<String> = emptyList())

/** A flattened playback view, never a second stored representation of recipe steps. */
data class PhasedStep(val phase: PhaseType, val phaseIndex: Int, val phaseSize: Int, val step: RecipeStep)
fun Recipe.orderedSteps(): List<PhasedStep> = phases.flatMap { phase ->
    phase.steps.mapIndexed { index, step -> PhasedStep(phase.type, index + 1, phase.steps.size, step) }
}
fun emptyRecipe(familyId: String) = Recipe(newId(), familyId, "", emptyList())
fun Recipe.putStep(phase: PhaseType, step: RecipeStep): Recipe = copy(phases = phases.map { current ->
    if (current.type == phase) {
        val exists = current.steps.any { it.id == step.id }
        current.copy(steps = if (exists) current.steps.map { if (it.id == step.id) step else it } else current.steps + step)
    } else current.copy(steps = current.steps.filterNot { it.id == step.id })
})
fun Recipe.removeStep(id: String) = copy(phases = phases.map { it.copy(steps = it.steps.filterNot { step -> step.id == id }) })
fun Recipe.moveStep(phase: PhaseType, index: Int, offset: Int): Recipe = copy(phases = phases.map {
    if (it.type != phase || index + offset !in it.steps.indices) it else it.copy(steps = it.steps.toMutableList().apply { add(index + offset, removeAt(index)) })
})

fun validateStep(step: RecipeStep, ingredients: List<RecipeIngredient>) {
    require(step.id.isNotBlank() && step.title.isNotBlank()) { "请填写步骤标题" }
    require(step.description.isNotBlank()) { "请填写步骤说明" }
    require(step.ingredients.map { it.ingredientId }.distinct().size == step.ingredients.size) { "步骤不能重复关联同一食材" }
    require(step.ingredients.all { use -> ingredients.any { it.id == use.ingredientId } }) { "步骤关联了不存在的食材" }
    step.ingredients.forEach { validateQuantity(it.quantity) }
    step.timer?.let { require(it.durationSeconds > 0 && it.startCondition.isNotBlank()) { "计时需大于 0 秒，并填写启动条件" } }
    if (step.kind == StepKind.TEMPLATE) {
        val ref = requireNotNull(step.templateRef) { "标准步骤缺少模板来源" }
        require(ref.id.isNotBlank() && ref.version > 0 && ref.bindings.isNotEmpty()) { "请选择模板对应食材" }
        require(ref.bindings.values.all { bound -> step.ingredients.any { it.ingredientId == bound } }) { "模板食材关联不完整" }
    }
    require(step.evidence.all { it.index > 0 }) { "来源索引必须大于 0" }
}
private fun validateQuantity(quantity: Double?) {
    require(quantity == null || quantity.isFinite() && quantity > 0) { "食材数量必须大于 0" }
}
fun validateRecipe(recipe: Recipe): Recipe {
    require(recipe.id.isNotBlank() && recipe.familyId.isNotBlank()) { "菜谱标识不能为空" }
    require(recipe.name.isNotBlank()) { "请填写菜名" }
    require(recipe.ingredients.isNotEmpty()) { "请至少添加一种食材" }
    require(recipe.ingredients.map { it.id }.toSet().size == recipe.ingredients.size) { "食材标识重复" }
    recipe.ingredients.forEach {
        require(it.id.isNotBlank() && it.name.isNotBlank()) { "请填写食材名称" }
        validateQuantity(it.quantity)
        require(it.evidence.all { ref -> ref.index > 0 }) { "来源索引必须大于 0" }
    }
    require(recipe.phases.map { it.type } == listOf(PhaseType.PREP, PhaseType.COOK)) { "菜谱需依次包含备菜和烹饪阶段" }
    val steps = recipe.orderedSteps().map { it.step }
    require(steps.isNotEmpty()) { "请至少添加一个步骤" }
    require(steps.map { it.id }.distinct().size == steps.size) { "步骤标识重复" }
    steps.forEach { validateStep(it, recipe.ingredients) }
    require(recipe.servings == null || recipe.servings > 0) { "份数需大于 0" }
    normalizeTags(recipe.tags)
    recipe.reviewItems.forEach { review ->
        require(review.code.isNotBlank() && review.message.isNotBlank()) { "待确认项不能为空" }
        require(review.ingredientIds.all { id -> recipe.ingredients.any { it.id == id } } && review.stepIds.all { id -> steps.any { it.id == id } }) { "待确认项引用不存在" }
    }
    return recipe
}
fun validateKitchen(state: KitchenState) {
    require(state.schemaVersion == CURRENT_SCHEMA_VERSION) { "数据版本不匹配" }
    val familyId = state.family?.id
    require(familyId != null || state.recipes.isEmpty() && state.records.isEmpty() && state.pantryItems.isEmpty() && state.shoppingMemos.isEmpty()) { "家庭数据不完整" }
    require(state.recipes.map { it.id }.distinct().size == state.recipes.size) { "菜谱标识重复" }
    state.recipes.forEach { require(it.familyId == familyId); validateRecipe(it) }
    state.records.forEach { record ->
        require(record.familyId == familyId && state.recipes.any { it.id == record.recipeId }) { "制作记录归属不匹配" }
        dateToUtcMillis(record.cookingDate)
    }
    require(state.pantryItems.all { it.familyId == familyId }) { "库存归属不匹配" }
    require(state.pantryItems.map { it.id }.distinct().size == state.pantryItems.size) { "物品标识重复" }
    state.pantryItems.forEach { normalizedPantryItem(it) }
    state.shoppingMemos.forEach { memo ->
        val recipe = state.recipes.singleOrNull { it.id == memo.recipeId }
        require(memo.familyId == familyId && recipe != null) { "便签归属不匹配" }
        require(memo.items.all { item -> recipe.ingredients.any { it.id == item.key } }) { "便签引用了不存在的食材" }
    }
}
