package com.example.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import com.example.model.GnssReading
import com.example.model.ImuReading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.sqrt

data class SensorAvailability(
    val hasAccel: Boolean = false,
    val hasGyro: Boolean = false,
    val hasMag: Boolean = false,
    val hasLinearAccel: Boolean = false,
    val hasRotationVector: Boolean = false,
    val hasGps: Boolean = false,
    val activeSensorsCount: Int = 0
)

/**
 * Live Android Sensor & GNSS Manager (ISRO NavDR)
 * Listens to on-device hardware IMU (Accel, Gyro, Mag, Rotation Vector) and GPS/NavIC location stream.
 */
class LiveSensorManager(private val context: Context) : SensorEventListener, LocationListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _liveImu = MutableStateFlow(ImuReading())
    val liveImu: StateFlow<ImuReading> = _liveImu.asStateFlow()

    private val _liveGnss = MutableStateFlow<GnssReading?>(null)
    val liveGnss: StateFlow<GnssReading?> = _liveGnss.asStateFlow()

    private val _isSensorsActive = MutableStateFlow(false)
    val isSensorsActive: StateFlow<Boolean> = _isSensorsActive.asStateFlow()

    private val _sensorAvailability = MutableStateFlow(SensorAvailability())
    val sensorAvailability: StateFlow<SensorAvailability> = _sensorAvailability.asStateFlow()

    private var curAx = 0f
    private var curAy = 0f
    private var curAz = 9.81f

    private var curGx = 0f
    private var curGy = 0f
    private var curGz = 0f

    private var curMx = 0f
    private var curMy = 0f
    private var curMz = 0f

    var sensorRateHz = 50
        private set
    private var sampleCount = 0
    private var lastRateCalcTimeMs = System.currentTimeMillis()

    init {
        checkAvailability()
    }

    private fun checkAvailability() {
        if (sensorManager == null) return
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null
        val mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
        val linAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null
        val rotVec = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
        val gps = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false

        var count = 0
        if (accel) count++
        if (gyro) count++
        if (mag) count++
        if (linAccel) count++
        if (rotVec) count++
        if (gps) count++

        _sensorAvailability.value = SensorAvailability(
            hasAccel = accel,
            hasGyro = gyro,
            hasMag = mag,
            hasLinearAccel = linAccel,
            hasRotationVector = rotVec,
            hasGps = gps,
            activeSensorsCount = count
        )
    }

    fun startListening() {
        if (sensorManager == null) return

        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val linAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val rotVec = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        // Register available sensors with low latency
        accel?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gyro?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        mag?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        linAccel?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        rotVec?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }

        startLocationUpdates()
        checkAvailability()
        _isSensorsActive.value = true
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
        try {
            locationManager?.removeUpdates(this)
        } catch (_: Exception) {}
        _isSensorsActive.value = false
    }

    @SuppressLint("MissingPermission")
    fun getDeviceLocation(): Location? {
        if (locationManager == null) return null
        try {
            val gpsLoc = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } else null

            val netLoc = if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } else null

            val passiveLoc = try {
                locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            } catch (_: Exception) { null }

            val candidates = listOfNotNull(gpsLoc, netLoc, passiveLoc)
            val best = candidates.maxByOrNull { it.time }

            // If we have a location, update _liveGnss
            best?.let { loc ->
                _liveGnss.value = GnssReading(
                    lat = loc.latitude,
                    lng = loc.longitude,
                    altitude = loc.altitude,
                    speedMps = loc.speed,
                    bearingDeg = loc.bearing,
                    accuracyMeters = loc.accuracy,
                    satellitesInUse = 16,
                    hdop = (loc.accuracy / 3.0f).coerceIn(0.6f, 3.5f),
                    timestampMs = loc.time,
                    isValid = true
                )
            }
            return best
        } catch (_: SecurityException) {
            return null
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (locationManager == null) return
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    500L,
                    0f,
                    this
                )
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L,
                    0f,
                    this
                )
            }
        } catch (_: SecurityException) {
            // Handled via dynamic permissions in UI
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                curAx = event.values[0]
                curAy = event.values[1]
                curAz = event.values[2]
            }
            Sensor.TYPE_GYROSCOPE -> {
                curGx = event.values[0]
                curGy = event.values[1]
                curGz = event.values[2]
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                curMx = event.values[0]
                curMy = event.values[1]
                curMz = event.values[2]
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                // If direct linear acceleration is available
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                // Device 3D rotation vector
            }
        }

        sampleCount++
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastRateCalcTimeMs >= 1000) {
            sensorRateHz = sampleCount.coerceAtLeast(1)
            sampleCount = 0
            lastRateCalcTimeMs = nowMs
        }

        _liveImu.value = ImuReading(
            accelX = curAx,
            accelY = curAy,
            accelZ = curAz,
            gyroX = curGx,
            gyroY = curGy,
            gyroZ = curGz,
            magX = curMx,
            magY = curMy,
            magZ = curMz,
            timestampNs = event.timestamp
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onLocationChanged(location: Location) {
        _liveGnss.value = GnssReading(
            lat = location.latitude,
            lng = location.longitude,
            altitude = location.altitude,
            speedMps = location.speed,
            bearingDeg = location.bearing,
            accuracyMeters = location.accuracy,
            satellitesInUse = 16,
            hdop = (location.accuracy / 3.0f).coerceIn(0.6f, 3.5f),
            timestampMs = location.time,
            isValid = true
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
