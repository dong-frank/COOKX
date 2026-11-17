package com.cookx.app.domain

/** Optional examples, never inserted into a new household automatically. */
fun exampleRecipes(familyId: String): List<Recipe> {
    val tomato = RecipeIngredient("tomato", "番茄", "2个")
    val egg = RecipeIngredient("egg", "鸡蛋", "3个")
    val oil = RecipeIngredient("oil", "食用油", "适量")
    val salt = RecipeIngredient("salt", "盐", "适量")
    val first = Recipe("example-tomato-eggs", familyId, "番茄炒蛋", listOf(tomato, egg, oil, salt), listOf(
        RecipePhase(PhaseType.PREP, listOf(
            RecipeStep("wash-cut", "处理番茄", "番茄洗净后切块。", operationType = OperationType.CUT,
                ingredients = listOf(StepIngredientUse("tomato", "2个"))),
            RecipeStep("beat-eggs", "打散鸡蛋", "将鸡蛋打入碗中搅散。", operationType = OperationType.MIX,
                ingredients = listOf(StepIngredientUse("egg", "3个"))),
        )),
        RecipePhase(PhaseType.COOK, listOf(
            RecipeStep("fry-eggs", "炒鸡蛋", "热锅加油，倒入蛋液，凝固后盛出。", operationType = OperationType.STIR_FRY,
                ingredients = listOf(StepIngredientUse("egg"), StepIngredientUse("oil", "适量")), parameters = StepParameters(endCondition = "蛋液凝固")),
            RecipeStep("fry-tomatoes", "翻炒番茄", "炒软番茄，加入鸡蛋翻炒。", operationType = OperationType.STIR_FRY,
                ingredients = listOf(StepIngredientUse("tomato"), StepIngredientUse("egg"))),
            RecipeStep("season-plate", "调味装盘", "按口味加盐，翻炒均匀后盛出。", operationType = OperationType.PLATE,
                ingredients = listOf(StepIngredientUse("salt", "适量"))),
        )),
    ), tags = listOf("家常菜", "快手菜"), servings = 2, isExample = true)
    val mushrooms = RecipeIngredient("mushrooms", "口蘑", "250克")
    val garlic = RecipeIngredient("garlic", "蒜", "2瓣")
    val second = Recipe("example-mushrooms", familyId, "清炒口蘑", listOf(mushrooms, garlic, oil, salt), listOf(
        RecipePhase(PhaseType.PREP, listOf(RecipeStep("prep-mushrooms", "清洗切片", "口蘑洗净切片，蒜切碎。", operationType = OperationType.SLICE,
            ingredients = listOf(StepIngredientUse("mushrooms", "250克"), StepIngredientUse("garlic", "2瓣"))))),
        RecipePhase(PhaseType.COOK, listOf(
            RecipeStep("saute-garlic", "炒香蒜末", "热锅加油，放入蒜末炒香。", operationType = OperationType.SAUTE,
                ingredients = listOf(StepIngredientUse("garlic"), StepIngredientUse("oil", "适量"))),
            RecipeStep("fry-mushrooms", "翻炒口蘑", "加入口蘑炒熟，按口味加盐后盛出。", operationType = OperationType.STIR_FRY,
                ingredients = listOf(StepIngredientUse("mushrooms"), StepIngredientUse("salt", "适量"))),
        )),
    ), tags = listOf("素菜", "快手菜"), isExample = true)
    return listOf(first, second).onEach(::validateRecipe)
}
