package net.lgiki.soundmemo.domain.recorder

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class AudioInputDeviceRepository(context: Context) {
    private val audioManager = context.applicationContext.getSystemService(AudioManager::class.java)

    val devices: Flow<List<AudioInputDevice>> = callbackFlow {
        var refreshJob: kotlinx.coroutines.Job? = null

        fun sendDevices() {
            trySend(currentDevices())
        }

        fun scheduleSendDevices() {
            refreshJob?.cancel()
            refreshJob = launch {
                delay(DEVICE_REFRESH_DEBOUNCE_MS)
                sendDevices()
            }
        }

        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                scheduleSendDevices()
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                scheduleSendDevices()
            }
        }

        sendDevices()
        audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose {
            refreshJob?.cancel()
            audioManager.unregisterAudioDeviceCallback(callback)
        }
    }.conflate().distinctUntilChanged()

    fun currentDevices(): List<AudioInputDevice> =
        currentAudioDeviceInfos().map { it.toAudioInputDevice() }

    fun findPreferredDevice(preference: AudioInputPreference?): AudioDeviceInfo? {
        if (preference == null) return null
        val devices = currentAudioDeviceInfos()
        return devices.firstOrNull { preference.matches(it.toAudioInputDevice()) }
            ?: devices.firstOrNull { preference.matchesTypeAndName(it.toAudioInputDevice()) }
    }

    private fun currentAudioDeviceInfos(): List<AudioDeviceInfo> =
        audioManager
            .getDevices(AudioManager.GET_DEVICES_INPUTS)
            .filter { it.isSource }
            .sortedWith(compareBy<AudioDeviceInfo> { deviceTypeSortOrder(it.type) }.thenBy { it.productName.toString() })

    private fun AudioDeviceInfo.toAudioInputDevice(): AudioInputDevice =
        AudioInputDevice(
            id = id,
            type = type,
            productName = audioInputProductName(type, productName),
        )

    private fun deviceTypeSortOrder(type: Int): Int = when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_MIC -> 0
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_HEADSET -> 1
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_HEADSET -> 2
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> 3
        else -> 4
    }

    private companion object {
        const val DEVICE_REFRESH_DEBOUNCE_MS = 150L
    }
}
