package com.example.mqttpublisher

import android.graphics.Bitmap
import android.location.Location
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MqttPublishManager(
    private val brokerUrl: String = "tcp://broker.hivemq.com:1883",
    private val topicPrefix: String = "animal/photos"
) {
    private val mqttClient: MqttClient by lazy {
        MqttClient(brokerUrl, MqttClient.generateClientId(), MemoryPersistence())
    }

    fun publish(topic: String, photo: Bitmap, location: Location, retained: Boolean = true) {
        try {
            // Cria payload
            val payload = createPayload(topic, photo, location)

            // Conecta ao broker
            connectToBroker()

            // Cria mensagem MQTT
            val message = MqttMessage(payload.toString().toByteArray()).apply {
                qos = 1  // At least once delivery
                isRetained = retained
            }

            // Publica no tópico
            val fullTopic = "$topicPrefix/$topic"
            mqttClient.publish(fullTopic, message)

            Log.d("MqttPublishManager", "Mensagem publicada com sucesso no tópico: $fullTopic")
        } catch (e: Exception) {
            Log.e("MqttPublishManager", "Erro ao publicar mensagem", e)
            throw e
        } finally {
            // Sempre desconecta após a publicação
            disconnectFromBroker()
        }
    }

    private fun connectToBroker() {
        if (!mqttClient.isConnected) {
            try {
                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    connectionTimeout = 10  // segundos
                    keepAliveInterval = 20  // segundos
                }
                mqttClient.connect(options)
                Log.d("MqttPublishManager", "Conectado ao broker: $brokerUrl")
            } catch (e: MqttException) {
                Log.e("MqttPublishManager", "Erro ao conectar ao broker", e)
                throw e
            }
        }
    }

    private fun disconnectFromBroker() {
        try {
            if (mqttClient.isConnected) {
                mqttClient.disconnect()
                Log.d("MqttPublishManager", "Desconectado do broker")
            }
        } catch (e: MqttException) {
            Log.e("MqttPublishManager", "Erro ao desconectar do broker", e)
        }
    }

    private fun createPayload(topic: String, photo: Bitmap, location: Location): JSONObject {

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val currentDate = dateFormat.format(Date()) // Obter a data atual formatada
        return JSONObject().apply {
            put("topic", topic)
            put("photo", encodeImageToBase64(photo))
            put("location", JSONObject().apply {
                put("latitude", location.latitude)
                put("longitude", location.longitude)
            })
            put("date", currentDate)
        }
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }

    companion object {
        // Máximo de tentativas de reconexão
        private const val MAX_RECONNECT_ATTEMPTS = 3
    }
}