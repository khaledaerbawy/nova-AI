package com.opendroid.ai.core.voice
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.KeywordSpotter
import com.k2fsa.sherpa.onnx.KeywordSpotterConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/** Local streaming KWS. English NOVA detection is primary; Arabic phrase recognition falls back to the platform offline recognizer. */
class SherpaKwsWakeWordDetector(private val context: Context) {
    companion object {
        private const val SAMPLE_RATE = 16_000
        private const val MODEL_DIR = "sherpa-onnx-kws-zipformer-gigaspeech-3.3M-2024-01-01"
        private const val MODEL_ARCHIVE = "https://github.com/k2-fsa/sherpa-onnx/releases/download/kws-models/sherpa-onnx-kws-zipformer-gigaspeech-3.3M-2024-01-01.tar.bz2"
        private const val ENCODER_FLOAT = "encoder-epoch-12-avg-2-chunk-16-left-64.onnx"
        private const val ENCODER_INT8 = "encoder-epoch-12-avg-2-chunk-16-left-64.int8.onnx"
        private const val DECODER = "decoder-epoch-12-avg-2-chunk-16-left-64.onnx"
        private const val JOINER_FLOAT = "joiner-epoch-12-avg-2-chunk-16-left-64.onnx"
        private const val JOINER_INT8 = "joiner-epoch-12-avg-2-chunk-16-left-64.int8.onnx"
        private val REQUIRED_FILES = listOf(DECODER, "tokens.txt", "keywords.txt")
        private val MODEL_CANDIDATES = listOf(ENCODER_FLOAT, ENCODER_INT8, JOINER_FLOAT, JOINER_INT8)
        private val ALL_MODEL_FILES = REQUIRED_FILES + MODEL_CANDIDATES
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(5, TimeUnit.MINUTES).callTimeout(6, TimeUnit.MINUTES).build()
    @Volatile private var worker: Job? = null
    @Volatile private var recorder: AudioRecord? = null
    @Volatile private var spotter: KeywordSpotter? = null
    @Volatile private var activeStream: OnlineStream? = null
    @Volatile private var onDetected: (() -> Unit)? = null

    fun start(onWakeWordDetected: () -> Unit, onUnavailable: (String) -> Unit, wakeWordPhrase: String = "NOVA") {
        if (worker?.isActive == true) return
        onDetected = onWakeWordDetected
        worker = scope.launch {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) throw IllegalStateException("Microphone permission is not granted")
                val normalizedPhrase = wakeWordPhrase.trim().uppercase()
                if (normalizedPhrase.isBlank() || !normalizedPhrase.matches(Regex("[A-Z0-9 ]+"))) {
                    throw IllegalArgumentException("كلمة التنبيه العربية أو غير الإنجليزية ستستخدم محرك التعرف الاحتياطي على الجهاز")
                }
                val modelDir = ensureModelInstalled()
                val modelConfig = OnlineModelConfig(
                    transducer = OnlineTransducerModelConfig(
                        encoder = chooseModelFile(modelDir, ENCODER_INT8, ENCODER_FLOAT),
                        decoder = File(modelDir, DECODER).absolutePath,
                        joiner = chooseModelFile(modelDir, JOINER_INT8, JOINER_FLOAT)
                    ),
                    tokens = File(modelDir, "tokens.txt").absolutePath, numThreads = 1, provider = "cpu", modelType = "zipformer2"
                )
                val config = KeywordSpotterConfig(featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80), modelConfig = modelConfig, keywordsFile = File(modelDir, "keywords.txt").absolutePath, keywordsScore = 1.5f, keywordsThreshold = 0.25f)
                val kws = KeywordSpotter(config = config)
                spotter = kws
                val vocabulary = File(modelDir, "tokens.txt").readLines()
                    .mapNotNull { line -> line.trim().split(Regex("\\s+")).firstOrNull() }
                    .toSet()
                val encodedKeywords = listOf(normalizedPhrase, "HEY $normalizedPhrase", "YA $normalizedPhrase")
                    .flatMap { phrase -> BpeKeywordTokenizer.alternatives(phrase, vocabulary).map { "$it @NOVA" } }
                    .distinct()
                    .joinToString("/")
                if (encodedKeywords.isBlank()) throw IllegalStateException("تعذر ترميز كلمة التنبيه لنموذج الصوت")
                val stream = kws.createStream(encodedKeywords)
                activeStream = stream
                runMicrophoneLoop(kws, stream)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { mainHandler.post { onUnavailable(error.message ?: "تعذر تشغيل محرك كلمة التنبيه المحلي") } }
            finally { releaseResources() }
        }
    }

    private suspend fun ensureModelInstalled(): File = withContext(Dispatchers.IO) {
        val root = File(context.filesDir, "voice-models")
        val modelDir = File(root, MODEL_DIR)
        if (hasInstalledModel(modelDir)) return@withContext modelDir
        if (!root.exists() && !root.mkdirs()) throw IllegalStateException("تعذر إنشاء مجلد نماذج الصوت")
        val archive = File(root, "$MODEL_DIR.tar.bz2")
        val request = Request.Builder().url(MODEL_ARCHIVE).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("فشل تنزيل نموذج كلمة التنبيه: HTTP ${response.code}")
            val body = response.body ?: throw IllegalStateException("ملف نموذج كلمة التنبيه فارغ")
            FileOutputStream(archive).use { output -> body.byteStream().copyTo(output) }
        }
        if (!modelDir.exists() && !modelDir.mkdirs()) throw IllegalStateException("تعذر تجهيز مجلد نموذج الصوت")
        BZip2CompressorInputStream(BufferedInputStream(FileInputStream(archive))).use { bz2 ->
            TarArchiveInputStream(bz2).use { tar ->
                while (true) {
                    val entry = tar.nextTarEntry ?: break
                    if (entry.isDirectory) continue
                    val name = entry.name.substringAfterLast("/")
                    if (name !in ALL_MODEL_FILES) continue
                    FileOutputStream(File(modelDir, name)).use { output -> tar.copyTo(output) }
                }
            }
        }
        archive.delete()
        if (!hasInstalledModel(modelDir)) { modelDir.deleteRecursively(); throw IllegalStateException("نموذج كلمة التنبيه لم يكتمل تنزيله") }
        modelDir
    }

    private fun hasInstalledModel(directory: File): Boolean {
        val requiredPresent = REQUIRED_FILES.all { File(directory, it).isFile && File(directory, it).length() > 0L }
        val encoderPresent = listOf(ENCODER_INT8, ENCODER_FLOAT).any { File(directory, it).isFile && File(directory, it).length() > 0L }
        val joinerPresent = listOf(JOINER_INT8, JOINER_FLOAT).any { File(directory, it).isFile && File(directory, it).length() > 0L }
        return requiredPresent && encoderPresent && joinerPresent
    }

    private fun chooseModelFile(directory: File, preferred: String, fallback: String): String {
        val selected = listOf(preferred, fallback).firstOrNull { File(directory, it).isFile && File(directory, it).length() > 0L }
            ?: throw IllegalStateException("ملفات نموذج كلمة التنبيه غير مكتملة")
        return File(directory, selected).absolutePath
    }

    private suspend fun runMicrophoneLoop(kws: KeywordSpotter, stream: OnlineStream) {
        val minBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBytes <= 0) throw IllegalStateException("الميكروفون لا يدعم إعدادات الصوت المطلوبة")
        val audio = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBytes * 2)
        if (audio.state != AudioRecord.STATE_INITIALIZED) { audio.release(); throw IllegalStateException("تعذر تهيئة الميكروفون") }
        recorder = audio
        audio.startRecording()
        val shortBuffer = ShortArray(SAMPLE_RATE / 10)
        while (currentCoroutineContext().isActive) {
            val count = audio.read(shortBuffer, 0, shortBuffer.size)
            if (count == 0) continue
            if (count < 0) throw IllegalStateException("تعذر قراءة الصوت من الميكروفون: $count")
            val samples = FloatArray(count) { index -> shortBuffer[index] / 32768.0f }
            stream.acceptWaveform(samples, sampleRate = SAMPLE_RATE)
            while (kws.isReady(stream)) {
                kws.decode(stream)
                if (kws.getResult(stream).keyword.isNotBlank()) { kws.reset(stream); mainHandler.post { onDetected?.invoke() }; return }
            }
        }
    }

    fun stop() { worker?.cancel(); worker = null; releaseResources(); onDetected = null }
    fun destroy() { stop(); scope.cancel() }
    private fun releaseResources() {
        runCatching { recorder?.stop() }; runCatching { recorder?.release() }; recorder = null
        runCatching { activeStream?.release() }; activeStream = null
        runCatching { spotter?.release() }; spotter = null
    }
}
