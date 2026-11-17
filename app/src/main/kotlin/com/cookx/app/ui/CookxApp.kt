package com.cookx.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.cookx.app.domain.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable data object Home : NavKey
@Serializable data object NewRecipe : NavKey
@Serializable data class Detail(val id: String) : NavKey
@Serializable data class Cook(val id: String, val initialDate: String? = null, val initialMeal: Meal? = null) : NavKey
@Serializable data object TagSettings : NavKey
@Serializable data class EditTags(val id: String) : NavKey


@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CookxApp(model: KitchenViewModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val stack = rememberNavBackStack(Home)
    val back: () -> Unit = { if (stack.size > 1) stack.removeLastOrNull() }
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { TopAppBar(title = { Text("COOKX", fontWeight = FontWeight.Bold) },
            navigationIcon = { if (stack.size > 1) TextButton(onClick = back) { Text("返回") } },
            actions = {
                if (stack.lastOrNull() == Home && state?.family != null) {
                    TextButton(onClick = { stack.add(TagSettings) }) { Text("标签管理") }
                    TextButton(onClick = { stack.add(NewRecipe) }) { Text("新增菜谱") }
                }
            }) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            horizontalAlignment = Alignment.CenterHorizontally) {
            if (error != null) {
                Surface(color = MaterialTheme.colorScheme.errorContainer) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(error!!, modifier = Modifier.weight(1f))
                        if (state == null) TextButton(onClick = model::reload) { Text("重试") }
                    }
                }
            }
            val kitchen = state
            if (kitchen == null) {
                if (error == null) CircularProgressIndicator(Modifier.padding(48.dp))
            } else if (kitchen.family == null) {
                FamilyWelcome(busy, model::createFamily)
            } else {
                Box(Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                    NavDisplay(backStack = stack, onBack = back, entryProvider = { route ->
                        when (route) {
                            Home -> NavEntry(route) {
                                LiveKitchen(model) { current, saving ->
                                    HomeScreen(current, saving, model::addExamples,
                                        { stack.add(Detail(it)) }, current.favoriteTags)
                                }
                            }
                            NewRecipe -> NavEntry(route) {
                                LiveKitchen(model) { current, saving ->
                                    RecipeEditor(requireNotNull(current.family).id, current.availableTags(), saving) { recipe -> model.addRecipe(recipe, back) }
                                }
                            }
                            TagSettings -> NavEntry(route) {
                                LiveKitchen(model) { current, saving -> FamilyTagSettings(current, saving, model::createTag, model::setFavorites) }
                            }
                            is EditTags -> NavEntry(route) {
                                LiveKitchen(model) { current, saving ->
                                    current.recipes.find { it.id == route.id }?.let { recipe ->
                                        RecipeTagsEditor(recipe, current.availableTags(), saving) { model.setRecipeTags(recipe.id, it, back) }
                                    }
                                }
                            }
                            is Detail -> NavEntry(route) {
                                LiveKitchen(model) { current, _ ->
                                    val recipe = current.recipes.find { it.id == route.id }
                                    if (recipe != null) RecipeDetail(recipe, current, editTags = { stack.add(EditTags(recipe.id)) }) {
                                        val selection = mealAt(System.currentTimeMillis())
                                        stack.add(Cook(recipe.id, selection.date, selection.meal))
                                    }
                                    else Text("找不到这道菜", Modifier.padding(24.dp))
                                }
                            }
                            is Cook -> NavEntry(route) {
                                LiveKitchen(model) { current, saving ->
                                    val recipe = current.recipes.find { it.id == route.id }
                                    if (recipe != null) CookingScreen(recipe, route, saving) { note, selection -> model.finish(recipe.id, note, selection, back) }
                                    else Text("找不到这道菜", Modifier.padding(24.dp))
                                }
                            }
                            else -> error("Unknown destination: $route")
                        }
                    })
                }
            }
        }
    }
}

