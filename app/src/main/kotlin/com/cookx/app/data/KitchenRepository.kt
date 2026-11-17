package com.cookx.app.data

import android.content.Context
import com.cookx.app.domain.KitchenState
import kotlinx.serialization.json.Json

/** Local starter storage. Replace behind this boundary when persistence/sync requirements are agreed. */
class KitchenRepository(context: Context) {
    private val preferences = context.getSharedPreferences("cookx_kitchen_v1", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    fun read(): KitchenState = preferences.getString("state", null)?.let {
        json.decodeFromString<KitchenState>(it)
    } ?: KitchenState()
    fun write(state: KitchenState) {
        check(preferences.edit().putString("state", json.encodeToString(state)).commit()) {
            "保存失败，请重试"
        }
    }
}
