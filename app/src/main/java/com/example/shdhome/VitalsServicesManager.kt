package com.example.shdhome

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler

class VitalsServicesManager constructor(
    private val context: Context,
    private val sensorManager: SensorManager,
    resources: Resources,
    customTypeface: Typeface
) {
    private var sensor: Sensor? = null
    private var lastReading: Float = 0f
    private var olderReadings: FloatArray = FloatArray(10) { 0f }
    private val mTypeface: Typeface = customTypeface
    private val heartIcon: Drawable =
        resources.getDrawable(R.drawable.heart, resources.newTheme())
    private val orange: Int = Color.parseColor("#ff6d10")
    private lateinit var orangePaint: Paint
    public var isFaceVisible: Boolean = false

    init {
        // This will get the default sensor for the heart rate.
        val tempVal = sensorManager.getDefaultSensor(
            Sensor.TYPE_HEART_RATE, true)

        // This is the paint that we'll use to draw the text on the canvas.
        orangePaint = Paint().apply {
            color = orange
            textSize = 24f
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            typeface = mTypeface
        }

        if (tempVal != null) {
            sensor = tempVal
        }
    }

    private fun measure(callback: () -> Boolean) {
        var sensorEventListener: SensorEventListener? = null

        sensorEventListener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                //Not needed
                println("Accuracy changed to '$accuracy'")
            }

            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_HEART_RATE) {
                    lastReading = event.values[0]
                    sensorManager.unregisterListener(sensorEventListener!!)
                    callback()
                }
            }
        }

        // Register an event listener for the heart rate.
        sensorManager.registerListener(
            sensorEventListener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    fun draw(canvas: Canvas, width: Int) {
        val halfWidth = width / 2
        val labelText = "${lastReading.toInt()}bpm"
        val xPos = ((width / 2) - (12 * labelText.length / 2)).toFloat()

        heartIcon.setBounds(
            halfWidth - 16,
            width - 58,
            halfWidth + 16,
            width - 26
        )
        heartIcon.draw(canvas)

        canvas.drawText(labelText, xPos, width - 68f, orangePaint)
    }

    fun registerListener() {
        if (sensor == null) {
            return
        }

//        Handler().postDelayed(object : Runnable {
//            override fun run() {
//                println("hello, world!")
//            }
//        }, 1000)

        measure {
            println("Reading $lastReading")

            for (i in 1 until olderReadings.size) {
                olderReadings[10 - i] = olderReadings[10 - (i + 1)]
            }

            olderReadings[0] = lastReading

            var delayMills = when (isFaceVisible) {
                true -> 15000
                false -> 25000
            }.toLong()

            Handler().postDelayed({ registerListener() }, delayMills)
        }
    }
}

/*sensorManager.registerListener(object : SensorEventListener {
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Passthrough.
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_GRAVITY) {
            System.out.println("X: ${event.values[0]} Y: ${event.values[1]} Z: ${event.values[2]}")
        }
    }
}, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), Sensor.TYPE_GRAVITY)*/