// Entries observe the live repository state instead of capturing a navigation snapshot.
@Composable private fun LiveKitchen(model: KitchenViewModel, content: @Composable (KitchenState, Boolean) -> Unit) {
    val current by model.state.collectAsStateWithLifecycle()
    val saving by model.busy.collectAsStateWithLifecycle()
    current?.let { content(it, saving) }
}

@Composable private fun FamilyWelcome(busy: Boolean, create: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    Column(Modifier.widthIn(max = 540.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Spacer(Modifier.height(32.dp))
        Text("家的味道，\n从这一本开始。", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("一个人也可以是一个家庭。把熟悉的配方、做菜的经验，慢慢写进共同的菜谱。",
            style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(value = name, onValueChange = { name = it.take(40) },
            label = { Text("家庭名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = { create(name) }, enabled = name.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth()) {
            Text(if (busy) "正在创建…" else "创建我的家庭")
        }
        Text("当前版本保存在本机。邀请码加入与多成员同步尚未接入。",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun HomeScreen(state: KitchenState, busy: Boolean, examples: () -> Unit,
    open: (String) -> Unit, favorites: List<String>) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (tab == 0) {
                RecipeBrowser(state.recipes, busy, examples, open, favorites)
            } else {
                LazyColumn(contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    item {
                        Text(state.family!!.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text(listOf("", "家庭小厨房", "囤囤鼠", "厨房助手")[tab],
                            style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    }
                    item {
                        when (tab) {
                            1 -> Section("家庭共用一本菜谱", "当前家庭：${state.family!!.name}\n菜谱与制作记录归属于这个家庭，数据目前仅保存在此设备。\n\n邀请码、账号与多人协作将在后续接入。") {}
                            2 -> Section("冰箱有数，做饭有底", "这里将记录采购花费、食材余量与调料情况。\n\n库存精度和扣减方式仍待讨论，当前尚未开放录入。") {}
                            3 -> Section("从经验，到自己的配方", "未来协助整理帖子、替换食材与调料，回顾一天的饮食。\n\n尚未接入模型、小红书读取或健康分析。") {}
                        }
                    }
                }
            }
        }
        NavigationBar {
            listOf("菜谱", "家庭", "囤囤鼠", "助手").forEachIndexed { index, title ->
                NavigationBarItem(selected = tab == index, onClick = { tab = index },
                    icon = { Text(listOf("食", "家", "藏", "助")[index], fontWeight = FontWeight.Bold) }, label = { Text(title) })
            }
        }
    }
}

@Composable private fun Section(title: String, body: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable private fun RecipeDetail(recipe: Recipe, state: KitchenState, editTags: () -> Unit, start: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Text(recipe.name, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(if (recipe.isExample) "示例菜谱 · 可体验制作流程" else "家庭菜谱", color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(state.recordsFor(recipe.id).firstOrNull()?.let { "上次制作：${it.mealLabel()}" } ?: "还没有做过这道菜")
            Spacer(Modifier.height(12.dp))
            Text(recipe.tags.joinToString(" · ").ifEmpty { "尚未添加标签" })
            TextButton(onClick = editTags) { Text("编辑标签") }
            Spacer(Modifier.height(20.dp))
            Button(onClick = start, modifier = Modifier.fillMaxWidth()) { Text("开始制作") }
        }
        item { Section("准备食材", recipe.ingredients.joinToString("\n")) {} }
        item { Text("做法预览", style = MaterialTheme.typography.titleLarge) }
        items(recipe.steps.size) { index -> Text("${index + 1}. ${recipe.steps[index]}", style = MaterialTheme.typography.bodyLarge) }
        item { Text("制作记录", style = MaterialTheme.typography.titleLarge) }
        val records = state.recordsFor(recipe.id)
        if (records.isEmpty()) item { Text("完成制作后，在这里留下今天的经验。") }
        items(records, key = { it.id }) { record -> Section(record.mealLabel() + if (record.mealInferred) "（历史时间推定）" else "", record.note.ifBlank { "完成了一次制作" }) {} }
    }
}

@Composable private fun RecipeEditor(familyId: String, availableTags: List<String>, busy: Boolean, save: (Recipe) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var ingredients by rememberSaveable { mutableStateOf("") }
    var selectedTags by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var steps by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("记下一道拿手菜", style = MaterialTheme.typography.headlineMedium)
        Text("写入家庭共用的菜谱，之后可以随时开始制作。")
        OutlinedTextField(name, { name = it }, label = { Text("菜名") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        TagSelector(availableTags, selectedTags) { selectedTags = ArrayList(it) }
        OutlinedTextField(ingredients, { ingredients = it }, label = { Text("食材与用量，每行一种") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        OutlinedTextField(steps, { steps = it }, label = { Text("制作步骤，每行一步") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            runCatching { recipeDraft(familyId, name, ingredients, steps, selectedTags) }
                .onSuccess { error = null; save(it) }.onFailure { error = it.message }
        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("保存菜谱") }
    }
}

@Composable internal fun CookingScreen(recipe: Recipe, route: Cook, busy: Boolean, finish: (String, MealSelection) -> Unit) {
    val initial = remember { mealAt(System.currentTimeMillis()) }
    var cookingDate by rememberSaveable { mutableStateOf(route.initialDate ?: initial.date) }
    var mealName by rememberSaveable { mutableStateOf((route.initialMeal ?: initial.meal).name) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    val scroll = rememberScrollState()
    LaunchedEffect(step) { scroll.scrollTo(0) }
    var deadline by rememberSaveable { mutableLongStateOf(0L) }
    var remaining by remember { mutableLongStateOf(0L) }
    var note by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(deadline) {
        while (deadline > 0L) {
            remaining = ((deadline - System.currentTimeMillis() + 999) / 1000).coerceAtLeast(0)
            if (remaining == 0L) break
            delay(250)
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Text(recipe.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        MealControls(cookingDate, Meal.valueOf(mealName), { cookingDate = it }, { mealName = it.name })
        if (step < recipe.steps.size) {
            Text("第 ${step + 1} / ${recipe.steps.size} 步", style = MaterialTheme.typography.headlineMedium)
            LinearProgressIndicator(progress = { (step + 1f) / recipe.steps.size }, modifier = Modifier.fillMaxWidth())
            Text(recipe.steps[step], style = MaterialTheme.typography.headlineLarge)
            Section("厨房计时器", when {
                deadline == 0L -> "手动开启，切换步骤仍会继续"
                remaining == 0L -> "时间到，请检查锅中的食材"
                else -> "%02d:%02d".format(remaining / 60, remaining % 60)
            }) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { deadline = System.currentTimeMillis() + 60_000 }) { Text("计时 1 分钟") }
                    if (deadline != 0L) TextButton(onClick = { deadline = 0L; remaining = 0 }) { Text("取消计时") }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { step-- }, enabled = step > 0, modifier = Modifier.weight(1f)) { Text("上一步") }
                Button(onClick = { step++ }, modifier = Modifier.weight(1f)) { Text(if (step == recipe.steps.lastIndex) "完成步骤" else "下一步") }
            }
            Text("离开做菜页面后不提供后台计时通知。", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("又完成了一道家的味道", style = MaterialTheme.typography.headlineLarge)
            Text("记下这次的口味、难度，或下次想调整的地方。")
            OutlinedTextField(note, { note = it }, label = { Text("本次制作心得（选填）") }, minLines = 4, modifier = Modifier.fillMaxWidth())
            Text("照片上传与独立评分项将在后续接入。", style = MaterialTheme.typography.bodySmall)
            Button(onClick = { finish(note, MealSelection(cookingDate, Meal.valueOf(mealName))) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("保存制作记录") }
            TextButton(onClick = { step-- }) { Text("回看最后一步") }
        }
    }
}
