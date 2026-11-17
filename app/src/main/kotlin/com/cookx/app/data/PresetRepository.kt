package com.cookx.app.data

import android.content.Context
import com.cookx.app.domain.*

class PresetRepository(private val context: Context) {
    val tags by lazy { parseTagCatalog(read("tags.json")).tags }
    val templates by lazy { parseTemplateCatalog(read("step-templates.json")).templates }
    private fun read(name: String) = context.assets.open("presets/$name").bufferedReader().use { it.readText() }
}
