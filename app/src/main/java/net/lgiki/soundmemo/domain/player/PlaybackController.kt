package net.lgiki.soundmemo.domain.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.lgiki.soundmemo.data.model.Recording

class PlaybackController(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val player = ExoPlayer.Builder(context.applicationContext).build()
    private val mutableState = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = mutableState.asStateFlow()

    init {
        player.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    mutableState.value = mutableState.value.copy(isPlaying = isPlaying)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    mutableState.value = mutableState.value.copy(durationMs = player.duration.coerceAtLeast(0L))
                }

                override fun onPlayerError(error: PlaybackException) {
                    mutableState.value = mutableState.value.copy(error = error.localizedMessage ?: "Playback failed.")
                }
            },
        )
        scope.launch {
            while (true) {
                mutableState.value = mutableState.value.copy(
                    positionMs = player.currentPosition.coerceAtLeast(0L),
                    durationMs = player.duration.coerceAtLeast(0L),
                    isPlaying = player.isPlaying,
                )
                delay(500)
            }
        }
    }

    fun play(recording: Recording) {
        if (!File(recording.filePath).exists()) {
            mutableState.value = mutableState.value.copy(error = "The audio file is missing.")
            return
        }
        mutableState.value = mutableState.value.copy(recording = recording, error = null, durationMs = recording.durationMs)
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(recording.filePath))))
        player.prepare()
        player.play()
    }

    fun toggle() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun skipBy(deltaMs: Long) {
        seekTo(player.currentPosition + deltaMs)
    }

    fun setSpeed(speed: Float) {
        val coerced = speed.coerceIn(0.5f, 2f)
        player.playbackParameters = PlaybackParameters(coerced)
        mutableState.value = mutableState.value.copy(speed = coerced)
    }

    fun stop() {
        player.stop()
        mutableState.value = PlayerUiState(speed = mutableState.value.speed)
    }

    fun release() {
        player.release()
    }
}

