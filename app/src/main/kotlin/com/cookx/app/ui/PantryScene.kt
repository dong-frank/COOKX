package com.cookx.app.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** One process-wide 480x640 ARGB cache: 20 frames, at most 23.44 MiB of pixels. */
private object SceneFrames {
    private val lock = Mutex()
    private var cached: List<ImageBitmap>? = null
    suspend fun load(context: Context): List<ImageBitmap> = withContext(Dispatchers.IO) {
        lock.withLock {
            cached ?: (0..19).map { index ->
                context.assets.open(String.format(Locale.ROOT, "pantry-scene/%02d.png", index)).use {
                    requireNotNull(BitmapFactory.decodeStream(it)).asImageBitmap()
                }
            }.also { cached = it }
        }
    }
}

@Composable internal fun PantryScene(frame: Int, enabled: Boolean, spicesSelected: Boolean, fridge: () -> Unit, spices: () -> Unit) {
    val context = LocalContext.current.applicationContext
    var attempt by remember { mutableIntStateOf(0) }
    var frames by remember { mutableStateOf<List<ImageBitmap>?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(attempt) {
        failed = false
        try { frames = SceneFrames.load(context) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { failed = true }
    }
    if (failed) {
        Column {
            androidx.compose.material3.TextButton(onClick = fridge) { androidx.compose.material3.Text("冰箱") }
            androidx.compose.material3.TextButton(onClick = spices) { androidx.compose.material3.Text("调料") }
            androidx.compose.material3.TextButton(onClick = { attempt++ }) { androidx.compose.material3.Text("重试") }
        }
    } else BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = minOf(maxWidth, maxHeight * .75f)
        val height = width / .75f
        val left = (maxWidth - width) / 2
        val top = (maxHeight - height) / 2
        frames?.let { Image(androidx.compose.ui.graphics.painter.BitmapPainter(it[frame.coerceIn(0, 19)]), null,
            Modifier.offset(left, top).size(width, height), contentScale = ContentScale.Fit) }
        if (frames == null) androidx.compose.material3.CircularProgressIndicator(Modifier.offset(maxWidth / 2 - 20.dp, maxHeight / 2 - 20.dp))
        Box(Modifier.offset(left + width * .065f, top + height * .175f).size(width * .315f, height * .660f)
            .clickable(enabled && frames != null, onClickLabel = "开关冰箱", role = Role.Button, onClick = fridge).testTag("pantryFridge").semantics { contentDescription = "冰箱"; stateDescription = if (frame == 19) "已打开" else "已关闭" })
        Box(Modifier.offset(left + width * .415f, top + height * .395f).size(width * .240f, height * .130f)
            .then(if (spicesSelected) Modifier.border(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = .25f), RoundedCornerShape(12.dp)) else Modifier)
            .clickable(enabled && frames != null, onClickLabel = "查看调料", role = Role.Button, onClick = spices).testTag("pantrySpices")
            .semantics { contentDescription = "调料瓶"; stateDescription = if (spicesSelected) "已展开" else "已收起" })
    }
}
