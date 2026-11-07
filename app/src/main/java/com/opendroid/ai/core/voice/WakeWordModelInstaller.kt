package com.opendroid.ai.core.voice

import android.content.Context
import com.k2fsa.sherpa.onnx.KeywordSpotter
import com.github.k2fsa.sherpa.onnx.KeywordSpotterConfig
import com.github.k2fsa.sherpa.onnx.OnlineModelConfig
import com.github.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import com.github.k2fsa.sherpa.onnx.FeatureConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

/**
 * Installs the small, quantized sherpa-onnx KWS model on first use.
 * Model files live in app-private storage and are never added to the APK.
 */
class WakeWordModelInstaller(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(4, TimeUnit.MINUTES)
        .build()
) {
    private val installMutex = Mutex()

    suspend fun ensureInstalled(): File = withContext(Dispatchers.IO) {
        installMutex.withLock {
            val parent = File(context.filesDir, MODEL_PARENT)
            val target = File(parent, MODEL_DIR)
            if (hasRequiredFiles(target)) return@withLock target

            if (!parent.exists() && !parent.mkdirs()) {
                error("Unable to create wake-word model directory")
            }
            val archive = File(context.cacheDir, "nova-kws-model.tar.bz2")
            val staging = File(parent, ".installing")
            staging.deleteRecursively()
            if (!staging.mkdirs()) error("Unable to prepare wake-word model directory")

            try {
                downloadModel(archive)
                extractRequiredFiles(archive, staging)
                check(hasRequiredFiles(staging)) { "Wake-word model archive is incomplete" }
                target.deleteRecursively()
                check(staging.renameTo(target)) { "Unable to activate wake-word model" }
                target
            } catch (failure: Exception) {
                staging.deleteRecursively()
                throw failure
            } finally {
                archive.delete()
            }
        }
    }

    private fun downloadModel(destination: File) {
        val request = Request.Builder().url(MODEL_URL).get().build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Wake-word model download failed: HTTP ${response.code}" }
            val body = response.body ?: error("Wake-word model download returned no data")
            FileOutputStream(destination).use { output -> body.byteStream().use { it.copyTo(output) } }
        }
    }

    private fun extractRequiredFiles(archive: File, destination: File) {
        val required = REQUIRED_FILES.toSet()
        TarArchiveInputStream(
            BZip2CompressorInputStream(BufferedInputStream(FileInputStream(archive)))
        ).use { tar ->
            while (true) {
                val entry = tar.nextTarEntry ?: break
                if (entry.isDirectory) continue
                val name = File(entry.name).name
                if (name !in required) continue
                val output = File(destination, name)
                FileOutputStream(output).use { stream -> tar.copyTo(stream) }
            }
        }
        File(destination, KEYWORDS_FILE).writeText(KEYWORDS_CONTENT)
    }

    private fun hasRequiredFiles(directory: File): Boolean =
        REQUIRED_FILES.all { name -> File(directory, name).let { it.isFile && it.length() > 0L } } &&
            File(directory, KEYWORDS_FILE).let { it.isFile && it.length() > 0L }

    fun createSpotter(modelDirectory: File): KeywordSpotter {
        val model = OnlineModelConfig(
            transducer = OnlineTransducerModelConfig(
                encoder = File(modelDirectory, ENCODER_FILE).absolutePath,
                decoder = File(modelDirectory, DECODER_FILE).absolutePath,
                joiner = File(modelDirectory, JOINER_FILE).absolutePath
            ),
            tokens = File(modelDirectory, TOKENS_FILE).absolutePath,
            numThreads = 1,
            modelType = "zipformer2",
            modelingUnit = "phone+ppinyin",
            bpeVocab = File(modelDirectory, LEXICON_FILE).absolutePath
        )
        return KeywordSpotter(
            config = KeywordSpotterConfig(
                featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80),
                modelConfig = model,
                maxActivePaths = 4,
                keywordsFile = File(modelDirectory, KEYWORDS_FILE).absolutePath,
                keywordsScore = 1.5f,
                keywordsThreshold = 0.35f,
                numTrailingBlanks = 1
            )
        )
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        private const val MODEL_PARENT = "voice"
        private const val MODEL_DIR = "sherpa-kws-zh-en"
        private const val MODEL_URL =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/kws-models/sherpa-onnx-kws-zipformer-zh-en-3M-2025-12-20.tar.bz2"

        private const val ENCODER_FILE = "encoder-epoch-13-avg-2-chunk-16-left-64.int8.onnx"
        private const val DECODER_FILE = "decoder-epoch-13-avg-2-chunk-16-left-64.onnx"
        private const val JOINER_FILE = "joiner-epoch-13-avg-2-chunk-16-left-64.int8.onnx"
        private const val TOKENS_FILE = "tokens.txt"
        private const val LEXICON_FILE = "en.phone"
        private const val KEYWORDS_FILE = "nova-keywords.txt"

        private val REQUIRED_FILES = listOf(
            ENCODER_FILE, DECODER_FILE, JOINER_FILE, TOKENS_FILE, LEXICON_FILE
        )

        // Phonetic spellings let the English-phone KWS model detect the Egyptian
        // pronunciation of "يا نوفا" without pretending it is an Arabic-trained model.
        private const val KEYWORDS_CONTENT = """N OW1 F AH0 @NOFA
N OW1 V AH0 @NOVA
Y AA1 N OW1 F AH0 @YA_NOFA
"""

        private fun modelDirectory(context: Context) =
            File(File(context.filesDir, MODEL_PARENT), MODEL_DIR)
    }
}
