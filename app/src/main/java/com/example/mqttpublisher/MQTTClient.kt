package com.example.mqttpublisher

import org.eclipse.paho.client.mqttv3.*

class MQTTClient(
    private val brokerUrl: String = "tcp://broker.hivemq.com:1883",
    private val clientId: String = MqttClient.generateClientId()
) {
    private val mqttClient: MqttClient = MqttClient(brokerUrl, clientId)

    fun connect() {
        val options = MqttConnectOptions().apply {
            isCleanSession = true
        }
        mqttClient.connect(options)
        println("Connected to $brokerUrl")
    }

    fun publish(topic: String, payload: String, retained: Boolean = true) {
        val message = MqttMessage(payload.toByteArray()).apply {
            qos = 1
            isRetained = retained
        }
        mqttClient.publish(topic, message)
        println("Message published to topic $topic (Retained: $retained)")
    }

    fun disconnect() {
        mqttClient.disconnect()
        println("Disconnected from $brokerUrl")
    }
}
