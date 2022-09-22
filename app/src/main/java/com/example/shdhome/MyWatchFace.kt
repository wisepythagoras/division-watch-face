package com.example.shdhome

import android.Manifest
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.*
import androidx.palette.graphics.Palette
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.util.DisplayMetrics
import android.view.SurfaceHolder
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat

import java.lang.ref.WeakReference
import java.util.Calendar
import java.util.TimeZone

/**
 * Updates rate in milliseconds for interactive mode. We update once a second to advance the
 * second hand.
 */
private const val INTERACTIVE_UPDATE_RATE_MS = 1000

/**
 * Handler message id for updating the time periodically in interactive mode.
 */
private const val MSG_UPDATE_TIME = 0

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn"t
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 *
 *
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
class MyWatchFace : CanvasWatchFaceService() {

    override fun onCreateEngine(): Engine {
        return Engine()
    }

    private class EngineHandler(reference: MyWatchFace.Engine) : Handler(Looper.myLooper()!!) {
        private val mWeakReference: WeakReference<MyWatchFace.Engine> = WeakReference(reference)

        override fun handleMessage(msg: Message) {
            val engine = mWeakReference.get()
            if (engine != null) {
                when (msg.what) {
                    MSG_UPDATE_TIME -> engine.handleUpdateTimeMessage()
                }
            }
        }
    }

    inner class Engine : CanvasWatchFaceService.Engine() {
        private lateinit var mBackgroundPaint: Paint
        private lateinit var mCalendar: Calendar
        private lateinit var displayMetrics: DisplayMetrics
        private lateinit var logo: Drawable
        private lateinit var logoSmall: Drawable
        private lateinit var batteryManager: BatteryManager
        private lateinit var geoTypeface: Typeface
        private lateinit var chakraTypeface: Typeface
        private lateinit var mainTypeface: Typeface
        private lateinit var batteryRenderManager: BatteryRenderManager
        private lateinit var heartRateSensor: VitalsServicesManager
        private val sensorManager: SensorManager =
            getSystemService(Context.SENSOR_SERVICE) as SensorManager
        private var orangeColor: Int = 0
        private var lighterOrangeColor: Int = 0
        private var useChakra = false

        /* Handler to update the time once a second in interactive mode. */
        private val mUpdateTimeHandler = EngineHandler(this)

        private val mTimeZoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            setWatchFaceStyle(WatchFaceStyle.Builder(this@MyWatchFace)
                .setAcceptsTapEvents(true)
                .build())

            mCalendar = Calendar.getInstance()
            displayMetrics = DisplayMetrics()
            orangeColor = Color.parseColor("#ff6d10")
            lighterOrangeColor = Color.parseColor("#fff6b7")
            batteryManager = getSystemService(Service.BATTERY_SERVICE) as BatteryManager
            geoTypeface = Typeface.createFromAsset(assets, "fonts/georegular.ttf")
            chakraTypeface = Typeface.createFromAsset(assets, "fonts/chakrapetchregular.ttf")

            if (useChakra) {
                mainTypeface = chakraTypeface
            } else {
                mainTypeface = geoTypeface
            }

            // val manager = ContextCompat.getSystemService(applicationContext, NotificationManager::class.java) as NotificationManager
            // val intent = this.registerReceiver() // null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)

            @Suppress("DEPRECATION")
            displayContext?.display?.getRealMetrics(displayMetrics)

            logo = resources.getDrawable(R.drawable.the_division_shd_logo, resources.newTheme())
            logoSmall = resources.getDrawable(R.drawable.the_division_shd_logo_2, resources.newTheme())

            batteryRenderManager = BatteryRenderManager(batteryManager, resources, mainTypeface)

            // TODO: Mostly for debugging purposes.
            sensorManager.getSensorList(Sensor.TYPE_ALL).forEach(fun (v) {
                println("- ${v.name} -> ${v.id} / ${v.type}")
                v.type
            })

            heartRateSensor = VitalsServicesManager(
                applicationContext,
                sensorManager,
                resources,
                mainTypeface,
            )
            heartRateSensor.isFaceVisible = isVisible
            heartRateSensor.registerListener()

            initializeBackground()
            initializeWatchFace()
        }

        private fun initializeBackground() {
            mBackgroundPaint = Paint().apply {
                color = Color.BLACK
            }
        }

        private fun initializeWatchFace() {
            /* Set defaults for colors */
        }

