package com.cookx.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import android.content.pm.ApplicationInfo
import com.cookx.app.data.PresetRepository
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cookx.app.domain.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val RecipeSaver = Saver<Recipe, String>(save = { Json.encodeToString(it) }, restore = { Json.decodeFromString(it) })
private val StepSaver = Saver<RecipeStep, String>(save = { Json.encodeToString(it) }, restore = { Json.decodeFromString(it) })
@Serializable private data class EditingStep(val phase: PhaseType, val step: RecipeStep)

@Composable internal fun StructuredRecipeEditor(familyId: String, availableTags: List<String>, busy: Boolean, save: (Recipe) -> Unit) {
    var recipe by rememberSaveable(stateSaver = RecipeSaver) { mutableStateOf(emptyRecipe(familyId)) }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var servings by rememberSaveable { mutableStateOf("") }
    var showAi by rememberSaveable { mutableStateOf(false) }
    val debug = LocalContext.current.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    if (showAi && debug) {
        AiRecipeChat(familyId, availableTags,
            replacesDraft = recipe.name.isNotBlank() || servings.isNotBlank() || recipe.tags.isNotEmpty() ||
                recipe.ingredients.isNotEmpty() || recipe.orderedSteps().isNotEmpty(),
            dismiss = { showAi = false }) { draft ->
            recipe = draft
            servings = draft.servings?.toString().orEmpty()
            error = null
            showAi = false
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("记录菜谱", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
            if (debug) OutlinedButton(onClick = { showAi = true }, enabled = !busy) { Text("AI 对话") }
        }
        XiaohongshuSourceLink(recipe.source)
        if (recipe.reviewItems.isNotEmpty()) {
            Text("待核对", style = MaterialTheme.typography.titleMedium)
            recipe.reviewItems.forEach { item -> Text(item.message, style = MaterialTheme.typography.bodyMedium) }
        }
        OutlinedTextField(recipe.name, { recipe = recipe.copy(name = it) }, label = { Text("菜名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(servings, { servings = it }, label = { Text("份数（选填）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        TagSelector(availableTags, recipe.tags) { recipe = recipe.copy(tags = it) }
        HorizontalDivider()
        Text("食材", style = MaterialTheme.typography.titleLarge)
        recipe.ingredients.forEachIndexed { index, ingredient -> key(ingredient.id) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(ingredient.name, { value -> recipe = recipe.copy(ingredients = recipe.ingredients.map { if (it.id == ingredient.id) it.copy(name = value) else it }) },
                        label = { Text("食材名称") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("ingredientName:$index"))
                    OutlinedTextField(ingredient.quantityText.orEmpty(), { value -> recipe = recipe.copy(ingredients = recipe.ingredients.map { if (it.id == ingredient.id) it.copy(quantityText = value.ifBlank { null }) else it }) },
                        label = { Text("用量（选填）") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("ingredientAmount:$index"))
                    TextButton(onClick = {
                        if (recipe.orderedSteps().any { node -> node.step.ingredients.any { it.ingredientId == ingredient.id } }) error = "请先移除步骤中的食材关联"
                        else recipe = recipe.copy(ingredients = recipe.ingredients.filterNot { it.id == ingredient.id })
                    }) { Text("删除食材") }
                }
            }
        } }
        OutlinedButton(onClick = { recipe = recipe.copy(ingredients = recipe.ingredients + RecipeIngredient(newId(), "")) }) { Text("添加食材") }
        recipe.phases.forEach { phase ->
            HorizontalDivider()
            Text(phase.type.label, style = MaterialTheme.typography.titleLarge)
            phase.steps.forEachIndexed { index, step ->
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OperationBadge(step.operationType)
                            Text(step.title, Modifier.weight(1f).padding(start = 10.dp), style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = { editing = Json.encodeToString(EditingStep(phase.type, step)) }) { Text("编辑") }
                        }
                        Row {
                            TextButton(onClick = { recipe = recipe.moveStep(phase.type, index, -1) }, enabled = index > 0) { Text("上移") }
                            TextButton(onClick = { recipe = recipe.moveStep(phase.type, index, 1) }, enabled = index < phase.steps.lastIndex) { Text("下移") }
                            TextButton(onClick = { recipe = recipe.removeStep(step.id) }) { Text("删除") }
                        }
                    }
                }
            }
            OutlinedButton(onClick = {
                if (recipe.ingredients.any { it.name.isBlank() } || recipe.ingredients.isEmpty()) error = "请先填写食材"
                else editing = Json.encodeToString(EditingStep(phase.type, RecipeStep(newId(), "", "")))
            }, modifier = Modifier.testTag("addStep:${phase.type.name}")) { Text("添加${phase.type.label}步骤") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            runCatching {
                val count = if (servings.isBlank()) null else servings.toIntOrNull() ?: error("份数需为正整数")
                validateRecipe(recipe.copy(name = recipe.name.trim(), servings = count))
            }.onSuccess { error = null; save(it) }.onFailure { error = it.message }
        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("保存菜谱") }
    }
    editing?.let { json ->
        val initial = Json.decodeFromString<EditingStep>(json)
        StepEditorDialog(initial.phase, initial.step, recipe.ingredients, onDismiss = { editing = null }) { phase, step ->
            recipe = recipe.putStep(phase, step); editing = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable private fun StepEditorDialog(initialPhase: PhaseType, initialStep: RecipeStep, ingredients: List<RecipeIngredient>,
    onDismiss: () -> Unit, save: (PhaseType, RecipeStep) -> Unit) {
    var step by rememberSaveable(stateSaver = StepSaver) { mutableStateOf(initialStep) }
    var phase by rememberSaveable { mutableStateOf(initialPhase.name) }
    var seconds by rememberSaveable { mutableStateOf(initialStep.timer?.durationSeconds?.toString().orEmpty()) }
    var condition by rememberSaveable { mutableStateOf(initialStep.timer?.startCondition ?: "手动开始") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val templates = remember(context) { PresetRepository(context).templates }
    val template = templates.find { it.id == step.templateRef?.id && it.version == step.templateRef?.version }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize()) {
            Scaffold(contentWindowInsets = WindowInsets.safeDrawing, topBar = {
                TopAppBar(title = { Text("步骤") }, navigationIcon = { TextButton(onClick = onDismiss) { Text("取消") } }, actions = {
                    TextButton(onClick = {
                        runCatching {
                            val resolved = if (step.kind == StepKind.TEMPLATE && template != null)
                                applyOperationTemplate(template, step.templateRef!!.values, ingredients, step.id)
                            else step
                            val timer = if (step.kind == StepKind.TEMPLATE) resolved.timer else if (seconds.isBlank()) null else StepTimer(seconds.toIntOrNull() ?: error("请填写有效秒数"), condition.trim())
                            resolved.copy(timer = timer).also { validateStep(it, ingredients) }
                        }.onSuccess { save(PhaseType.valueOf(phase), it) }.onFailure { error = it.message }
                    }) { Text("保存步骤") }
                })
            }) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PhaseType.entries.forEach { item -> FilterChip(phase == item.name, { phase = item.name }, label = { Text(item.label) }) }
                    }
                    var templateMenu by remember { mutableStateOf(false) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(onClick = {
                            val resolved = template?.let { runCatching { applyOperationTemplate(it, step.templateRef!!.values, ingredients, step.id) }.getOrNull() } ?: step
                            step = resolved.copy(kind = StepKind.CUSTOM, templateRef = null)
                            seconds = step.timer?.durationSeconds?.toString().orEmpty(); condition = step.timer?.startCondition ?: "手动开始"
                        }) { Text("自定义步骤") }
                        Box {
                            TextButton(onClick = { templateMenu = true }) { Text("标准操作") }
                            DropdownMenu(templateMenu, { templateMenu = false }) {
                                templates.forEach { entry -> DropdownMenuItem(text = { Text(entry.name) }, onClick = {
                                    step = RecipeStep(step.id, entry.name, entry.actions.joinToString("\n") { it.instruction }, StepKind.TEMPLATE, entry.operation,
                                        templateRef = TemplateReference(entry.id, entry.version, emptyMap(), entry.parameters.associate { it.id to TemplateValue(text = it.defaultText.orEmpty()) }))
                                    phase = entry.phase.name; seconds = ""; condition = "手动开始"; templateMenu = false
                                }) }
                            }
                        }
                    }
                    OutlinedTextField(step.title, { step = step.copy(title = it) }, label = { Text("步骤标题") }, singleLine = true,
                        readOnly = step.kind == StepKind.TEMPLATE, modifier = Modifier.fillMaxWidth())
                    if (step.kind == StepKind.CUSTOM) OutlinedTextField(step.description, { step = step.copy(description = it) }, label = { Text("步骤说明") }, minLines = 3,
                        readOnly = step.kind == StepKind.TEMPLATE, modifier = Modifier.fillMaxWidth())
                    var operationMenu by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { operationMenu = true }, enabled = step.kind == StepKind.CUSTOM) {
                            OperationBadge(step.operationType); Text(step.operationType.label, Modifier.padding(start = 10.dp))
                        }
                        DropdownMenu(operationMenu, { operationMenu = false }, modifier = Modifier.heightIn(max = 360.dp)) {
                            OperationType.entries.forEach { operation -> DropdownMenuItem(text = { Text(operation.label) }, onClick = {
                                step = step.copy(operationType = operation); operationMenu = false
                            }, modifier = Modifier.testTag("operation:${operation.name}")) }
                        }
                    }
                    if (step.kind == StepKind.TEMPLATE && template != null) {
                        TemplateFields(template, step.templateRef!!.values, ingredients) { id, value ->
                            step = step.copy(templateRef = step.templateRef!!.copy(values = updateTemplateValue(template, step.templateRef!!.values, id, value)))
                        }
                    } else if (step.kind == StepKind.CUSTOM) {
                        ingredients.forEach { ingredient ->
                            val use = step.ingredients.find { it.ingredientId == ingredient.id }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(use != null, { checked -> step = step.copy(ingredients = if (checked) step.ingredients + StepIngredientUse(ingredient.id) else step.ingredients.filterNot { it.ingredientId == ingredient.id }) }, modifier = Modifier.testTag("stepIngredient:${ingredient.id}"))
                                Text(ingredient.name)
                            }
                            if (use != null) OutlinedTextField(use.quantityText.orEmpty(), { value ->
                                step = step.copy(ingredients = step.ingredients.map { if (it.ingredientId == ingredient.id) it.copy(quantityText = value.ifBlank { null }) else it })
                            }, label = { Text("${ingredient.name}本步用量（选填）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                    }
                    if (step.kind == StepKind.CUSTOM) {
                    OutlinedTextField(step.parameters.heat.orEmpty(), { step = step.copy(parameters = step.parameters.copy(heat = it.ifBlank { null })) }, label = { Text("火候（选填）") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(step.parameters.cutSize.orEmpty(), { step = step.copy(parameters = step.parameters.copy(cutSize = it.ifBlank { null })) }, label = { Text("切制尺寸（选填）") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(step.parameters.temperature.orEmpty(), { step = step.copy(parameters = step.parameters.copy(temperature = it.ifBlank { null })) }, label = { Text("温度（选填）") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(step.parameters.endCondition.orEmpty(), { step = step.copy(parameters = step.parameters.copy(endCondition = it.ifBlank { null })) }, label = { Text("完成标准（选填）") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(seconds, { seconds = it }, label = { Text("计时秒数（选填）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (seconds.isNotBlank()) OutlinedTextField(condition, { condition = it }, label = { Text("计时启动条件") }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        error?.let { message -> AlertDialog(onDismissRequest = { error = null }, text = { Text(message) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("确定") } }) }
    }
}
