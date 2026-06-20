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
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import java.io.File
import kotlin.math.sqrt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.lgiki.soundmemo.MainActivity
import net.lgiki.soundmemo.R
import net.lgiki.soundmemo.SoundMemoApplication
import net.lgiki.soundmemo.data.storage.RecordingNameTemplate
import net.lgiki.soundmemo.domain.recorder.RecordingFormat
import net.lgiki.soundmemo.domain.recorder.RecordingLocation
import net.lgiki.soundmemo.domain.recorder.RecorderStatus
import net.lgiki.soundmemo.domain.recorder.RecorderUiState
import net.lgiki.soundmemo.domain.recorder.RecordingStateHolder
import net.lgiki.soundmemo.domain.recorder.WAVEFORM_SAMPLE_COUNT
import net.lgiki.soundmemo.service.audio.AudioRecordingBackend
import net.lgiki.soundmemo.service.audio.MediaRecorderBackend
import net.lgiki.soundmemo.service.audio.PcmRecordingBackend
import net.lgiki.soundmemo.util.wrapWithLocale

class RecordingService : LifecycleService() {
    private var recorder: AudioRecordingBackend? = null
    private var outputFile: File? = null
    private var outputDisplayName: String? = null
    private var outputFormat: RecordingFormat? = null
    private var outputBitrate: Int = 0
    private var outputSampleRate: Int = 0
    private var recordingLocation: RecordingLocation? = null
    private var isStarting = false
    private var isStopping = false
    private var startedAt = 0L
    private var pausedAt = 0L
    private var pausedTotal = 0L
    private var ticker: Job? = null

    private val container by lazy { (application as SoundMemoApplication).container }

