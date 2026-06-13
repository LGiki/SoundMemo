package net.lgiki.soundmemo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.lgiki.soundmemo.MainActivity
import net.lgiki.soundmemo.R
import net.lgiki.soundmemo.SoundMemoApplication
import net.lgiki.soundmemo.domain.recorder.RecorderStatus
import net.lgiki.soundmemo.domain.recorder.RecorderUiState
import net.lgiki.soundmemo.domain.recorder.RecordingStateHolder

class RecordingService : LifecycleService() {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAt = 0L
    private var pausedAt = 0L
    private var pausedTotal = 0L
    private var ticker: Job? = null

    private val container by lazy { (application as SoundMemoApplication).container }

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stopRecording(save = true)
            ACTION_CANCEL -> stopRecording(save = false)
        }
        return Service.START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private fun startRecording() {
        if (recorder != null) return
        lifecycleScope.launch {
            runCatching {
                val settings = container.settingsRepository.settings.first()
                val file = container.recordingStorage.createOutputFile()
                val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(this@RecordingService)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                mediaRecorder.setAudioEncodingBitRate(settings.bitrate)
                mediaRecorder.setAudioSamplingRate(settings.sampleRate)
                mediaRecorder.setOutputFile(file.absolutePath)
                mediaRecorder.prepare()
                mediaRecorder.start()
                recorder = mediaRecorder
                outputFile = file
                startedAt = SystemClock.elapsedRealtime()
                pausedAt = 0L
                pausedTotal = 0L
                RecordingStateHolder.update(RecorderUiState(status = RecorderStatus.Recording))
                startForeground(NOTIFICATION_ID, buildNotification(RecorderStatus.Recording))
                startTicker()
            }.onFailure {
                cleanupRecorder()
                RecordingStateHolder.update(
                    RecorderUiState(
                        status = RecorderStatus.Error,
                        message = it.localizedMessage ?: "Recording could not start.",
                    ),
                )
                stopSelf()
            }
        }
    }

    private fun pauseRecording() {
        val activeRecorder = recorder ?: return
        if (RecordingStateHolder.state.value.status != RecorderStatus.Recording) return
        runCatching {
            activeRecorder.pause()
            pausedAt = SystemClock.elapsedRealtime()
            RecordingStateHolder.update(RecordingStateHolder.state.value.copy(status = RecorderStatus.Paused, amplitude = 0))
            notifyStatus(RecorderStatus.Paused)
        }
    }

    private fun resumeRecording() {
        val activeRecorder = recorder ?: return
        if (RecordingStateHolder.state.value.status != RecorderStatus.Paused) return
        runCatching {
            activeRecorder.resume()
            pausedTotal += SystemClock.elapsedRealtime() - pausedAt
            pausedAt = 0L
            RecordingStateHolder.update(RecordingStateHolder.state.value.copy(status = RecorderStatus.Recording))
            notifyStatus(RecorderStatus.Recording)
        }
    }

    private fun stopRecording(save: Boolean) {
        val activeRecorder = recorder ?: return stopSelf()
        RecordingStateHolder.update(RecordingStateHolder.state.value.copy(status = RecorderStatus.Saving, amplitude = 0))
        ticker?.cancel()
        lifecycleScope.launch {
            val file = outputFile
            val elapsed = currentElapsed()
            runCatching {
                activeRecorder.stop()
            }
            cleanupRecorder()
            if (save && file != null && file.exists() && file.length() > 0) {
                val settings = container.settingsRepository.settings.first()
                val id = container.recordingRepository.addFromFile(
                    file = file,
                    durationMs = elapsed,
                    bitrate = settings.bitrate,
                    sampleRate = settings.sampleRate,
                )
                RecordingStateHolder.update(
                    RecorderUiState(status = RecorderStatus.Saved, elapsedMs = elapsed, lastSavedId = id, message = "Recording saved."),
                )
            } else {
                file?.delete()
                RecordingStateHolder.update(RecorderUiState(status = RecorderStatus.Idle))
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = lifecycleScope.launch {
            while (true) {
                val status = RecordingStateHolder.state.value.status
                val amplitude = if (status == RecorderStatus.Recording) runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0) else 0
                RecordingStateHolder.update(
                    RecordingStateHolder.state.value.copy(
                        elapsedMs = currentElapsed(),
                        amplitude = amplitude,
                    ),
                )
                delay(250)
            }
        }
    }

    private fun currentElapsed(): Long {
        if (startedAt == 0L) return 0L
        val now = if (pausedAt > 0L) pausedAt else SystemClock.elapsedRealtime()
        return (now - startedAt - pausedTotal).coerceAtLeast(0L)
    }

    private fun cleanupRecorder() {
        runCatching { recorder?.release() }
        recorder = null
        outputFile = null
        startedAt = 0L
        pausedAt = 0L
        pausedTotal = 0L
        ticker?.cancel()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Recording",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Active SoundMemo recording controls"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun notifyStatus(status: RecorderStatus) {
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            runCatching {
                NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification(status))
            }
        }
    }

    private fun buildNotification(status: RecorderStatus): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val pauseResumeAction = if (status == RecorderStatus.Paused) {
            NotificationCompat.Action(0, "Resume", commandIntent(ACTION_RESUME, 2))
        } else {
            NotificationCompat.Action(0, "Pause", commandIntent(ACTION_PAUSE, 1))
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("SoundMemo is recording")
            .setContentText(if (status == RecorderStatus.Paused) "Recording paused" else "Recording in progress")
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(pauseResumeAction)
            .addAction(NotificationCompat.Action(0, "Stop", commandIntent(ACTION_STOP, 3)))
            .build()
    }

    private fun commandIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, RecordingService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    companion object {
        private const val CHANNEL_ID = "recording"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_START = "net.lgiki.soundmemo.START_RECORDING"
        const val ACTION_PAUSE = "net.lgiki.soundmemo.PAUSE_RECORDING"
        const val ACTION_RESUME = "net.lgiki.soundmemo.RESUME_RECORDING"
        const val ACTION_STOP = "net.lgiki.soundmemo.STOP_RECORDING"
        const val ACTION_CANCEL = "net.lgiki.soundmemo.CANCEL_RECORDING"

        fun startIntent(context: Context, action: String): Intent =
            Intent(context, RecordingService::class.java).setAction(action)
    }
}
