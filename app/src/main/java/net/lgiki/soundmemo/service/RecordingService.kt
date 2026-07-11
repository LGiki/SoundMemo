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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.lgiki.soundmemo.MainActivity
import net.lgiki.soundmemo.R
import net.lgiki.soundmemo.SoundMemoApplication
import net.lgiki.soundmemo.data.storage.GeneratedRecordingName
import net.lgiki.soundmemo.data.storage.RecordingNameTemplate
import net.lgiki.soundmemo.domain.recorder.RecordingFormat
import net.lgiki.soundmemo.domain.recorder.RecordingLocation
import net.lgiki.soundmemo.domain.recorder.RecordingLocationProvider
import net.lgiki.soundmemo.domain.recorder.RecorderStatus
import net.lgiki.soundmemo.domain.recorder.RecorderUiState
import net.lgiki.soundmemo.domain.recorder.RecordingStateHolder
import net.lgiki.soundmemo.domain.recorder.WAVEFORM_SAMPLE_COUNT
import net.lgiki.soundmemo.service.audio.AudioRecordingBackend
import net.lgiki.soundmemo.service.audio.MediaRecorderBackend
import net.lgiki.soundmemo.service.audio.PcmRecordingBackend
import net.lgiki.soundmemo.service.audio.RecordingChannels
import net.lgiki.soundmemo.util.formatDuration
import net.lgiki.soundmemo.util.wrapWithLocale

