package net.lgiki.soundmemo

import android.app.Application

class SoundMemoApplication : Application() {
    val container by lazy { SoundMemoContainer(this) }
}

