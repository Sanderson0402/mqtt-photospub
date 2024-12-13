package com.example.mqttpublisher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager as AndroidLocationManager
import android.util.Log
import androidx.core.app.ActivityCompat

class LocationManager(private val context: Context) {
    private var currentLocation: Location? = null
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as AndroidLocationManager

    // Coordenadas padrão (local específico)
    private val defaultLocation = Location("").apply {
        latitude = -22.772663
        longitude = -43.6857564
    }

    fun requestLocationUpdates() {
        // Verificando as permissões em tempo de execução
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("LocationManager", "Permissão para acessar a localização não concedida.")
            return
        }

        val providers = listOf(
            AndroidLocationManager.GPS_PROVIDER,
            AndroidLocationManager.NETWORK_PROVIDER
        )

        for (provider in providers) {
            try {
                locationManager.requestLocationUpdates(
                    provider,
                    1000L,  // Minimo intervalo de tempo: 1 segundo
                    10f,    // Minima distancia: 10 metros
                    object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            currentLocation = location
                            Log.d("LocationManager", "Localização recebida: ${location.latitude}, ${location.longitude}")
                        }

                        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {
                            Log.d("LocationManager", "Status da localização mudou para: $status, provider: $provider")
                        }

                        override fun onProviderEnabled(provider: String) {
                            Log.d("LocationManager", "Provider $provider habilitado")
                        }

                        override fun onProviderDisabled(provider: String) {
                            Log.d("LocationManager", "Provider $provider desabilitado")
                        }
                    }
                )
            } catch (e: SecurityException) {
                Log.e("LocationManager", "Erro ao tentar obter atualizações de localização: ${e.message}")
            }
        }
    }

    fun getCurrentLocation(): Location {
        // Verifique a permissão antes de tentar obter a localização
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val lastKnownLocation = locationManager.getLastKnownLocation(AndroidLocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(AndroidLocationManager.NETWORK_PROVIDER)

                if (lastKnownLocation != null) {
                    currentLocation = lastKnownLocation
                    Log.d("LocationManager", "Localização obtida: ${lastKnownLocation.latitude}, ${lastKnownLocation.longitude}")
                    return lastKnownLocation
                } else {
                    Log.w("LocationManager", "Nenhuma localização conhecida disponível.")
                }
            } catch (e: SecurityException) {
                Log.e("LocationManager", "Erro ao tentar obter localização: ${e.message}")
            }
        } else {
            Log.w("LocationManager", "Permissão de localização não concedida.")
        }

        // Se não conseguir obter localização, retorna localização padrão
        Log.w("LocationManager", "Usando localização padrão: ${defaultLocation.latitude}, ${defaultLocation.longitude}")
        return defaultLocation
    }
}


