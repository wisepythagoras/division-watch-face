package com.example.shdhome

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.BatteryManager

class BatteryRenderManager constructor(
    batteryManager: BatteryManager,
    resources: Resources,
    typeface: Typeface
) {
    private val mTypeface: Typeface = typeface
    private val mBatteryManager: BatteryManager = batteryManager
    private val batteryEmpty: Drawable =
        resources.getDrawable(R.drawable.battery, resources.newTheme())
    private val orange: Int = Color.parseColor("#ff6d10")
    private val orangePaint: Paint = Paint().apply {
        color = orange
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    /**
     * Draw the battery image and indicator.
     */
    public fun drawBattery(canvas: Canvas, width: Int) {
        val level = mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val halfWidth = width / 2
        val batteryText = "${level}%"
        val xPos = ((width / 2) - (12 * batteryText.length / 2)).toFloat()
        val textPaint = Paint().apply {
            color = orange
            textSize = 24f
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            typeface = mTypeface
        }

        val batteryIcon = batteryEmpty

        // X goes from xPos + 12 to xPos + 32.
        val levelXStart = width / 2f - 12f
        val levelXOffset = levelXStart + (20 * level / 100)

        canvas.drawRect(levelXStart, 38f, levelXOffset, 46f, orangePaint)

        batteryIcon.setBounds(
            halfWidth - 16,
            26,
            halfWidth + 16,
            58
        )
        batteryIcon.draw(canvas)

        canvas.drawText(batteryText, xPos, 68f, textPaint)
    }
}