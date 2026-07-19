package net.lgiki.soundmemo.domain.recorder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object RecordingLocationProvider {
    private const val LOCATION_TIMEOUT_MS = 2_500L

    suspend fun currentLocation(context: Context): RecordingLocation? {
        val appContext = context.applicationContext
        if (!hasLocationPermission(appContext)) return null
        val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return withContext(Dispatchers.IO) {
            withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                currentLocation(appContext, manager)
            } ?: lastKnownLocation(manager, permittedProviders(appContext))
        }
    }

    internal fun lastKnownLocation(context: Context): RecordingLocation? {
        val appContext = context.applicationContext
        if (!hasLocationPermission(appContext)) return null
        val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return lastKnownLocation(manager, permittedProviders(appContext))
    }

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun canCaptureLocation(context: Context): Boolean {
        val appContext = context.applicationContext
        if (!hasLocationPermission(appContext)) return false
        val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.isLocationEnabled
        } else {
            permittedProviders(appContext).any { provider ->
                runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
            }
        }
    }

    private suspend fun currentLocation(context: Context, manager: LocationManager): RecordingLocation? {
        val provider = preferredProvider(context, manager) ?: return null
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                currentLocationApi30(context, manager, provider)
            } else {
                currentLocationLegacy(manager, provider)
            }?.toRecordingLocation()
                ?.takeIf { isLocationTimestampFresh(it.capturedAt) }
        }.getOrNull()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("MissingPermission")
    private suspend fun currentLocationApi30(context: Context, manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()
            val executor = Executor { command -> ContextCompat.getMainExecutor(context).execute(command) }
            manager.getCurrentLocation(provider, cancellationSignal, executor) { location ->
                if (continuation.isActive) continuation.resume(location)
            }
            continuation.invokeOnCancellation { cancellationSignal.cancel() }
        }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private suspend fun currentLocationLegacy(manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (continuation.isActive) continuation.resume(location)
                }

                override fun onProviderDisabled(provider: String) {
                    manager.removeUpdates(this)
                    if (continuation.isActive) continuation.resume(null)
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            continuation.invokeOnCancellation { manager.removeUpdates(listener) }
        }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(manager: LocationManager, providers: List<String>): RecordingLocation? =
        providers
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .filter { location -> isLocationTimestampFresh(location.time) }
            .maxByOrNull { it.time }
            ?.toRecordingLocation()

    private fun preferredProvider(context: Context, manager: LocationManager): String? =
        permittedProviders(context)
            .firstOrNull { provider -> runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false) }

    private fun permittedProviders(context: Context): List<String> {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return buildList {
            if (fineGranted) add(LocationManager.GPS_PROVIDER)
            if (fineGranted || coarseGranted) add(LocationManager.NETWORK_PROVIDER)
        }
    }

    private fun Location.toRecordingLocation(): RecordingLocation =
        RecordingLocation(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = if (hasAccuracy()) accuracy else null,
            capturedAt = if (time > 0L) time else System.currentTimeMillis(),
        )

    internal fun isLocationTimestampFresh(
        capturedAt: Long,
        now: Long = System.currentTimeMillis(),
    ): Boolean = capturedAt > 0L &&
        capturedAt >= now - MAX_LOCATION_AGE_MS &&
        capturedAt <= now + MAX_FUTURE_LOCATION_OFFSET_MS

    internal const val MAX_LOCATION_AGE_MS = 5L * 60L * 1000L
    private const val MAX_FUTURE_LOCATION_OFFSET_MS = 60L * 1000L
}
