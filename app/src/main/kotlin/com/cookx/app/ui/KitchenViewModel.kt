package com.cookx.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cookx.app.data.KitchenRepository
import com.cookx.app.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class KitchenViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = KitchenRepository(application)
    private val mutableState = MutableStateFlow<KitchenState?>(null)
    val state = mutableState.asStateFlow()
    private val mutableError = MutableStateFlow<String?>(null)
    val error = mutableError.asStateFlow()
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    init { reload() }
    fun reload() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.read() }.onSuccess { mutableState.value = it; mutableError.value = null }
                .onFailure { mutableError.value = "无法读取本地记录，请重试；原数据已保留。" }
        }
    }
    private fun update(onSuccess: () -> Unit = {}, transform: (KitchenState) -> KitchenState) {
        if (mutableBusy.value) return
        mutableBusy.value = true
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val next = transform(requireNotNull(mutableState.value))
                repository.write(next)
                withContext(Dispatchers.Main) {
                    mutableState.value = next
                    mutableError.value = null
                    mutableBusy.value = false
                    onSuccess()
                }
            }.onFailure { mutableError.value = it.message ?: "保存失败，请重试" }
            mutableBusy.value = false
        }
    }
    fun createFamily(name: String) = update { state ->
        require(name.isNotBlank()) { "请填写家庭名称" }
        check(state.family == null)
        state.copy(family = Family(UUID.randomUUID().toString(), name.trim()))
    }
    fun addExamples() = update { state ->
        val family = requireNotNull(state.family)
        val added = exampleRecipes(family.id).filter { recipe -> state.recipes.none { it.id == recipe.id } }
        state.copy(recipes = state.recipes + added)
    }
    fun addRecipe(recipe: Recipe, onSuccess: () -> Unit) = update(onSuccess) { state ->
        require(recipe.familyId == state.family?.id)
        validateRecipe(recipe)
        state.copy(recipes = state.recipes + recipe,
            customTags = (state.customTags + recipe.tags.filter { it !in state.presetTags }).distinct())
    }
    fun finish(recipeId: String, note: String, selection: MealSelection, onSuccess: () -> Unit) = update(onSuccess) { state ->
        val family = requireNotNull(state.family)
        require(state.recipes.any { it.id == recipeId && it.familyId == family.id })
        dateToUtcMillis(selection.date)
        state.copy(records = state.records + CookingRecord(UUID.randomUUID().toString(), family.id,
            recipeId, System.currentTimeMillis(), note.trim(), selection.date, selection.meal))
    }
    fun createTag(name: String) = update { state ->
        val tag = normalizeTags(listOf(name)).singleOrNull() ?: error("请填写标签名")
        state.copy(customTags = (state.customTags + tag.takeIf { it !in state.presetTags }.let { listOfNotNull(it) }).distinct())
    }
    fun setFavorites(tags: List<String>) = update { state ->
        val normalized = normalizeTags(tags)
        require(normalized.all { it in state.availableTags() }) { "标签不存在" }
        state.copy(favoriteTags = normalized)
    }
    fun setRecipeTags(id: String, tags: List<String>, onSuccess: () -> Unit) = update(onSuccess) { state ->
        val normalized = normalizeTags(tags)
        require(state.recipes.any { it.id == id && it.familyId == state.family?.id })
        state.copy(recipes = state.recipes.map { if (it.id == id) it.copy(tags = normalized) else it },
            customTags = (state.customTags + normalized.filter { it !in state.presetTags }).distinct())
    }
    fun openShoppingMemo(recipeId: String, onSuccess: () -> Unit) = update(onSuccess) { state ->
        val recipe = state.recipes.single { it.id == recipeId && it.familyId == state.family?.id }
        val previous = state.shoppingMemos.find { it.recipeId == recipeId && it.familyId == recipe.familyId }
        val memo = prepareShoppingMemo(recipe, state.pantryItems, previous)
        state.copy(shoppingMemos = state.shoppingMemos.filterNot { it.recipeId == recipeId && it.familyId == recipe.familyId } + memo)
    }
    fun checkShoppingItem(recipeId: String, key: String, checked: Boolean) = update { state ->
        val family = requireNotNull(state.family)
        require(state.shoppingMemos.any { it.recipeId == recipeId && it.familyId == family.id })
        state.copy(shoppingMemos = state.shoppingMemos.map {
            if (it.recipeId == recipeId && it.familyId == family.id) it.markPrepared(key, checked) else it
        })
    }
    fun recheckShoppingPantry(recipeId: String) = update { state ->
        val recipe = state.recipes.single { it.id == recipeId && it.familyId == state.family?.id }
        state.copy(shoppingMemos = state.shoppingMemos.map {
            if (it.recipeId == recipeId && it.familyId == recipe.familyId) prepareShoppingMemo(recipe, state.pantryItems, it, true) else it
        })
    }
    fun addPantryItem(name: String) = update { state ->
        val family = requireNotNull(state.family)
        val normalized = name.trim()
        require(normalized.isNotEmpty() && normalized.length <= 64) { "请填写 1–64 个字的食材名称" }
        val existing = state.pantryItems.find { it.familyId == family.id && it.name == normalized }
        state.copy(pantryItems = if (existing == null) state.pantryItems + PantryItem(UUID.randomUUID().toString(), family.id, normalized)
            else state.pantryItems.map { if (it.id == existing.id) it.copy(available = true) else it })
    }
    fun setPantryAvailable(id: String, available: Boolean) = update { state ->
        require(state.pantryItems.any { it.id == id && it.familyId == state.family?.id })
        state.copy(pantryItems = state.pantryItems.map { if (it.id == id && it.familyId == state.family?.id) it.copy(available = available) else it })
    }
}
