package net.lgiki.soundmemo.service.audio

import android.media.AudioDeviceInfo
import net.lgiki.soundmemo.domain.recorder.AudioInputRoute
import net.lgiki.soundmemo.domain.recorder.audioInputProductName

internal fun AudioDeviceInfo.toAudioInputRoute(): AudioInputRoute =
    AudioInputRoute(
        type = type,
        productName = audioInputProductName(type, productName),
    )
