package com.cookx.app.domain

import kotlinx.serialization.Serializable

@Serializable enum class PantryZone(val label: String) { FRIDGE("冰箱"), SPICES("调料") }

fun PantryItem.inStock(): Boolean = available && !quantityText.orEmpty().trim()
    .matches(Regex("0+(?:\\.0+)?(?:\\s*[\\p{L}]+)?"))
fun PantryItem.amountLabel() = quantityText?.takeIf { it.isNotBlank() } ?: "未填数量"
fun KitchenState.pantryIn(zone: PantryZone) = pantryItems.filter { it.familyId == family?.id && it.zone == zone && it.inStock() }
fun normalizedPantryItem(item: PantryItem): PantryItem {
    val name = item.name.trim()
    val quantity = item.quantityText?.trim()?.ifBlank { null }
    require(item.id.isNotBlank() && item.familyId.isNotBlank()) { "物品标识不能为空" }
    require(name.isNotBlank() && name.length <= 64) { "请填写 1–64 个字的名称" }
    require(quantity == null || quantity.length <= 64) { "数量最多 64 个字" }
    return item.copy(name = name, quantityText = quantity)
}
fun KitchenState.savePantry(item: PantryItem): KitchenState {
    require(item.familyId == family?.id) { "物品不属于当前家庭" }
    val normalized = normalizedPantryItem(item)
    val existing = pantryItems.find { it.id == item.id }
    require(existing == null || existing.familyId == item.familyId) { "物品归属不匹配" }
    return copy(pantryItems = if (existing == null) pantryItems + normalized else pantryItems.map { if (it.id == item.id) normalized else it })
}
