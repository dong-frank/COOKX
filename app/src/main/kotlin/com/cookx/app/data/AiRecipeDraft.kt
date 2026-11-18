package com.cookx.app.data

import com.cookx.app.domain.*
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable private data class DraftRequest(
    val text: String, val tags: List<String>, val previous: AiRecipeDraft?, val query: String, val excluded: List<String>, val history: List<String>,
)
@Serializable internal data class DraftReply(val draft: AiRecipeDraft? = null, val reply: String? = null, val query: String? = null)
@Serializable private data class DraftError(val error: String)
@Serializable internal data class AiRecipeDraft(
    val name: String?, val servings: Int?, val tags: List<String>,
    val ingredients: List<DraftIngredient>, val prep: List<DraftStep>,
    val cook: List<DraftStep>, val review: List<String>, val source: RecipeSource? = null,
) {
    fun toRecipe(familyId: String, availableTags: List<String>): Recipe {
        require(ingredients.size <= 30 && prep.size + cook.size <= 30 && review.size <= 8)
        val mapped = ingredients.map {
            RecipeIngredient(newId(), it.name.trim(), it.quantityText?.trim()?.ifEmpty { null }, evidence = it.evidence)
        }
        val needsReview = review.map(String::trim).filter(String::isNotEmpty).toMutableList()
        fun List<DraftStep>.toSteps() = map { step ->
            val uses = step.ingredients.mapNotNull { use ->
                val ingredient = mapped.getOrNull(use.index - 1)
                if (ingredient == null) {
                    needsReview.add("核对「${step.title}」的食材关联")
                    null
                } else {
                    StepIngredientUse(ingredient.id, use.quantityText)
                }
            }.distinctBy { it.ingredientId }
            RecipeStep(
                id = newId(), title = step.title.trim(), description = step.description.trim(),
                operationType = step.operationType, ingredients = uses, parameters = step.parameters, evidence = step.evidence,
                timer = step.durationSeconds?.takeIf { it > 0 }?.let {
                    StepTimer(it, step.startCondition?.trim()?.ifEmpty { null } ?: "手动开始")
                },
            )
        }
        val phases = listOf(RecipePhase(PhaseType.PREP, prep.toSteps()), RecipePhase(PhaseType.COOK, cook.toSteps()))
        return Recipe(
            id = newId(), familyId = familyId, name = name.orEmpty().trim(),
            ingredients = mapped, phases = phases, tags = tags.filter { it in availableTags }.distinct(),
            servings = servings?.takeIf { it > 0 }, source = source, reviewItems = needsReview.distinct().map { ReviewItem("AI_REVIEW", it) },
        )
    }
}

@Serializable internal data class DraftIngredient(val name: String, val quantityText: String?, val evidence: List<EvidenceRef> = emptyList())
@Serializable internal data class DraftUse(val index: Int, val quantityText: String?)
@Serializable internal data class DraftStep(
    val title: String, val description: String, val operationType: OperationType,
    val ingredients: List<DraftUse>, val parameters: StepParameters,
    val durationSeconds: Int?, val startCondition: String?, val evidence: List<EvidenceRef> = emptyList(),
)

private val requestJson = Json { encodeDefaults = true }

internal suspend fun requestRecipeReply(
    message: String, availableTags: List<String>, previous: AiRecipeDraft?, query: String, excluded: List<String>, history: List<String>,
): DraftReply =
    withContext(Dispatchers.IO) {
        val connection = URI("http://127.0.0.1:8765/recipe-chat").toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 3000
            connection.readTimeout = 150_000
            connection.instanceFollowRedirects = false
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val body = requestJson.encodeToString(DraftRequest(message, availableTags.take(100), previous, query, excluded, history))
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode != 200) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                val message = error?.let { runCatching { Json.decodeFromString<DraftError>(it).error }.getOrNull() }
                throw DraftServiceException(message ?: "AI 整理失败，请重试")
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { Json.decodeFromString<DraftReply>(it.readText()) }
        } finally {
            connection.disconnect()
        }
    }

internal class DraftServiceException(message: String) : IOException(message)
