package com.pleos.radio.audio

import android.content.Context
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import com.pleos.radio.data.StreamResolver
import com.pleos.radio.domain.Station
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Plays a station's internet simulcast with Media3 ExoPlayer (HLS or progressive MP3).
 * ExoPlayer manages media audio focus itself (pause on loss, suspend on transient loss, duck).
 * Must be used from the main thread.
 */
class StreamingRadioAudio(
    context: Context,
    private val resolver: StreamResolver,
    private val scope: CoroutineScope,
) {
    var onFocusLost: (() -> Unit)? = null

    private val _status = MutableStateFlow<AudioStatus>(AudioStatus.Idle)
    val status: StateFlow<AudioStatus> = _status.asStateFlow()

    private val player = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private var station: Station? = null
    private var loadJob: Job? = null
    private var retried = false

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = refreshStatus()
            override fun onPlaybackStateChanged(playbackState: Int) = refreshStatus()

            override fun onPlayerError(error: PlaybackException) {
                Log.w(TAG, "playback error for ${station?.name}: ${error.errorCodeName}", error)
                val s = station ?: return
                if (httpStatus(error) == 403) {
                    // Geo-restricted (e.g. BBC domestic stations outside the UK): retrying cannot help.
                    _status.value = AudioStatus.Error("${s.name} is not available in your region")
                    return
                }
                // Signed URLs expire and live edges drift: fetch a fresh URL once before giving up.
                if (!retried) {
                    retried = true
                    load(s)
                } else {
                    _status.value = AudioStatus.Error("${s.name} stream unavailable")
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS) {
                    _status.value = AudioStatus.Idle
                    onFocusLost?.invoke()
                }
            }
        })
    }

    fun play(target: Station) {
        val sameAndHealthy = target.id == station?.id && player.playWhenReady && _status.value !is AudioStatus.Error
        if (sameAndHealthy) return
        station = target
        retried = false
        load(target)
    }

    private fun load(target: Station) {
        val source = target.stream ?: return
        loadJob?.cancel()
        player.stop()
        _status.value = AudioStatus.Connecting
        loadJob = scope.launch {
            val url = runCatching { resolver.resolve(source) }
                .onFailure { Log.w(TAG, "resolve failed for ${target.name}", it) }
                .getOrNull()
            if (station?.id != target.id) return@launch
            if (url == null) {
                _status.value = AudioStatus.Error("Cannot reach ${target.name} – check network")
                return@launch
            }
            // HLS playlists are declared explicitly (signed URLs hide the extension in some cases);
            // anything else (e.g. Icecast MP3) is left to ExoPlayer's progressive extractor sniffing.
            val item = MediaItem.Builder().setUri(url)
                .apply { if (".m3u8" in url) setMimeType(MimeTypes.APPLICATION_M3U8) }
                .build()
            player.setMediaItem(item)
            player.prepare()
            player.playWhenReady = true
        }
    }

    private fun httpStatus(error: Throwable): Int? =
        generateSequence(error) { it.cause }
            .filterIsInstance<HttpDataSource.InvalidResponseCodeException>()
            .firstOrNull()?.responseCode

    private fun refreshStatus() {
        if (station == null) return
        when {
            player.isPlaying -> _status.value = AudioStatus.Live
            player.playWhenReady && player.playbackState == Player.STATE_BUFFERING -> _status.value = AudioStatus.Connecting
        }
    }

    fun stop() {
        loadJob?.cancel()
        station = null
        player.playWhenReady = false
        player.stop()
        player.clearMediaItems()
        _status.value = AudioStatus.Idle
    }

    fun release() {
        stop()
        player.release()
    }

    private companion object {
        const val TAG = "StreamingRadioAudio"
    }
}
