package com.cookx.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cookx.app.data.AiRecipeDraft
import com.cookx.app.data.DraftServiceException
import com.cookx.app.data.requestRecipeReply
import com.cookx.app.domain.Recipe
import com.cookx.app.domain.RecipeSource
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun AiRecipeChat(
    familyId: String, availableTags: List<String>, replacesDraft: Boolean,
    dismiss: () -> Unit, applyDraft: (Recipe) -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    var messages by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    var encodedDraft by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var excluded by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val draft = remember(encodedDraft) { encodedDraft?.let { Json.decodeFromString<AiRecipeDraft>(it) } }
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    fun send(text: String, retry: Boolean = false) {
        if (working || text.isBlank()) return
        val history = (if (retry) messages.dropLast(1) else messages).takeLast(4).map {
            val role = if (it.startsWith("U")) "用户" else "助手"
            "$role：${it.drop(1).take(500)}"
        }
        keyboard?.hide()
        if (!retry) messages = ArrayList((messages + ("U" + text)).takeLast(10))
        pending = text
        input = ""
        error = null
        working = true
        scope.launch {
            try {
                val reply = requestRecipeReply(text, availableTags, draft, query, excluded, history)
                reply.draft?.let {
                    it.toRecipe(familyId, availableTags)
                    encodedDraft = Json.encodeToString(it)
                    it.source?.noteId?.let { id -> excluded = ArrayList((excluded + id).distinct().takeLast(20)) }
                }
                reply.query?.let { query = it }
                reply.reply?.let { messages = ArrayList((messages + ("A" + it)).takeLast(10)) }
                pending = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: DraftServiceException) {
                error = failure.message
            } catch (_: IOException) {
                error = "无法连接 AI 服务，请重试"
            } catch (_: IllegalArgumentException) {
                error = "草稿格式有误，请重试"
            } finally {
                working = false
            }
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.layoutInfo.totalItemsCount }.collect { count ->
            if (count > 0) state.scrollToItem(count - 1)
        }
    }
    Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize()) {
            Scaffold(contentWindowInsets = WindowInsets.safeDrawing, topBar = {
                TopAppBar(title = { Text("AI 对话") }, navigationIcon = { TextButton(onClick = dismiss) { Text("返回") } })
            }) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = state,
                        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (messages.isEmpty()) item { ChatBubble("今天想做什么菜？", false) }
                        items(messages) { message -> ChatBubble(message.drop(1), message.startsWith("U")) }
                        if (draft != null) item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(draft.name ?: "菜谱草稿", style = MaterialTheme.typography.titleLarge)
                                    Text("${draft.ingredients.size} 项食材 · ${draft.prep.size + draft.cook.size} 个步骤")
                                    XiaohongshuSourceLink(draft.source)
                                    Button(enabled = !working, onClick = { applyDraft(draft.toRecipe(familyId, availableTags)) }) {
                                        Text(if (replacesDraft) "替换并编辑草稿" else "查看并编辑")
                                    }
                                    if (draft.source?.platform == "xiaohongshu") TextButton(enabled = !working,
                                        onClick = { send("换一种做法") }) { Text("换一种做法") }
                                }
                            }
                        }
                        if (working) item { ChatBubble("正在整理菜谱…", false) }
                        if (pending != null && !working) item {
                            ChatBubble(error ?: "上次请求未完成", false)
                            TextButton(onClick = { pending?.let { send(it, retry = true) } }) { Text("重试") }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(input, { input = it.take(6000) }, label = { Text("菜名、口味或文字做法") },
                            modifier = Modifier.weight(1f), maxLines = 4, enabled = !working)
                        TextButton(onClick = { send(input.trim()) }, enabled = input.isNotBlank() && !working) { Text("发送") }
                    }
                }
            }
        }
    }
}

@Composable private fun ChatBubble(text: String, fromUser: Boolean) {
    Box(Modifier.fillMaxWidth(), contentAlignment = if (fromUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(Modifier.widthIn(max = 480.dp), shape = RoundedCornerShape(16.dp),
            color = if (fromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow) {
            Text(text, Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
        }
    }
}


private val xhsNoteId = Regex("[0-9a-f]{24}")

@Composable internal fun XiaohongshuSourceLink(source: RecipeSource?) {
    val noteId = source?.takeIf { it.platform == "xiaohongshu" }?.noteId
        ?.takeIf(xhsNoteId::matches) ?: return
    val uriHandler = LocalUriHandler.current
    var error by remember { mutableStateOf(false) }
    TextButton(onClick = {
        error = runCatching { uriHandler.openUri("https://www.xiaohongshu.com/explore/$noteId") }.isFailure
    }) { Text("查看小红书原帖") }
    if (error) Text("无法打开原帖", color = MaterialTheme.colorScheme.error)
}