        override fun onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            super.onDestroy()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            // Check and trigger whether or not timer should be running (only
            // in active mode).
            updateTimer()
        }

        override fun onInterruptionFilterChanged(interruptionFilter: Int) {
            super.onInterruptionFilterChanged(interruptionFilter)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

        }

        /**
         * Captures tap event (and tap type). The [WatchFaceService.TAP_TYPE_TAP] case can be
         * used for implementing specific logic to handle the gesture.
         */
        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {
            when (tapType) {
//                WatchFaceService.TAP_TYPE_TOUCH -> {
//                    // The user has started touching the screen.
//                }
//                WatchFaceService.TAP_TYPE_TOUCH_CANCEL -> {
//                    // The user has started a different gesture or otherwise cancelled the tap.
//                }
                TAP_TYPE_TAP -> {
                    // Text: R.string.message
                    Toast.makeText(applicationContext, "${x}x${y}", Toast.LENGTH_LONG)
                        .show()
                }
                else -> {
                    println(tapType)
                }
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            val now = System.currentTimeMillis()
            mCalendar.timeInMillis = now

            drawWatchFace(canvas)
        }

        private fun drawWatchFace(canvas: Canvas) {
            if (!isVisible) {
                return
            }

            /*
             * Save the canvas state before we can begin to rotate it.
             */
            canvas.save()

            val blackPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.FILL
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
                setShadowLayer(
                    20f, 0f, 0f, lighterOrangeColor
                )
            }

            val orangePaint = Paint().apply {
                color = orangeColor
                style = Paint.Style.FILL
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
            }

            val mainClockPaint = Paint().apply {
                color = Color.WHITE
                textSize = when { !useChakra -> 86f; else -> 68f }
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
                typeface = mainTypeface
            }

            val otherPaint = Paint().apply {
                color = orangeColor
                textSize = when { !useChakra -> 38f; else -> 28f }
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
                typeface = mainTypeface
            }

            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels

            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), blackPaint)

            canvas.drawCircle(
                width / 2f,
                height / 2f,
                width.toFloat(),
                orangePaint)

            // When an agent isn't deployed, the entire screen can be black and the logo
            // doesn't have to be drawn.
            canvas.drawCircle(
                width / 2f,
                height / 2f,
                width - (width / 2 + 16f),
                blackPaint)

            // Draw the logo.
            // logo.setBounds(width / 2 - 50, 100, width / 2 + 50, 200)
            val widthPart = (width * 0.33).toInt()
            val halfWidth = width / 2
            val halfHeight = height / 2

            logo.setBounds(
                halfWidth - widthPart,
                halfHeight - widthPart,
                halfWidth + widthPart,
                halfHeight + widthPart
            )
            logo.draw(canvas)

            val hourOfDay = "%02d".format(mCalendar.get(Calendar.HOUR_OF_DAY))
            val minutes = "%02d".format(mCalendar.get(Calendar.MINUTE))
            val seconds = "%02d".format(mCalendar.get(Calendar.SECOND))
            val timeStr = "$hourOfDay:$minutes:$seconds"

            val currentDate = DateHelper.getCurrentDate(mCalendar)

            val xSpreadConst = when {
                !useChakra -> 1f;
                else -> 0.861618f;
            }

            // Where the time label should be placed.
            val xPos = ((width / 2) - (38 * xSpreadConst * timeStr.length / 2)).toFloat()

            canvas.drawText(timeStr, xPos, height / 2f + 8, mainClockPaint)

            // Draw the battery icon.
            batteryRenderManager.drawBattery(canvas, width)

            // Draw the heart icon.
            heartRateSensor.draw(canvas, width)

            val xUPos = ((width / 2) - (15.8 * xSpreadConst * currentDate.length / 2)).toFloat()

            canvas.drawText(currentDate, xUPos, height / 2f + 48, otherPaint)

            // Restore the canvas" original orientation.
            canvas.restore()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            heartRateSensor.isFaceVisible = isVisible

            if (visible) {
                registerReceiver()
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
                // This should be run only if the previous event listener is unregistered when the
                // face is not visible.
                // heartRateSensor.registerListener()
            } else {
                unregisterReceiver()
            }

            // Check and trigger whether or not timer should be running (only in active mode).
            updateTimer()
        }

        private fun registerReceiver() {
        }

        private fun unregisterReceiver() {
        }

        /**
         * Starts/stops the [.mUpdateTimeHandler] timer based on the state of the watch face.
         */
        private fun updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME)
            }
        }

        /**
         * Returns whether the [.mUpdateTimeHandler] timer should be running. The timer
         * should only run in active mode.
         */
        private fun shouldTimerBeRunning(): Boolean {
            return isVisible
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        fun handleUpdateTimeMessage() {
            invalidate()
            if (shouldTimerBeRunning()) {
                val timeMs = System.currentTimeMillis()
                val delayMs = INTERACTIVE_UPDATE_RATE_MS - timeMs % INTERACTIVE_UPDATE_RATE_MS
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs)
            }
        }
    }
}