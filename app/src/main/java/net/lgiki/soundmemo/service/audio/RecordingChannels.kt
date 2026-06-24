package net.lgiki.soundmemo.service.audio

import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.os.Build
import net.lgiki.soundmemo.data.settings.RecordingChannelMode
import net.lgiki.soundmemo.domain.recorder.RecordingFormat

internal data class RecordingChannels(
    val channelCount: Int,
    val inputChannelMask: Int,
) {
    companion object {
        val Mono = RecordingChannels(
            channelCount = 1,
            inputChannelMask = AudioFormat.CHANNEL_IN_MONO,
        )
        val Stereo = RecordingChannels(
            channelCount = 2,
            inputChannelMask = AudioFormat.CHANNEL_IN_STEREO,
        )

        fun resolve(
            mode: RecordingChannelMode,
            format: RecordingFormat,
            preferredDevice: AudioDeviceInfo?,
        ): RecordingChannels {
            if (mode != RecordingChannelMode.Stereo || !format.supportsStereoRecording) {
                return Mono
            }
            return if (AudioChannelSupport.supportsStereo(preferredDevice)) Stereo else Mono
        }
    }
}

private val RecordingFormat.supportsStereoRecording: Boolean
    get() = this != RecordingFormat.ThreeGp

private object AudioChannelSupport {
    fun supportsStereo(preferredDevice: AudioDeviceInfo?): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        if (preferredDevice == null) return true
        return preferredDevice.channelCounts.any { it == RecordingChannels.Stereo.channelCount }
    }
}
