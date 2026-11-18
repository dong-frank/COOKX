package com.cookx.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cookx.app.data.DraftServiceException
import com.cookx.app.data.requestRecipeDraft
import com.cookx.app.domain.Recipe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable internal fun AiRecipeDraftDialog(
    familyId: String, availableTags: List<String>, replacesDraft: Boolean,
    dismiss: () -> Unit, applyDraft: (Recipe) -> Unit,
) {
    var source by rememberSaveable { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("AI 整理") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(source, { source = it.take(6000); error = null },
                    label = { Text("粘贴菜谱或输入做法") }, enabled = !working,
                    minLines = 4, maxLines = 8, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(enabled = source.isNotBlank() && !working, onClick = {
                working = true
                error = null
                scope.launch {
                    try {
                        applyDraft(requestRecipeDraft(source.trim(), availableTags).toRecipe(familyId, availableTags))
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: DraftServiceException) {
                        error = failure.message
                    } catch (_: java.io.IOException) {
                        error = "无法连接 AI 服务，请重试"
                    } catch (_: IllegalArgumentException) {
                        error = "草稿格式有误，请调整原文后重试"
                    } finally {
                        working = false
                    }
                }
            }) { Text(if (working) "整理中…" else if (replacesDraft) "生成并替换草稿" else "生成草稿") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("取消") } },
    )
}
