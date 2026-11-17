package com.cookx.app.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cookx.app.R

/** Once per cooking session; returning, rotation and foregrounding never replay it. */
@Composable internal fun CookingResultSound(alreadyPlayed: Boolean, markPlayed: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val played by rememberUpdatedState(alreadyPlayed)
    val mark by rememberUpdatedState(markPlayed)
    DisposableEffect(owner, context) {
        var player: MediaPlayer? = null
        var disposed = false
        var attempted = alreadyPlayed
        fun release() { player?.release(); player = null }
        fun play() {
            if (attempted || played || disposed) return
            attempted = true
            mark()
            runCatching {
                val next = MediaPlayer()
                player = next
                next.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                context.resources.openRawResourceFd(R.raw.cooking_result).use { file ->
                    next.setDataSource(file.fileDescriptor, file.startOffset, file.length)
                }
                next.setOnPreparedListener {
                    if (!disposed && player === it && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) runCatching { it.start() }.onFailure { release() }
                    else release()
                }
                next.setOnCompletionListener { release() }
                next.setOnErrorListener { _, _, _ -> release(); true }
                next.prepareAsync()
            }.onFailure { release() }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> play()
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> release()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) play()
        onDispose { disposed = true; owner.lifecycle.removeObserver(observer); release() }
    }
}
