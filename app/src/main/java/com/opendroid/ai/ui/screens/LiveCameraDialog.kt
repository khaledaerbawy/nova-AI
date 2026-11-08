package com.opendroid.ai.ui.screens

import android.content.Context
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun LiveCameraDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED)
    }
    var permissionRequested by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        permissionRequested = true
    }
    var detectedLabels by remember { mutableStateOf(emptyList<String>()) }
    var captureMessage by remember { mutableStateOf("الكاميرا جاهزة للتحليل") }
    var useFrontCamera by remember { mutableStateOf(false) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val detector = remember {
        ObjectDetection.getClient(
            ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableClassification()
                .enableMultipleObjects()
                .build()
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            detector.close()
            executor.shutdown()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF050914))
        ) {
            if (hasPermission) {
                LiveCameraPreview(
                    context = context,
                    useFrontCamera = useFrontCamera,
                    detector = detector,
                    executor = executor,
                    onLabels = { labels -> detectedLabels = labels },
                    onCaptureStatus = { captureMessage = it }
                )
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("NOVA VISION", color = Color(0xFF78E7F5), style = MaterialTheme.typography.labelLarge)
                    Text("اسمح لنوفا باستخدام الكاميرا لعرض بث مباشر وتحليل الأشياء حولك على الجهاز.", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) }) {
                        Text(if (permissionRequested) "السماح مرة أخرى" else "السماح بالكاميرا")
                    }
                    TextButton(onClick = onDismiss) { Text("رجوع", color = Color.LightGray) }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Color(0xDD050914), Color.Transparent)))
                    .padding(start = 22.dp, end = 14.dp, top = 24.dp, bottom = 28.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("NOVA VISION", color = Color(0xFF78E7F5), style = MaterialTheme.typography.labelLarge)
                        Text("رؤية مباشرة", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    }
                    Surface(color = Color(0x3327D9C2), shape = RoundedCornerShape(30.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x8878E7F5))) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(if (hasPermission) Color(0xFF5EF0C2) else Color.Gray))
                            Spacer(Modifier.width(7.dp))
                            Text(if (hasPermission) "LIVE" else "OFFLINE", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "إغلاق", tint = Color.White) }
                }
            }

            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xF2050914))))
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = Color(0xB8101D30),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x5578E7F5))
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text("تحليل مباشر", color = Color(0xFF78E7F5), style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = detectedLabels.takeIf { it.isNotEmpty() }?.joinToString("  ·  ") ?: "وجّه الكاميرا للأشياء… نوفا بتحاول تتعرف عليها",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(captureMessage, color = Color(0xFF9AABC2), style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = { useFrontCamera = !useFrontCamera },
                        modifier = Modifier.size(52.dp).clip(CircleShape).background(Color(0xCC18263A))
                    ) { Icon(Icons.Default.FlipCameraAndroid, "تبديل الكاميرا", tint = Color.White) }
                    Surface(color = Color(0xFF78E7F5), shape = CircleShape, modifier = Modifier.size(72.dp)) {
                        IconButton(onClick = { captureMessage = "استخدم زر الالتقاط داخل معاينة الكاميرا لحفظ لقطة." }) {
                            Icon(Icons.Default.PhotoCamera, "التقاط صورة", tint = Color(0xFF07111D), modifier = Modifier.size(30.dp))
                        }
                    }
                    Spacer(Modifier.size(52.dp))
                }
            }
        }
    }
}

@Composable
private fun LiveCameraPreview(
    context: Context,
    useFrontCamera: Boolean,
    detector: com.google.mlkit.vision.objects.ObjectDetector,
    executor: java.util.concurrent.ExecutorService,
    onLabels: (List<String>) -> Unit,
    onCaptureStatus: (String) -> Unit
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(lifecycleOwner, useFrontCamera, previewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val capture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { proxy ->
                val mediaImage = proxy.image
                if (mediaImage == null) {
                    proxy.close()
                } else {
                    val input = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                    detector.process(input)
                        .addOnSuccessListener { objects ->
                            val labels = objects.flatMap { item -> item.labels.map { it.text } }.distinct().take(5)
                            onLabels(labels)
                        }
                        .addOnFailureListener { onLabels(emptyList()) }
                        .addOnCompleteListener { proxy.close() }
                }
            }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture,
                    analysis
                )
                imageCapture = capture
            } catch (error: Exception) {
                onCaptureStatus("تعذر تشغيل الكاميرا: ${error.localizedMessage ?: "خطأ غير معروف"}")
            }
        }
        providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            runCatching { providerFuture.get().unbindAll() }
            imageCapture = null
        }
    }

    // Capture button is exposed as a floating control over the preview.
    Box(Modifier.fillMaxSize()) {
        androidx.compose.material3.FloatingActionButton(
            onClick = {
                val capture = imageCapture
                if (capture == null) {
                    onCaptureStatus("الكاميرا لسه بتجهز…")
                } else {
                    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val file = File(context.cacheDir, "nova_vision_$stamp.jpg")
                    val options = ImageCapture.OutputFileOptions.Builder(file).build()
                    capture.takePicture(options, executor, object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            onCaptureStatus("تم التقاط صورة وحفظها مؤقتًا داخل NOVA")
                        }
                        override fun onError(exception: ImageCaptureException) {
                            onCaptureStatus("فشل التقاط الصورة: ${exception.message}")
                        }
                    })
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 126.dp),
            containerColor = Color(0xFF78E7F5),
            contentColor = Color(0xFF07111D)
        ) {
            Icon(Icons.Default.PhotoCamera, "التقاط صورة")
        }
    }
}
