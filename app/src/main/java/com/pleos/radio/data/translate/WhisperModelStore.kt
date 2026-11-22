package com.pleos.radio.data.translate

import android.content.Context
import com.pleos.radio.domain.caption.WhisperModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Downloads whisper models into app-private storage on demand, verifying size and SHA-256. */
class WhisperModelStore(context: Context, private val scope: CoroutineScope) {

    sealed interface State {
        data object Missing : State
        data class Downloading(val progress: Float) : State
        data object Ready : State
        data class Failed(val message: String) : State
    }

    private val dir = File(context.filesDir, "models").apply { mkdirs() }
    private val _states = MutableStateFlow(WhisperModel.entries.associateWith { initialState(it) })
    val states: StateFlow<Map<WhisperModel, State>> = _states.asStateFlow()
    private val jobs = mutableMapOf<WhisperModel, Job>()

    fun file(model: WhisperModel) = File(dir, model.fileName)

    fun isReady(model: WhisperModel) = _states.value[model] == State.Ready

    private fun initialState(model: WhisperModel): State =
        if (file(model).length() == model.sizeBytes) State.Ready else State.Missing

    private fun set(model: WhisperModel, state: State) {
        _states.value = _states.value + (model to state)
    }

    @Synchronized
    fun download(model: WhisperModel) {
        if (isReady(model) || jobs[model]?.isActive == true) return
        jobs[model] = scope.launch(Dispatchers.IO) {
            val part = File(dir, model.fileName + ".part")
            try {
                set(model, State.Downloading(0f))
                val conn = URL(model.url).openConnection() as HttpURLConnection
                conn.connectTimeout = 15_000
                conn.readTimeout = 30_000
                conn.instanceFollowRedirects = true
                if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
                val digest = MessageDigest.getInstance("SHA-256")
                var done = 0L
                var lastReported = 0f
                conn.inputStream.use { input ->
                    part.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            ensureActive()
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            digest.update(buf, 0, n)
                            done += n
                            val p = done.toFloat() / model.sizeBytes
                            if (p - lastReported >= 0.01f) { lastReported = p; set(model, State.Downloading(p.coerceAtMost(1f))) }
                        }
                    }
                }
                conn.disconnect()
                val hash = digest.digest().joinToString("") { "%02x".format(it) }
                if (done != model.sizeBytes || hash != model.sha256) throw IOException("Model verification failed")
                if (!part.renameTo(file(model))) throw IOException("Could not save model")
                set(model, State.Ready)
            } catch (e: CancellationException) {
                part.delete(); set(model, State.Missing); throw e
            } catch (e: Exception) {
                part.delete(); set(model, State.Failed(e.message ?: "Download failed"))
            }
        }
    }

    @Synchronized
    fun delete(model: WhisperModel) {
        jobs.remove(model)?.cancel()
        file(model).delete()
        set(model, State.Missing)
    }
}