    override fun attachBaseContext(newBase: Context) {
        val app = newBase.applicationContext as SoundMemoApplication
        super.attachBaseContext(newBase.wrapWithLocale(app.currentLocale))
    }

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> startRecording(intent.recordingLocationExtra())
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stopRecording(save = true)
            ACTION_CANCEL -> stopRecording(save = false)
        }
        return Service.START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private fun startRecording(location: RecordingLocation?) {
        if (recorder != null || isStarting || isStopping) return
        isStarting = true
        lifecycleScope.launch {
            var recordingBackend: AudioRecordingBackend? = null
            var file: File? = null
            var started = false
            runCatching {
                val settings = container.settingsRepository.settings.first()
                val recordingFormat = settings.recordingFormat
                val recordingBitrate = recordingFormat.bitrateFor(settings.bitrate)
                val recordingSampleRate = recordingFormat.sampleRateFor(settings.sampleRate)
                val generatedName = RecordingNameTemplate.generate(
                    template = settings.recordingNameTemplate,
                    extension = recordingFormat.extension,
                )
                val createdFile = container.recordingStorage.createOutputFile(generatedName)
                val createdRecorder = if (recordingFormat.usesPcmRecorder) {
                    PcmRecordingBackend(
                        file = createdFile,
                        format = recordingFormat,
                        bitrate = recordingBitrate,
                        sampleRate = recordingSampleRate,
                    )
                } else {
                    MediaRecorderBackend(
                        context = this@RecordingService,
                        file = createdFile,
                        format = recordingFormat,
                        bitrate = recordingBitrate,
                        sampleRate = recordingSampleRate,
                        location = location,
                        writeLocationToMediaFile = settings.writeLocationToMediaFile,
                    )
                }
                file = createdFile
                recordingBackend = createdRecorder
                recorder = createdRecorder
                outputFile = createdFile
                outputDisplayName = generatedName.displayName
                outputFormat = recordingFormat
                outputBitrate = recordingBitrate
                outputSampleRate = recordingSampleRate
                recordingLocation = location
                createdRecorder.start()
                started = true
                startedAt = SystemClock.elapsedRealtime()
                pausedAt = 0L
                pausedTotal = 0L
                RecordingStateHolder.update(RecorderUiState(status = RecorderStatus.Recording))
                ServiceCompat.startForeground(
                    this@RecordingService,
                    NOTIFICATION_ID,
                    buildNotification(RecorderStatus.Recording),
                    foregroundServiceType(),
                )
                startTicker()
            }.onFailure {
                if (it is CancellationException) throw it
                if (started) {
                    runCatching { recordingBackend?.stop() }
                }
                cleanupRecorder()
                file?.delete()
                RecordingStateHolder.update(
                    RecorderUiState(
                        status = RecorderStatus.Error,
                        message = it.localizedMessage ?: getString(R.string.recorder_start_failed),
                    ),
                )
                stopSelf()
            }.also {
                isStarting = false
            }
        }
    }

    private fun pauseRecording() {
        if (isStarting || isStopping) return
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
        if (isStarting || isStopping) return
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
        if (isStopping) return
        if (isStarting) return
        val activeRecorder = recorder ?: return stopSelf()
        isStopping = true
        RecordingStateHolder.update(RecordingStateHolder.state.value.copy(status = RecorderStatus.Saving, amplitude = 0))
        ticker?.cancel()
        lifecycleScope.launch {
            val file = outputFile
            val displayName = outputDisplayName
            val activeFormat = outputFormat
            val activeBitrate = outputBitrate
            val activeSampleRate = outputSampleRate
            val elapsed = currentElapsed()
            val location = recordingLocation
            try {
                activeRecorder.stop()
                cleanupRecorder()
                if (save && file != null && file.exists() && file.length() > 0) {
                    val settings = container.settingsRepository.settings.first()
                    val recordingFormat = activeFormat ?: settings.recordingFormat
                    val id = container.recordingRepository.addFromFile(
                        file = file,
                        name = displayName.orEmpty(),
                        durationMs = elapsed,
                        bitrate = activeBitrate.takeIf { it > 0 } ?: recordingFormat.bitrateFor(settings.bitrate),
                        sampleRate = activeSampleRate.takeIf { it > 0 } ?: recordingFormat.sampleRateFor(settings.sampleRate),
                        format = recordingFormat.storageValue,
                        location = location,
                    )
                    RecordingStateHolder.update(
                        RecorderUiState(status = RecorderStatus.Saved, elapsedMs = elapsed, lastSavedId = id, message = getString(R.string.recorder_saved_message)),
                    )
                } else {
                    file?.delete()
                    RecordingStateHolder.update(RecorderUiState(status = RecorderStatus.Idle))
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                cleanupRecorder()
                file?.delete()
                RecordingStateHolder.update(
                    RecorderUiState(
                        status = RecorderStatus.Error,
                        message = exception.localizedMessage ?: getString(R.string.recorder_start_failed),
                    ),
                )
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                isStopping = false
                stopSelf()
            }
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = lifecycleScope.launch {
            while (true) {
                val current = RecordingStateHolder.state.value
                val status = current.status
                val amplitude = if (status == RecorderStatus.Recording) runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0) else 0
                val waveform = if (status == RecorderStatus.Recording) {
                    (current.waveform + normalizedWaveformSample(amplitude)).takeLast(WAVEFORM_SAMPLE_COUNT)
                } else {
                    current.waveform
                }
                RecordingStateHolder.update(
                    current.copy(
                        elapsedMs = currentElapsed(),
                        amplitude = amplitude,
                        waveform = waveform,
                    ),
                )
                delay(250)
            }
        }
    }

    private fun normalizedWaveformSample(amplitude: Int): Float {
        val linear = (amplitude / MAX_PCM_AMPLITUDE).coerceIn(0f, 1f)
        return sqrt(linear).coerceIn(0f, 1f)
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
        outputDisplayName = null
        outputFormat = null
        outputBitrate = 0
        outputSampleRate = 0
        recordingLocation = null
        isStarting = false
        startedAt = 0L
        pausedAt = 0L
        pausedTotal = 0L
        ticker?.cancel()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_desc)
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
            NotificationCompat.Action(0, getString(R.string.notification_action_resume), commandIntent(ACTION_RESUME, 2))
        } else {
            NotificationCompat.Action(0, getString(R.string.notification_action_pause), commandIntent(ACTION_PAUSE, 1))
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(if (status == RecorderStatus.Paused) getString(R.string.notification_text_paused) else getString(R.string.notification_text_recording))
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(pauseResumeAction)
            .addAction(NotificationCompat.Action(0, getString(R.string.notification_action_stop), commandIntent(ACTION_STOP, 3)))
            .build()
    }

    private fun foregroundServiceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
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
        private const val MAX_PCM_AMPLITUDE = 32767f
        const val ACTION_START = "net.lgiki.soundmemo.START_RECORDING"
        const val ACTION_PAUSE = "net.lgiki.soundmemo.PAUSE_RECORDING"
        const val ACTION_RESUME = "net.lgiki.soundmemo.RESUME_RECORDING"
        const val ACTION_STOP = "net.lgiki.soundmemo.STOP_RECORDING"
        const val ACTION_CANCEL = "net.lgiki.soundmemo.CANCEL_RECORDING"
        private const val EXTRA_LOCATION_LATITUDE = "net.lgiki.soundmemo.extra.LOCATION_LATITUDE"
        private const val EXTRA_LOCATION_LONGITUDE = "net.lgiki.soundmemo.extra.LOCATION_LONGITUDE"
        private const val EXTRA_LOCATION_ACCURACY = "net.lgiki.soundmemo.extra.LOCATION_ACCURACY"
        private const val EXTRA_LOCATION_CAPTURED_AT = "net.lgiki.soundmemo.extra.LOCATION_CAPTURED_AT"

        fun startIntent(context: Context, action: String, location: RecordingLocation? = null): Intent =
            Intent(context, RecordingService::class.java).setAction(action).apply {
                if (location != null) {
                    putExtra(EXTRA_LOCATION_LATITUDE, location.latitude)
                    putExtra(EXTRA_LOCATION_LONGITUDE, location.longitude)
                    location.accuracyMeters?.let { putExtra(EXTRA_LOCATION_ACCURACY, it) }
                    putExtra(EXTRA_LOCATION_CAPTURED_AT, location.capturedAt)
                }
            }

        private fun Intent.recordingLocationExtra(): RecordingLocation? {
            if (!hasExtra(EXTRA_LOCATION_LATITUDE) || !hasExtra(EXTRA_LOCATION_LONGITUDE)) return null
            return RecordingLocation(
                latitude = getDoubleExtra(EXTRA_LOCATION_LATITUDE, 0.0),
                longitude = getDoubleExtra(EXTRA_LOCATION_LONGITUDE, 0.0),
                accuracyMeters = if (hasExtra(EXTRA_LOCATION_ACCURACY)) getFloatExtra(EXTRA_LOCATION_ACCURACY, 0f) else null,
                capturedAt = getLongExtra(EXTRA_LOCATION_CAPTURED_AT, System.currentTimeMillis()),
            )
        }
    }
}
