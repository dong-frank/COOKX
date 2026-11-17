package com.cookx.app

import android.media.MediaPlayer
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class CookingResultAudioTest {
    @Test fun packagedSequenceDecodesWithThePreparedHandoffDuration() {
        val player = MediaPlayer.create(InstrumentationRegistry.getInstrumentation().targetContext, R.raw.cooking_result)
        assertNotNull(player)
        try { assertTrue(player.duration in 7500..7900) } finally { player.release() }
    }
}
