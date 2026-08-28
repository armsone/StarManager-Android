package com.armsone.starmanager.ui.composer

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ContinuousCameraCapture(
    maxCount: Int,
    currentCount: Int,
    onDone: (List<ByteArray>) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val photos = remember { mutableStateListOf<ByteArray>() }
    val scope = rememberCoroutineScope()
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isCapturing by remember { mutableStateOf(false) }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    DisposableEffect(lensFacing, lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        val listener = Runnable {
            val provider = future.get()
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                imageCapture = capture
            }
        }
        future.addListener(listener, executor)
        onDispose { runCatching { future.get().unbindAll() } }
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            Row(
                Modifier.align(Alignment.TopCenter).padding(top = 42.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) { Text("취소", color = Color.White, fontSize = 17.sp) }
                Text("${currentCount + photos.size}/$maxCount", color = Color.White, fontSize = 16.sp)
                TextButton(onClick = { onDone(photos.toList()) }, enabled = photos.isNotEmpty() && !isCapturing) {
                    Text("완료", color = if (photos.isNotEmpty()) Color.White else Color.Gray, fontSize = 17.sp)
                }
            }

            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 42.dp),
                horizontalArrangement = Arrangement.spacedBy(42.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else CameraSelector.LENS_FACING_BACK
                    },
                    enabled = !isCapturing
                ) {
                    Icon(Icons.Filled.Cameraswitch, "카메라 전환", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                IconButton(
                    onClick = {
                        val capture = imageCapture ?: return@IconButton
                        if (isCapturing || currentCount + photos.size >= maxCount) return@IconButton
                        isCapturing = true
                        val file = File.createTempFile("aibi-camera-", ".jpg", context.cacheDir)
                        capture.takePicture(
                            ImageCapture.OutputFileOptions.Builder(file).build(),
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    scope.launch {
                                        val prepared = withContext(Dispatchers.IO) {
                                            runCatching {
                                                ComposerImagePipeline.prepareForComposer(file.readBytes())
                                            }.getOrNull()
                                        }
                                        file.delete()
                                        prepared?.takeIf { it.isNotEmpty() }?.let(photos::add)
                                        isCapturing = false
                                        if (currentCount + photos.size >= maxCount) onDone(photos.toList())
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    file.delete()
                                    isCapturing = false
                                }
                            }
                        )
                    },
                    enabled = !isCapturing && currentCount + photos.size < maxCount,
                    modifier = Modifier.size(78.dp).background(Color.White, CircleShape)
                ) {
                    Icon(Icons.Filled.CameraAlt, "사진 촬영", tint = Color.Black, modifier = Modifier.size(38.dp))
                }
            }
        }
    }
}
