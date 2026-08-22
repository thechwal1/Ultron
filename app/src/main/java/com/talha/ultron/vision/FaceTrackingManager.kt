package com.talha.ultron.vision

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors

/**
 * Uses CameraX + ML Kit face detection to determine if someone is looking
 * at the phone. Used for presence detection — if no face is detected for
 * a while, the user is likely away.
 */
class FaceTrackingManager(private val context: Context) {

    private val executor = Executors.newSingleThreadExecutor()
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )

    private var isFaceVisible = false
    private var lastFaceTimestamp = 0L

    fun start(lifecycleOwner: LifecycleOwner, onFaceStateChanged: (Boolean) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(executor) { imageProxy ->
                        processImage(imageProxy, onFaceStateChanged)
                    }
                }
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, analysis)
            } catch (e: Exception) {
                // Camera binding failed
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun processImage(imageProxy: ImageProxy, callback: (Boolean) -> Unit) {
        val mediaImage = imageProxy.image ?: return
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                val faceVisible = faces.isNotEmpty()
                if (faceVisible != isFaceVisible) {
                    isFaceVisible = faceVisible
                    callback(faceVisible)
                }
                if (faceVisible) lastFaceTimestamp = System.currentTimeMillis()
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    fun isFaceCurrentlyVisible(): Boolean = isFaceVisible

    fun timeSinceLastFace(): Long = System.currentTimeMillis() - lastFaceTimestamp

    fun destroy() {
        executor.shutdown()
        faceDetector.close()
    }
}
