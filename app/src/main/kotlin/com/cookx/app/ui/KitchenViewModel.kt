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
        state.copy(recipes = state.recipes + recipe)
    }
    fun finish(recipeId: String, note: String, onSuccess: () -> Unit) = update(onSuccess) { state ->
        val family = requireNotNull(state.family)
        require(state.recipes.any { it.id == recipeId && it.familyId == family.id })
        state.copy(records = state.records + CookingRecord(UUID.randomUUID().toString(), family.id,
            recipeId, System.currentTimeMillis(), note.trim()))
    }
}
