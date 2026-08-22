package com.talha.ultron.brain

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import java.util.regex.Pattern

class SystemToggleHandler(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val flashlightPattern = Pattern.compile("(?i)(turn (on|off) flashlight|toggle flashlight|flashlight)")
    private val wifiPattern = Pattern.compile("(?i)(turn (on|off) wifi|toggle wifi|wifi (on|off))")
    private val bluetoothPattern = Pattern.compile("(?i)(turn (on|off) bluetooth|toggle bluetooth|bluetooth (on|off))")
    private val brightnessPattern = Pattern.compile("(?i)set brightness (\d+)%?")
    private val volumePattern = Pattern.compile("(?i)set volume (\d+)%?")

    fun tryHandle(input: String): String? {
        val flashMatcher = flashlightPattern.matcher(input)
        if (flashMatcher.find()) {
            val state = flashMatcher.group(2)?.lowercase()
            return if (state == "on" || state == null) toggleFlashlight(true)
            else toggleFlashlight(false)
        }

        val wifiMatcher = wifiPattern.matcher(input)
        if (wifiMatcher.find()) {
            val state = wifiMatcher.group(2)?.lowercase() ?: wifiMatcher.group(3)?.lowercase()
            return toggleWifi(state == "on" || state == null)
        }

        val btMatcher = bluetoothPattern.matcher(input)
        if (btMatcher.find()) {
            val state = btMatcher.group(2)?.lowercase() ?: btMatcher.group(3)?.lowercase()
            return toggleBluetooth(state == "on" || state == null)
        }

        val brightnessMatcher = brightnessPattern.matcher(input)
        if (brightnessMatcher.find()) {
            val percent = brightnessMatcher.group(1)?.toIntOrNull() ?: return null
            return setBrightness(percent)
        }

        return null
    }

    private fun toggleFlashlight(on: Boolean): String {
        return try {
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, on)
            if (on) "Flashlight turned on." else "Flashlight turned off."
        } catch (e: CameraAccessException) {
            "I couldn't control the flashlight."
        }
    }

    private fun toggleWifi(on: Boolean): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "On Android 10+, apps can't directly toggle Wi-Fi. Please use the quick settings panel."
        } else {
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = on
            if (on) "Wi-Fi turned on." else "Wi-Fi turned off."
        }
    }

    private fun toggleBluetooth(on: Boolean): String {
        return if (bluetoothAdapter == null) {
            "Bluetooth is not available on this device."
        } else {
            if (on) {
                bluetoothAdapter.enable()
                "Bluetooth turned on."
            } else {
                bluetoothAdapter.disable()
                "Bluetooth turned off."
            }
        }
    }

    private fun setBrightness(percent: Int): String {
        return try {
            val value = (percent.coerceIn(0, 100) / 100.0 * 255).toInt()
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
            "Brightness set to $percent%."
        } catch (e: SecurityException) {
            "I need WRITE_SETTINGS permission to change brightness."
        }
    }
}