class RecordingService : LifecycleService() {
    private var recorder: AudioRecordingBackend? = null
    private var outputFile: File? = null
    private var outputGeneratedName: GeneratedRecordingName? = null
    private var outputDisplayName: String? = null
    private var outputFormat: RecordingFormat? = null
    private var outputBitrate: Int = 0
    private var outputSampleRate: Int = 0
    private var recordingLocation: RecordingLocation? = null
    @Volatile private var isStarting = false
    @Volatile private var isStopping = false
    private var pendingStopSave: Boolean? = null
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
        ensureSavedChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> startRecording(intent.getBooleanExtra(EXTRA_RECORD_LOCATION, false))
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stopRecording(
                save = true,
                fromNotification = intent.getBooleanExtra(EXTRA_FROM_NOTIFICATION, false),
            )
            ACTION_CANCEL -> stopRecording(save = false)
        }
        return Service.START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private fun startRecording(recordLocation: Boolean) {
        if (recorder != null || isStarting || isStopping) return
        isStarting = true
        val captureLocation = recordLocation && RecordingLocationProvider.canCaptureLocation(this)
        val foregroundStartFailure = runCatching {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildStartingNotification(),
                foregroundServiceType(captureLocation),
            )
        }.exceptionOrNull()
        if (foregroundStartFailure != null) {
            isStarting = false
            RecordingStateHolder.update(
                RecorderUiState(
                    status = RecorderStatus.Error,
                    message = foregroundStartFailure.localizedMessage ?: getString(R.string.recorder_start_failed),
                ),
            )
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        lifecycleScope.launch {
            val location = if (captureLocation) {
                RecordingLocationProvider.currentLocation(this@RecordingService)
            } else {
                null
            }
            if (pendingStopSave != null) {
                pendingStopSave = null
                isStarting = false
                RecordingStateHolder.update(RecorderUiState(status = RecorderStatus.Idle))
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@launch
            }
            var recordingBackend: AudioRecordingBackend? = null
            var file: File? = null
            var started = false
            runCatching {
                val settings = container.settingsRepository.settings.first()
                val recordingFormat = settings.recordingFormat
                val recordingBitrate = recordingFormat.bitrateFor(settings.bitrate)
                val recordingSampleRate = recordingFormat.sampleRateFor(settings.sampleRate)
                val preferredAudioInput = settings.preferredAudioInput
                val preferredAudioDevice = container.audioInputDeviceRepository.findPreferredDevice(preferredAudioInput)
                val recordingChannels = RecordingChannels.resolve(
                    mode = settings.recordingChannelMode,
                    format = recordingFormat,
                    preferredDevice = preferredAudioDevice,
                )
                val generatedName = RecordingNameTemplate.generate(
                    template = settings.recordingNameTemplate,
                    extension = recordingFormat.extension,
                )
                val createdFile = container.recordingStorage.createTempOutputFile(generatedName)
                val createdRecorder = if (recordingFormat.usesPcmRecorder) {
                    PcmRecordingBackend(
                        context = this@RecordingService,
                        file = createdFile,
                        format = recordingFormat,
                        bitrate = recordingBitrate,
                        sampleRate = recordingSampleRate,
                        channels = recordingChannels,
                        preferredDevice = preferredAudioDevice,
                    )
                } else {
                    MediaRecorderBackend(
                        context = this@RecordingService,
                        file = createdFile,
                        format = recordingFormat,
                        bitrate = recordingBitrate,
                        sampleRate = recordingSampleRate,
                        channels = recordingChannels,
                        location = location,
                        writeLocationToMediaFile = settings.writeLocationToMediaFile,
                        preferredDevice = preferredAudioDevice,
                    )
                }
                file = createdFile
                recordingBackend = createdRecorder
                recorder = createdRecorder
                outputFile = createdFile
                outputGeneratedName = generatedName
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
                RecordingStateHolder.update(
                    RecorderUiState(
                        status = RecorderStatus.Recording,
                        preferredAudioInput = preferredAudioInput,
                        actualAudioInput = createdRecorder.routedDevice,
                    ),
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
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }.also {
                isStarting = false
                pendingStopSave?.let { save ->
                    pendingStopSave = null
                    stopRecording(save)
                }
            }
        }
    }

    private fun pauseRecording() {
        if (isStarting || isStopping) return
        val activeRecorder = recorder ?: return
        if (RecordingStateHolder.state.value.status != RecorderStatus.Recording) return
        try {
            activeRecorder.pause()
            pausedAt = SystemClock.elapsedRealtime()
            RecordingStateHolder.update(RecordingStateHolder.state.value.copy(status = RecorderStatus.Paused, amplitude = 0))
            notifyStatus(RecorderStatus.Paused)
        } catch (exception: Exception) {
            failRecording(exception)
        }
    }

    private fun resumeRecording() {
        if (isStarting || isStopping) return
        val activeRecorder = recorder ?: return
        if (RecordingStateHolder.state.value.status != RecorderStatus.Paused) return
        try {
            activeRecorder.resume()
            pausedTotal += SystemClock.elapsedRealtime() - pausedAt
            pausedAt = 0L
            RecordingStateHolder.update(RecordingStateHolder.state.value.copy(status = RecorderStatus.Recording))
            notifyStatus(RecorderStatus.Recording)
        } catch (exception: Exception) {
            failRecording(exception)
        }
    }

    private fun failRecording(exception: Exception) {
        val file = outputFile
        ticker?.cancel()
        cleanupRecorder()
        file?.delete()
        RecordingStateHolder.update(
            RecorderUiState(
                status = RecorderStatus.Error,
                message = exception.localizedMessage ?: getString(R.string.recorder_start_failed),
            ),
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopRecording(save: Boolean, fromNotification: Boolean = false) {
        if (isStopping) return
        if (isStarting) {
            pendingStopSave = save
            return
        }
        val activeRecorder = recorder ?: return stopSelf()
        isStopping = true
        RecordingStateHolder.update(RecordingStateHolder.state.value.copy(status = RecorderStatus.Saving, amplitude = 0))
        ticker?.cancel()
        lifecycleScope.launch(Dispatchers.IO) {
            val file = outputFile
            val generatedName = outputGeneratedName
            val displayName = outputDisplayName
            val activeFormat = outputFormat
            val activeBitrate = outputBitrate
            val activeSampleRate = outputSampleRate
            val elapsed = currentElapsed()
            val location = recordingLocation
            var savedConfirmationMessage: String? = null
            try {
                activeRecorder.stop()
                cleanupRecorder()
                if (save && file != null && generatedName != null && file.exists() && file.length() > 0) {
                    val settings = container.settingsRepository.settings.first()
                    val recordingFormat = activeFormat ?: settings.recordingFormat
                    val saveResult = container.recordingStorage.publishRecording(
                        tempFile = file,
                        generatedName = generatedName,
                        location = settings.recordingStorageLocation,
                        format = recordingFormat.storageValue,
                        customFolderUri = settings.customRecordingFolderUri,
                    )
                    val id = container.recordingRepository.addFromSaveResult(
                        saveResult = saveResult,
                        name = displayName.orEmpty(),
                        durationMs = elapsed,
                        bitrate = activeBitrate.takeIf { it > 0 } ?: recordingFormat.bitrateFor(settings.bitrate),
                        sampleRate = activeSampleRate.takeIf { it > 0 } ?: recordingFormat.sampleRateFor(settings.sampleRate),
                        format = recordingFormat.storageValue,
                        location = location,
                    )
                    val savedMessage = if (saveResult.fellBackToAppFiles) {
                        getString(R.string.recorder_saved_to_app_files_message)
                    } else {
                        getString(R.string.recorder_saved_message)
                    }
                    RecordingStateHolder.update(
                        RecorderUiState(
                            status = RecorderStatus.Saved,
                            lastSavedId = id,
                            message = if (fromNotification) null else savedMessage,
                        ),
                    )
                    if (fromNotification) {
                        savedConfirmationMessage = savedMessage
                    }
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
                savedConfirmationMessage?.let(::showSavedConfirmationNotification)
                isStopping = false
                stopSelf()
            }
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = lifecycleScope.launch {
            var routeRefreshTick = 0
            var lastNotifiedSecond = -1L
            while (true) {
                val current = RecordingStateHolder.state.value
                val status = current.status
                recorder?.failure?.let { failure ->
                    failRecording(failure as? Exception ?: RuntimeException(failure))
                    return@launch
                }
                val elapsedMs = currentElapsed()
                val amplitude = if (status == RecorderStatus.Recording) runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0) else 0
                val refreshRoute = current.actualAudioInput == null || routeRefreshTick++ % ROUTE_REFRESH_TICKS == 0
                val routedDevice = if (refreshRoute) {
                    recorder?.routedDevice ?: current.actualAudioInput
                } else {
                    current.actualAudioInput
                }
                val waveform = if (status == RecorderStatus.Recording) {
                    (current.waveform + normalizedWaveformSample(amplitude)).takeLast(WAVEFORM_SAMPLE_COUNT)
                } else {
                    current.waveform
                }
                RecordingStateHolder.update(
                    current.copy(
                        elapsedMs = elapsedMs,
                        amplitude = amplitude,
                        waveform = waveform,
                        actualAudioInput = routedDevice,
                    ),
                )
                val elapsedSecond = elapsedMs / 1000
                if (
                    (status == RecorderStatus.Recording || status == RecorderStatus.Paused) &&
                    elapsedSecond != lastNotifiedSecond
                ) {
                    lastNotifiedSecond = elapsedSecond
                    updateNotification(status, elapsedMs)
                }
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
        outputGeneratedName = null
        outputDisplayName = null
        outputFormat = null
        outputBitrate = 0
        outputSampleRate = 0
        recordingLocation = null
        isStarting = false
        pendingStopSave = null
        startedAt = 0L
        pausedAt = 0L
        pausedTotal = 0L
        ticker?.cancel()
    }

    private fun ensureChannel() {
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
        updateNotification(status, currentElapsed())
    }

    private fun updateNotification(status: RecorderStatus, elapsedMs: Long) {
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            runCatching {
                NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification(status, elapsedMs))
            }
        }
    }

    private fun ensureSavedChannel() {
        val channel = NotificationChannel(
            SAVED_CHANNEL_ID,
            getString(R.string.notification_saved_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.notification_saved_channel_desc)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun showSavedConfirmationNotification(message: String) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        runCatching {
            val openIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val notification = NotificationCompat.Builder(this, SAVED_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setTimeoutAfter(SAVED_CONFIRMATION_TIMEOUT_MS)
                .setContentIntent(openIntent)
                .build()
            NotificationManagerCompat.from(this).notify(SAVED_NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(status: RecorderStatus, elapsedMs: Long): Notification {
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
        val durationText = formatDuration(elapsedMs)
        val contentText = if (status == RecorderStatus.Paused) {
            getString(R.string.notification_text_paused, durationText)
        } else {
            getString(R.string.notification_text_recording, durationText)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(contentText)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(pauseResumeAction)
            .addAction(NotificationCompat.Action(0, getString(R.string.notification_action_stop), commandIntent(ACTION_STOP, 3, fromNotification = true)))
            .build()
    }

    private fun buildStartingNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text_starting))
            .setOngoing(true)
            .setContentIntent(openIntent)
            .build()
    }

    private fun foregroundServiceType(recordLocation: Boolean): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                if (recordLocation) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        } else {
            0
        }

    private fun commandIntent(action: String, requestCode: Int, fromNotification: Boolean = false): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, RecordingService::class.java).setAction(action).apply {
                if (fromNotification) putExtra(EXTRA_FROM_NOTIFICATION, true)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    companion object {
        private const val CHANNEL_ID = "recording"
        private const val SAVED_CHANNEL_ID = "recording_saved"
        private const val NOTIFICATION_ID = 1001
        private const val SAVED_NOTIFICATION_ID = 1002
        private const val SAVED_CONFIRMATION_TIMEOUT_MS = 4_000L
        private const val MAX_PCM_AMPLITUDE = 32767f
        private const val ROUTE_REFRESH_TICKS = 4
        const val ACTION_START = "net.lgiki.soundmemo.START_RECORDING"
        const val ACTION_PAUSE = "net.lgiki.soundmemo.PAUSE_RECORDING"
        const val ACTION_RESUME = "net.lgiki.soundmemo.RESUME_RECORDING"
        const val ACTION_STOP = "net.lgiki.soundmemo.STOP_RECORDING"
        const val ACTION_CANCEL = "net.lgiki.soundmemo.CANCEL_RECORDING"
        private const val EXTRA_FROM_NOTIFICATION = "net.lgiki.soundmemo.extra.FROM_NOTIFICATION"
        private const val EXTRA_RECORD_LOCATION = "net.lgiki.soundmemo.extra.RECORD_LOCATION"

        fun startIntent(context: Context, action: String, recordLocation: Boolean = false): Intent =
            Intent(context, RecordingService::class.java).setAction(action).apply {
                putExtra(EXTRA_RECORD_LOCATION, recordLocation)
            }
    }
}
