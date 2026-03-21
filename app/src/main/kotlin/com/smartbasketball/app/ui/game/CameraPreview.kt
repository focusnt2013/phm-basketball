package com.smartbasketball.app.ui.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.smartbasketball.app.util.AppLogger
import java.util.concurrent.Executors

private var lastNoFaceLogTime = 0L

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onFaceDetected: (Bitmap, Face, Int, Int) -> Unit,
    onFaceLost: () -> Unit = {},
    onCameraReady: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    AppLogger.d("CameraPreview: 开始初始化")
    
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraStarted by remember { mutableStateOf(false) }
    
    val faceDetector = remember {
        AppLogger.d("CameraPreview: 创建FaceDetector")
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setMinFaceSize(0.15f)
            .build()
        FaceDetection.getClient(options)
    }
    
    DisposableEffect(Unit) {
        AppLogger.d("CameraPreview: DisposableEffect onDispose")
        onDispose {
            cameraExecutor.shutdown()
            faceDetector.close()
        }
    }

    AndroidView(
        modifier = modifier.background(Color.DarkGray),
        factory = { ctx ->
            AppLogger.d("CameraPreview: AndroidView factory")
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        update = { previewView ->
            if (!cameraStarted) {
                cameraStarted = true
                AppLogger.d("CameraPreview: 开始启动摄像头")
                startCamera(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    executor = cameraExecutor,
                    faceDetector = faceDetector,
                    onFaceDetected = onFaceDetected,
                    onFaceLost = onFaceLost,
                    onCameraReady = onCameraReady
                )
            }
        }
    )
}

private fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    executor: java.util.concurrent.ExecutorService,
    faceDetector: com.google.mlkit.vision.face.FaceDetector,
    onFaceDetected: (Bitmap, Face, Int, Int) -> Unit,
    onFaceLost: () -> Unit,
    onCameraReady: () -> Unit
) {
    AppLogger.d("startCamera: 获取CameraProvider")
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()
            AppLogger.d("startCamera: CameraProvider获取成功")
            
            val hasFrontCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
            val hasBackCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
            AppLogger.d("startCamera: 前置摄像头可用=$hasFrontCamera, 后置摄像头可用=$hasBackCamera")

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val isFrontCamera = hasFrontCamera
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .setTargetResolution(android.util.Size(1920, 1080))
                .build()
                .also {
                    var firstFrameReceived = false
                    it.setAnalyzer(executor) { imageProxy ->
                        val onFirstFrame = {
                            if (!firstFrameReceived) {
                                firstFrameReceived = true
                                onCameraReady()
                            }
                        }
                        processImage(imageProxy, faceDetector, onFaceDetected, onFaceLost, onFirstFrame, isFrontCamera)
                    }
                }

            val cameraSelector = if (hasFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else if (hasBackCamera) {
                AppLogger.w("startCamera: 前置摄像头不可用，使用后置摄像头")
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                AppLogger.e("startCamera: 没有可用的摄像头")
                return@addListener
            }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
            AppLogger.d("startCamera: 摄像头绑定成功，等待第一帧...")
        } catch (e: Exception) {
            AppLogger.e("startCamera: 摄像头初始化失败: ${e.message}", e)
        }
    }, ContextCompat.getMainExecutor(context))
}

@androidx.camera.core.ExperimentalGetImage
    private fun processImage(
    imageProxy: ImageProxy,
    faceDetector: com.google.mlkit.vision.face.FaceDetector,
    onFaceDetected: (Bitmap, Face, Int, Int) -> Unit,
    onFaceLost: () -> Unit,
    onFirstFrame: () -> Unit,
    isFrontCamera: Boolean = true
) {
    val image = imageProxy.image ?: run {
        imageProxy.close()
        return
    }

    try {
        val bitmap = imageProxy.toBitmap()
        var rotatedBitmap = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees.toFloat())
        
        // 前置摄像头需要水平翻转（镜像），因为预览是镜像显示的
        if (isFrontCamera) {
            rotatedBitmap = flipBitmapHorizontally(rotatedBitmap)
        }
        
        onFirstFrame()
        
        val inputImage = InputImage.fromBitmap(rotatedBitmap, 0)
        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    AppLogger.d("原图尺寸: ${rotatedBitmap.width}x${rotatedBitmap.height}")
                    onFaceDetected(rotatedBitmap, faces[0], rotatedBitmap.width, rotatedBitmap.height)
                } else {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastNoFaceLogTime > 7000) {
                        AppLogger.d("processImage: 未检测到人脸")
                        lastNoFaceLogTime = currentTime
                    }
                    onFaceLost()
                }
            }
            .addOnFailureListener { e ->
                AppLogger.e("processImage: 人脸检测失败: ${e.message}")
                onFaceLost()
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } catch (e: Exception) {
        AppLogger.e("processImage: 图像处理异常: ${e.message}")
        imageProxy.close()
    }
}

private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    if (degrees == 0f) return bitmap
    
    val matrix = Matrix().apply {
        postRotate(degrees)
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun flipBitmapHorizontally(bitmap: Bitmap): Bitmap {
    val matrix = Matrix().apply {
        preScale(-1f, 1f)
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
