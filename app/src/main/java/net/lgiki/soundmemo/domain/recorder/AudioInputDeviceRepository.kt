package net.lgiki.soundmemo.domain.recorder

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MicrophoneInfo
import android.os.Build
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

    fun currentDevices(): List<AudioInputDevice> {
        val microphoneDetails = microphoneDetailsById()
        return currentAudioDeviceInfos().map { device ->
            device.toAudioInputDevice(microphoneDetails[device.id])
        }
    }

    fun findPreferredDevice(preference: AudioInputPreference?): AudioDeviceInfo? {
        if (preference == null) return null
        val devices = currentAudioDeviceInfos()
        return devices.firstOrNull { preference.matches(it.toAudioInputDevice(null)) }
            ?: devices.firstOrNull { preference.matchesTypeAndName(it.toAudioInputDevice(null)) }
    }

    private fun currentAudioDeviceInfos(): List<AudioDeviceInfo> =
        audioManager
            .getDevices(AudioManager.GET_DEVICES_INPUTS)
            .filter { it.isSource }
            .sortedWith(compareBy<AudioDeviceInfo> { deviceTypeSortOrder(it.type) }.thenBy { it.productName.toString() })

    private fun AudioDeviceInfo.toAudioInputDevice(microphoneInfo: MicrophoneInfo?): AudioInputDevice =
        AudioInputDevice(
            id = id,
            type = type,
            productName = audioInputProductName(type, productName),
            details = AudioInputDeviceDetails(
                channelCounts = channelCounts.toList(),
                sampleRates = sampleRates.toList(),
                location = microphoneInfo?.location?.toAudioInputLocation(),
                directionality = microphoneInfo?.directionality?.toAudioInputDirectionality(),
                group = microphoneInfo?.group?.takeUnless { it == MicrophoneInfo.GROUP_UNKNOWN },
                indexInGroup = microphoneInfo?.indexInTheGroup
                    ?.takeUnless { it == MicrophoneInfo.INDEX_IN_THE_GROUP_UNKNOWN },
            ),
        )

    private fun microphoneDetailsById(): Map<Int, MicrophoneInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { audioManager.microphones.associateBy { it.id } }.getOrDefault(emptyMap())
        } else {
            emptyMap()
        }

    private fun Int.toAudioInputLocation(): AudioInputLocation? = when (this) {
        MicrophoneInfo.LOCATION_MAINBODY -> AudioInputLocation.MainBody
        MicrophoneInfo.LOCATION_MAINBODY_MOVABLE -> AudioInputLocation.MovableMainBody
        MicrophoneInfo.LOCATION_PERIPHERAL -> AudioInputLocation.Peripheral
        else -> null
    }

    private fun Int.toAudioInputDirectionality(): AudioInputDirectionality? = when (this) {
        MicrophoneInfo.DIRECTIONALITY_OMNI -> AudioInputDirectionality.Omni
        MicrophoneInfo.DIRECTIONALITY_BI_DIRECTIONAL -> AudioInputDirectionality.BiDirectional
        MicrophoneInfo.DIRECTIONALITY_CARDIOID -> AudioInputDirectionality.Cardioid
        MicrophoneInfo.DIRECTIONALITY_HYPER_CARDIOID -> AudioInputDirectionality.HyperCardioid
        MicrophoneInfo.DIRECTIONALITY_SUPER_CARDIOID -> AudioInputDirectionality.SuperCardioid
        else -> null
    }

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
