package com.example.mqttpublisher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(private val activity: AppCompatActivity) {
    private var capturedPhoto: Bitmap? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private val imageCapture by lazy {
        ImageCapture.Builder()
            .setTargetResolution(android.util.Size(480, 640))
            .build()
    }

    init {
        outputDirectory = getOutputDirectory(activity)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    fun initializeCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    activity as LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch(exc: Exception) {
                Log.e("CameraManager", "Camera initialization failed", exc)
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    fun capturePhoto(
        onPhotoCaptured: (Bitmap) -> Unit,
        onError: ((Exception) -> Unit)? = null
    ) {
        // Criar arquivo de saída para a foto
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        // Opções de saída
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Capturar foto
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(activity),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraManager", "Photo capture failed: ${exc.message}", exc)
                    onError?.invoke(exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val bitmap = BitmapFactory.decodeFile(savedUri.path)

                    Log.d("CameraManager", "Photo capture succeeded: $savedUri")
                    onPhotoCaptured(bitmap)
                }
            }
        )
    }

    fun getCapturedPhoto(): Bitmap {
        return capturedPhoto ?: throw IllegalStateException("Nenhuma foto capturada")
    }

    private fun getOutputDirectory(context: Context): File {
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, context.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }
}