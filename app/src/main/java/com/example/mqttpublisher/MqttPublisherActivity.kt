package com.example.mqttpublisher

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.mqttpublisher.CameraManager
import com.example.mqttpublisher.LocationManager
import com.example.mqttpublisher.MqttPublishManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog

class MqttPublisherActivity : AppCompatActivity() {
    private lateinit var mqttManager: MqttPublishManager
    private lateinit var cameraManager: CameraManager
    private lateinit var locationManager: LocationManager

    private val topics = listOf("cachorro", "boi", "cavalo", "capivara")
    private var selectedTopic: String = "cachorro"

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    @RequiresApi(Build.VERSION_CODES.M)
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val deniedPermissions = permissions.filter { !it.value }.keys

        if (permissions.all { it.value }) {
            setupOperations()
        } else {
            val permissionMessages = deniedPermissions.map { permission ->
                when (permission) {
                    Manifest.permission.CAMERA -> "Permissão de Câmera"
                    Manifest.permission.ACCESS_FINE_LOCATION -> "Permissão de Localização"
                    else -> permission
                }
            }.joinToString(", ")

            Toast.makeText(
                this,
                "As seguintes permissões são necessárias: $permissionMessages",
                Toast.LENGTH_LONG
            ).show()

            // Opcional: Redirecionar para configurações se o usuário negou permanentemente
            if (deniedPermissions.any { permission ->
                    !shouldShowRequestPermissionRationale(permission)
                }) {
                showSettingsDialog()
            }
        }
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissões Necessárias")
            .setMessage("Algumas permissões foram negadas permanentemente. Por favor, habilite-as nas configurações do aplicativo.")
            .setPositiveButton("Ir para Configurações") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mqtt_publisher)

        mqttManager = MqttPublishManager()
        cameraManager = CameraManager(this)
        locationManager = LocationManager(this)

        val previewView: PreviewView = findViewById(R.id.previewView)
        val captureButton: Button = findViewById(R.id.captureButton)
        val topicSpinner: Spinner = findViewById(R.id.topicSpinner)

        cameraManager.initializeCamera(previewView)

        // Configurar Spinner de tópicos
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, topics)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        topicSpinner.adapter = adapter

        topicSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                selectedTopic = topics[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedTopic = "cachorro"
            }
        }

        captureButton.setOnClickListener {
            captureAndPublishPhoto(selectedTopic)
        }

        checkAndRequestPermissions()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkAndRequestPermissions() {
        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            setupOperations()
        } else {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun setupOperations() {
        Log.d("MqttPublisherActivity", "Permissões concedidas, configurando operações")
    }

    private fun captureAndPublishPhoto(topic: String) {
        cameraManager.capturePhoto(
            onPhotoCaptured = { photoBitmap ->
                val location = locationManager.getCurrentLocation()
                    ?: throw IllegalStateException("Localização não disponível")

                mqttManager.publish(topic, photoBitmap, location)
            },
            onError = { exception ->
                Log.e("MqttPublisherActivity", "Erro ao capturar foto", exception)
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.shutdown()
    }
}