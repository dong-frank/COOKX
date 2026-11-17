package com.cookx.app.data

import android.content.Context
import com.cookx.app.domain.KitchenState
import com.cookx.app.domain.migrateKitchen
import kotlinx.serialization.json.Json

/** Local starter storage. Replace behind this boundary when persistence/sync requirements are agreed. */
class KitchenRepository(context: Context, storageName: String = "cookx_kitchen_v1") {
    private val preferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    fun read(): KitchenState {
        val raw = preferences.getString("state", null) ?: return KitchenState(schemaVersion = 2)
        val old = json.decodeFromString<KitchenState>(raw)
        val migrated = migrateKitchen(old)
        if (migrated != old) {
            val editor = preferences.edit()
            if (!preferences.contains("state_backup_before_tags_meals")) editor.putString("state_backup_before_tags_meals", raw)
            check(editor.putString("state", json.encodeToString(migrated)).commit()) { "旧数据升级保存失败，原数据已保留" }
        }
        return migrated
    }
    fun write(state: KitchenState) {
        check(preferences.edit().putString("state", json.encodeToString(state)).commit()) {
            "保存失败，请重试"
        }
    }
}
