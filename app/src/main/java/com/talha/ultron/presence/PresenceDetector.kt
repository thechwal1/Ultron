package com.talha.ultron.presence

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.os.Build
import com.talha.ultron.SecureSettings

class PresenceDetector(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val settings = SecureSettings(context)

    private var isNear = false
    private var isScreenOn = false

    init {
        if (settings.presenceUseProximity && proximitySensor != null) {
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun getStatus(): String {
        val checks = mutableListOf<String>()

        if (settings.presenceUseScreenState) {
            isScreenOn = powerManager.isInteractive
            checks.add("Screen is ${if (isScreenOn) "on" else "off"}")
        }

        if (settings.presenceUseProximity && proximitySensor != null) {
            checks.add("Phone is ${if (isNear) "near your face" else "not near your face"}")
        }

        if (settings.presenceUseBluetooth) {
            val bt = bluetoothManager?.adapter
            val connected = bt?.bondedDevices?.any { it.isConnected } ?: false
            checks.add("Bluetooth wearable ${if (connected) "connected" else "not connected"}")
        }

        val away = !isScreenOn || (settings.presenceUseProximity && proximitySensor != null && isNear)
        return checks.joinToString(". ") + ". Overall: you appear ${if (away) "away from" else "near"} your phone."
    }

    fun isAway(): Boolean {
        return !powerManager.isInteractive || (proximitySensor != null && isNear)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            isNear = event.values[0] < proximitySensor!!.maximumRange
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    fun destroy() {
        sensorManager.unregisterListener(this)
    }
}
