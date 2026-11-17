package com.cookx.app.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class PresetTag(val id: String, val name: String)
@Serializable data class TagCatalog(val version: Int, val tags: List<PresetTag>)
@Serializable data class TemplateCatalog(val version: Int, val templates: List<OperationTemplate>)
@Serializable data class ParameterCondition(val parameter: String, val equals: Boolean? = null, val isSet: Boolean? = null)
@Serializable data class TemplateParameter(
    val id: String, val name: String, val type: String, val required: Boolean = false,
    val enabledWhen: ParameterCondition? = null, val requiredWhen: ParameterCondition? = null,
    val requiredWhenEnabled: Boolean = false, val quantityRequired: Boolean = false,
    val ingredientListParameter: String? = null, val defaultText: String? = null,
)
@Serializable data class TemplateAction(
    val id: String, val name: String, val uses: List<String>, val instruction: String,
    val enabledWhen: ParameterCondition? = null, val omitWhenEmpty: String? = null,
    val instructionWhenDurationMissing: String? = null,
)
@Serializable data class TemplateTimer(
    val durationParameter: String, val availableAtAction: String,
    val startConditionParameter: String? = null, val startConditionText: String? = null,
)
@Serializable data class OperationTemplate(
    val id: String, val version: Int, val name: String, val operationType: String,
    val suggestedStage: String, val parameters: List<TemplateParameter>, val actions: List<TemplateAction>,
    val timer: TemplateTimer? = null,
) {
    val operation: OperationType get() = when (operationType) {
        "saute-aromatics" -> OperationType.SAUTE
        else -> OperationType.entries.first { Json.encodeToString(it).trim('"') == operationType }
    }
    val phase: PhaseType get() = if (suggestedStage == "cooking") PhaseType.COOK else PhaseType.PREP
}
@Serializable data class TemplateValue(
    val text: String = "", val enabled: Boolean = false,
    val ingredients: List<StepIngredientUse> = emptyList(), val ordered: Boolean = false,
    val order: List<String> = emptyList(),
)
private val catalogJson = Json { ignoreUnknownKeys = true }
fun parseTagCatalog(raw: String): TagCatalog = catalogJson.decodeFromString<TagCatalog>(raw).also {
    require(it.version > 0 && it.tags.isNotEmpty())
    require(it.tags.map { tag -> tag.id }.distinct().size == it.tags.size)
    require(it.tags.all { tag -> tag.id.isNotBlank() && tag.name.isNotBlank() })
    require(normalizeTags(it.tags.map { tag -> tag.name }).size == it.tags.size)
}
fun parseTemplateCatalog(raw: String): TemplateCatalog = catalogJson.decodeFromString<TemplateCatalog>(raw).also {
    require(it.version > 0 && it.templates.map { t -> t.id }.distinct().size == it.templates.size)
    it.templates.forEach { t ->
        require(t.version > 0 && t.id.isNotBlank() && t.name.isNotBlank())
        t.operation
        val ids = t.parameters.map { p -> p.id }.toSet()
        require(ids.size == t.parameters.size)
        require(t.parameters.all { p -> p.type in setOf("text", "boolean", "duration", "ingredient-use", "ingredient-use-list", "ingredient-order") })
        require(t.actions.all { a -> a.uses.all { id -> id in ids } })
    }
}
fun ParameterCondition.matches(values: Map<String, TemplateValue>): Boolean = when {
    equals != null -> (values[parameter]?.enabled ?: false) == equals
    isSet != null -> !values[parameter]?.text.isNullOrBlank() == isSet
    else -> false
}
fun TemplateParameter.isEnabled(values: Map<String, TemplateValue>) = enabledWhen?.matches(values) != false

