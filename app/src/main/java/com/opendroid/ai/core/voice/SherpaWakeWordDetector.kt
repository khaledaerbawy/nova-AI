package com.opendroid.ai.core.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.k2fsa.sherpa.onnx.KeywordSpotter
import com.k2fsa.sherpa.onnx.OnlineStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Local streaming keyword spotter. It keeps audio on-device and only signals
 * the caller when a configured wake phrase is detected.
 */
internal class SherpaWakeWordDetector(
    context: Context,
    private val installer: WakeWordModelInstaller = WakeWordModelInstaller(context),
    private val onStatus: (String) -> Unit = {}
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val running = AtomicBoolean(false)

    @Volatile private var audioRecord: AudioRecord? = null
    @Volatile private var listenJob: Job? = null

    fun startListening(onWakeWordDetected: () -> Unit, onUnavailable: () -> Unit) {
        if (!running.compareAndSet(false, true)) return
        onStatus("جاري تجهيز محرك الاستيقاظ المحلي")
        listenJob = scope.launch {
            var spotter: KeywordSpotter? = null
            var stream: OnlineStream? = null
            var recorder: AudioRecord? = null
            try {
                val modelDirectory = installer.ensureInstalled()
                if (!running.get() || !isActive) return@launch

                spotter = installer.createSpotter(modelDirectory)
                stream = spotter.createStream()
                val minBytes = AudioRecord.getMinBufferSize(
                    WakeWordModelInstaller.SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                check(minBytes > 0) { "AudioRecord is unavailable on this device" }
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    WakeWordModelInstaller.SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(minBytes * 2, WakeWordModelInstaller.SAMPLE_RATE / 2)
                )
                check(recorder.state == AudioRecord.STATE_INITIALIZED) {
                    "Unable to initialize microphone capture"
                }
                audioRecord = recorder
                recorder.startRecording()
                onStatus("NOVA جاهز — الاستماع محلياً لكلمة التنبيه")

                val pcm = ShortArray(WakeWordModelInstaller.SAMPLE_RATE / 10)
                while (running.get() && isActive) {
                    val count = recorder.read(pcm, 0, pcm.size, AudioRecord.READ_BLOCKING)
                    if (count <= 0) {
                        if (count == AudioRecord.ERROR_DEAD_OBJECT ||
                            count == AudioRecord.ERROR_INVALID_OPERATION
                        ) error("Microphone capture stopped unexpectedly")
                        continue
                    }

                    val samples = FloatArray(count) { index -> pcm[index] / 32768.0f }
                    stream.acceptWaveform(samples, WakeWordModelInstaller.SAMPLE_RATE)
                    while (spotter.isReady(stream)) spotter.decode(stream)

                    val result = spotter.getResult(stream)
                    if (WakeWordMatcher.isMatch(result.keyword)) {
                        running.set(false)
                        mainHandler.post(onWakeWordDetected)
                        break
                    }
                    if (result.keyword.isNotBlank()) spotter.reset(stream)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Log.w(TAG, "Local wake-word engine unavailable; using compatibility recognizer", failure)
                if (running.get()) {
                    onStatus("تعذر تشغيل محرك الاستيقاظ المحلي — وضع التوافق")
                    mainHandler.post(onUnavailable)
                }
            } finally {
                running.set(false)
                audioRecord = null
                runCatching { recorder?.stop() }
                runCatching { recorder?.release() }
                runCatching { stream?.release() }
                runCatching { spotter?.release() }
            }
        }
    }

    fun stopListening() {
        running.set(false)
        runCatching { audioRecord?.stop() }
        listenJob?.cancel()
        listenJob = null
    }

    fun destroy() {
        stopListening()
        scope.cancel()
    }

    private companion object {
        const val TAG = "SherpaWakeWordDetector"
    }
}

internal object WakeWordMatcher {
    fun isMatch(keyword: String): Boolean {
        val normalized = keyword
            .replace('_', ' ')
            .trim()
            .uppercase()
        return normalized in setOf("NOFA", "NOVA", "YA NOFA")
    }
}
