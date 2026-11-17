package com.cookx.app.domain

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class OperationTemplateTest {
    private val templates = parseTemplateCatalog(File("src/main/assets/presets/step-templates.json").readText()).templates
    private val ingredients = listOf("肉", "盐", "水", "油", "淀粉", "葱", "姜").mapIndexed { i, name -> RecipeIngredient("i$i", name) }
    private fun use(index: Int, amount: String? = null) = TemplateValue(ingredients = listOf(StepIngredientUse("i$index", amount)))
    @Test fun templateApplicationStoresVersionBindingsAndCompleteSnapshot() {
        val template = templates.single { it.id == "meat-blanch-cold" }
        val values = mapOf("meat" to use(0), "water" to use(2, "没过食材"), "durationMinutes" to TemplateValue(text = "2"), "timerStart" to TemplateValue(text = "水沸后"))
        val step = applyOperationTemplate(template, values, ingredients, "step")
        assertEquals(StepKind.TEMPLATE, step.kind)
        assertEquals(template.version, step.templateRef!!.version)
        assertEquals("没过食材", step.ingredients.single { it.ingredientId == "i2" }.quantityText)
        assertEquals(StepTimer(120, "水沸后"), step.timer)
        assertFalse(step.description.contains("温水清理"))
        assertEquals(step, Json.decodeFromString<RecipeStep>(Json.encodeToString(step)))
        assertEquals(values, step.templateRef!!.values)
    }
    @Test fun applyingTemplateRequiresIngredientsAmountsAndTimerStart() {
        val template = templates.single { it.id == "meat-blanch-cold" }
        assertThrows(IllegalArgumentException::class.java) { applyOperationTemplate(template, mapOf("meat" to use(0)), ingredients) }
        assertThrows(IllegalArgumentException::class.java) { applyOperationTemplate(template, mapOf("meat" to use(0), "water" to use(2), "durationMinutes" to TemplateValue(text = "2")), ingredients) }
        val marinade = templates.single { it.id == "marinate-meat-strips" }
        assertThrows(IllegalArgumentException::class.java) { applyOperationTemplate(marinade, mapOf("meat" to use(0), "seasonings" to use(1, "适量"), "starch" to use(4, "少许")), ingredients) }
    }
    @Test fun allSixCatalogTemplatesResolveAndOptionalActionsStayHidden() {
        assertEquals(6, templates.size)
        templates.forEach { t ->
            val values = t.parameters.associate { p -> p.id to when(p.type) {
                "ingredient-use", "ingredient-use-list" -> use(t.parameters.filter { it.type.startsWith("ingredient-use") }.indexOf(p), "适量")
                else -> TemplateValue(text = p.defaultText.orEmpty())
            } }
            val step = applyOperationTemplate(t, values, ingredients)
            assertNull(step.timer)
            assertFalse(step.description.contains("{durationMinutes}"))
            assertTrue(step.templateRef!!.bindings.isNotEmpty())
        }
        val broccoli = templates.single { it.id == "broccoli-blanch" }
        val base = mapOf("broccoli" to use(0), "water" to use(2))
        val plain = applyOperationTemplate(broccoli, base, ingredients)
        assertFalse(plain.description.contains("过凉")); assertFalse(plain.description.contains("加盐"))
        val salted = applyOperationTemplate(broccoli, base + mapOf("addSalt" to TemplateValue(enabled = true), "salt" to use(1, "少许"), "cool" to TemplateValue(enabled = true)), ingredients)
        assertTrue(salted.description.contains("过凉")); assertTrue(salted.description.contains("盐 少许"))
        assertEquals(3, salted.ingredients.size)
    }
    @Test fun everyOptionalActionCombinationResolvesWithoutInventedTimers() {
        templates.forEach { template ->
            val flags = template.parameters.filter { it.type == "boolean" }
            repeat(1 shl flags.size) { mask ->
                val values = template.parameters.associate { p -> p.id to when (p.type) {
                    "boolean" -> TemplateValue(enabled = mask and (1 shl flags.indexOf(p)) != 0)
                    "ingredient-use", "ingredient-use-list" -> TemplateValue(ingredients = listOf(StepIngredientUse("i${template.parameters.filter { it.type.startsWith("ingredient-use") }.indexOf(p)}", "适量")))
                    else -> TemplateValue(text = p.defaultText.orEmpty())
                } }
                val step = applyOperationTemplate(template, values, ingredients)
                assertNull(step.timer)
                template.actions.filter { it.enabledWhen?.matches(values) == false }.forEach { action ->
                    assertFalse(step.description.contains(action.instruction))
                }
                template.parameters.filter { !it.isEnabled(values) }.forEach { parameter ->
                    assertFalse(step.templateRef!!.bindings.keys.any { it.startsWith("${parameter.id}:") })
                }
            }
        }
    }
    @Test fun aromaticsOrderReferencesSelectedIngredientsExactlyOnce() {
        val t = templates.single { it.id == "aromatics-saute" }
        val base = mapOf("aromatics" to TemplateValue(ingredients = listOf(StepIngredientUse("i5"), StepIngredientUse("i6"))), "oil" to use(3))
        val ordered = base + ("additionOrder" to TemplateValue(ordered = true, order = listOf("i6", "i5")))
        assertTrue(applyOperationTemplate(t, ordered, ingredients).description.contains("姜 → 葱"))
        assertThrows(IllegalArgumentException::class.java) { applyOperationTemplate(t, base + ("additionOrder" to TemplateValue(ordered = true, order = listOf("i5", "i5"))), ingredients) }
    }
    @Test fun tagCatalogContainsAllNamesAndStableIds() {
        val catalog = parseTagCatalog(File("src/main/assets/presets/tags.json").readText())
        assertEquals(33, catalog.tags.size)
        assertEquals("quick-meal", catalog.tags.single { it.name == "快手菜" }.id)
        assertTrue(catalog.tags.any { it.name == "湘菜" })
        assertFalse(catalog.tags.any { it.name == "待尝试" })
    }
    @Test fun operationIconDoesNotImplyTemplateProvenance() {
        val step = RecipeStep("s", "切块", "按自己的做法切块。", operationType = OperationType.CUT)
        validateStep(step, ingredients)
        assertNull(step.templateRef)
        assertThrows(IllegalArgumentException::class.java) { validateStep(step.copy(kind = StepKind.TEMPLATE), ingredients) }
    }
}
