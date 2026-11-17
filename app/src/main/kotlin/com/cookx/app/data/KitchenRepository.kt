package com.cookx.app.data

import android.content.Context
import com.cookx.app.domain.*
import kotlinx.serialization.json.Json

class KitchenRepository(context: Context, storageName: String = "cookx_kitchen_v3") {
    private val presets = PresetRepository(context)
    private val preferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    fun read(): KitchenState {
        val raw = preferences.getString("state", null)
        return (raw?.let { json.decodeFromString<KitchenState>(it) } ?: KitchenState())
            .copy(presetTags = presets.tags.map { it.name }).also(::validateKitchen)
    }
    fun write(state: KitchenState) {
        validateKitchen(state)
        check(preferences.edit().putString("state", json.encodeToString(state)).commit()) { "保存失败，请重试" }
    }
}