/** Resolve once at authoring time. Playback never looks up a live template. */
fun applyOperationTemplate(template: OperationTemplate, values: Map<String, TemplateValue>, ingredients: List<RecipeIngredient>, stepId: String = newId()): RecipeStep {
    val active = template.parameters.filter { it.isEnabled(values) }
    val uses = mutableListOf<StepIngredientUse>()
    val bindings = linkedMapOf<String, String>()
    val resolved = linkedMapOf<String, String>()
    active.forEach { p ->
        val value = values[p.id] ?: TemplateValue(text = p.defaultText.orEmpty())
        val required = p.required || p.requiredWhenEnabled || p.requiredWhen?.matches(values) == true
        when (p.type) {
            "ingredient-use", "ingredient-use-list" -> {
                require(!required || value.ingredients.isNotEmpty()) { "请选择${p.name}" }
                require(p.type != "ingredient-use" || value.ingredients.size <= 1) { "${p.name}只能选择一种食材" }
                require(value.ingredients.map { it.ingredientId }.distinct().size == value.ingredients.size) { "${p.name}存在重复食材" }
                value.ingredients.forEachIndexed { index, use ->
                    require(ingredients.any { it.id == use.ingredientId }) { "模板关联了不存在的食材" }
                    require(!p.quantityRequired || !use.quantityText.isNullOrBlank() || use.quantity != null) { "请填写${p.name}的用量" }
                    bindings["${p.id}:$index"] = use.ingredientId
                    uses += use
                }
                resolved[p.id] = value.ingredients.joinToString("、") { it.displayText(ingredients) }
            }
            "boolean" -> Unit
            "ingredient-order" -> {
                val selected = values[p.ingredientListParameter]?.ingredients.orEmpty().map { it.ingredientId }
                if (value.ordered) require(value.order.size == selected.size && value.order.toSet() == selected.toSet()) { "下料顺序需包含每种已选香料且不重复" }
                resolved[p.id] = if (value.ordered) value.order.joinToString(" → ") { id -> ingredients.single { it.id == id }.name } else "一起下"
            }
            "duration" -> {
                if (value.text.isNotBlank()) {
                    val minutes = value.text.toDoubleOrNull()
                    require(minutes != null && minutes.isFinite() && minutes > 0 && minutes * 60 <= Int.MAX_VALUE && minutes * 60 >= 1) { "时长需为大于 0 的分钟数" }
                    resolved[p.id] = "${value.text} 分钟"
                } else require(!required) { "请填写${p.name}" }
            }
            else -> {
                require(!required || value.text.isNotBlank()) { "请填写${p.name}" }
                resolved[p.id] = value.text.trim()
            }
        }
    }
    val timer = template.timer?.let { config ->
        values[config.durationParameter]?.text?.takeIf { it.isNotBlank() }?.let { duration ->
            val start = config.startConditionParameter?.let { values[it]?.text?.trim() }
                ?: config.startConditionText ?: "${template.actions.single { it.id == config.availableAtAction }.name}开始时"
            require(start.isNotBlank()) { "请填写计时起点" }
            StepTimer((duration.toDouble() * 60).toInt(), start)
        }
    }
    val description = template.actions.filter { action ->
        action.enabledWhen?.matches(values) != false && (action.omitWhenEmpty == null || !resolved[action.omitWhenEmpty].isNullOrBlank())
    }.joinToString("\n\n") { action ->
        var instruction = if (timer == null && action.instructionWhenDurationMissing != null) action.instructionWhenDurationMissing else action.instruction
        resolved.forEach { (id, text) -> instruction = instruction.replace("{$id}", text) }
        // The duration placeholder already includes its unit.
        instruction = instruction.replace("分钟 分钟", "分钟")
        val details = action.uses.mapNotNull { id -> resolved[id]?.takeIf { it.isNotBlank() }?.let { value ->
            "${template.parameters.single { it.id == id }.name}：$value"
        } }
        (listOf(action.name, instruction) + details).joinToString("\n")
    }
    val groupedUses = uses.groupBy { it.ingredientId }.map { (_, entries) ->
        val first = entries.first()
        if (entries.size == 1) first else first.copy(quantity = null, unit = null,
            quantityText = entries.mapNotNull { it.quantityText }.takeIf { it.isNotEmpty() }?.joinToString("；"))
    }
    return RecipeStep(stepId, template.name, description, StepKind.TEMPLATE, template.operation,
        ingredients = groupedUses, timer = timer,
        parameters = StepParameters(heat = resolved["heat"], cutSize = resolved["size"], endCondition = resolved["target"]),
        templateRef = TemplateReference(template.id, template.version, bindings, values.toMap())).also { validateStep(it, ingredients) }
}
