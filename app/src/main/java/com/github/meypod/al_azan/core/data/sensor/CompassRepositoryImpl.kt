package com.github.meypod.al_azan.core.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface
import com.github.meypod.al_azan.core.domain.model.compass.CompassAccuracy
import com.github.meypod.al_azan.core.domain.model.compass.CompassReading
import com.github.meypod.al_azan.core.domain.repository.CompassRepository
import com.github.meypod.al_azan.core.util.android.TSAGeoMag
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import java.util.GregorianCalendar

/**
 * Reads the device orientation from the rotation-vector sensor and exposes it as a [Flow] of true
 * (or magnetic, if no location) headings.
 *
 * Azimuth math ported from the legacy app, originally adapted from
 * https://github.com/Kr0oked/Compass.
 */
class CompassRepositoryImpl(
    private val context: Context,
) : CompassRepository {

    private companion object {
        // ~60 Hz (16.67ms) to match a 60fps display.
        const val SENSOR_DELAY_US = 16_667

        // Low-pass filter factor: smaller = smoother but slower. Mirrors legacy ALPHA.
        const val ALPHA = 0.96f
    }

    override fun headings(
        latitude: Double?,
        longitude: Double?,
    ): Flow<CompassReading> = callbackFlow {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        if (sensorManager == null || rotationSensor == null) {
            trySend(CompassReading(headingDegrees = 0f, accuracy = CompassAccuracy.NO_SENSOR))
            awaitClose { }
            return@callbackFlow
        }

        val magneticDeclination = if (latitude != null && longitude != null) {
            val geoMag = TSAGeoMag()
            geoMag.getDeclination(
                latitude,
                longitude,
                geoMag.decimalYear(GregorianCalendar()),
                0.0,
            ).toFloat()
        } else {
            0f
        }

        val filteredReading = FloatArray(3)
        var hasFilteredReading = false
        var currentAccuracy = CompassAccuracy.UNRELIABLE

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

                if (hasFilteredReading) {
                    lowPassFilter(event.values, filteredReading)
                } else {
                    System.arraycopy(event.values, 0, filteredReading, 0, 3)
                    hasFilteredReading = true
                }

                val azimuth = calculateAzimuth(filteredReading)
                if (azimuth.isNaN()) return

                val trueHeading = ((azimuth + magneticDeclination) % 360f + 360f) % 360f
                trySend(CompassReading(headingDegrees = trueHeading, accuracy = currentAccuracy))
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                if (sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
                currentAccuracy = accuracy.toCompassAccuracy()
            }
        }

        val registered = sensorManager.registerListener(listener, rotationSensor, SENSOR_DELAY_US)
        if (!registered) {
            trySend(CompassReading(headingDegrees = 0f, accuracy = CompassAccuracy.NO_SENSOR))
        }

        awaitClose { sensorManager.unregisterListener(listener) }
    }.conflate()

    private fun lowPassFilter(input: FloatArray, output: FloatArray) {
        for (i in 0..2) {
            output[i] += ALPHA * (input[i] - output[i])
        }
    }

    private fun calculateAzimuth(rotationVector: FloatArray): Float {
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)

        val remapped = FloatArray(9)
        val (axisX, axisY) = when (displayRotation()) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remapped)

        val orientation = FloatArray(3)
        SensorManager.getOrientation(remapped, orientation)
        val azimuthDegrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
        return (azimuthDegrees + 360f) % 360f
    }

    private fun displayRotation(): Int {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager?
        return displayManager?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0
    }

    private fun Int.toCompassAccuracy(): CompassAccuracy = when (this) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> CompassAccuracy.HIGH
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> CompassAccuracy.MEDIUM
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> CompassAccuracy.LOW
        else -> CompassAccuracy.UNRELIABLE
    }
}
