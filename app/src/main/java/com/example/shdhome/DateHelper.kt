package com.example.shdhome

import java.util.Calendar

object DateHelper {
    fun getCurrentDate(calendar: Calendar): String {
        val dayOfWeek = getDayOfWeekString(calendar.get(Calendar.DAY_OF_WEEK))
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val dayField = "${"%02d".format(dayOfMonth)}${getDayOfMonthEnd(dayOfMonth)}"
        val month = getMonthString(calendar.get(Calendar.MONTH))

        return "$dayOfWeek, $month $dayField"
    }

    fun getDayOfMonthEnd(dayOfMonth: Int): String {
        return when (dayOfMonth) {
            1, 21, 31 -> "st"
            2, 22 -> "nd"
            3, 23 -> "rd"
            else -> "th"
        }
    }

    fun getMonthString(month: Int): String {
        return when (month) {
            Calendar.JANUARY -> "Jan"
            Calendar.FEBRUARY -> "Feb"
            Calendar.MARCH -> "Mar"
            Calendar.APRIL -> "Apr"
            Calendar.MAY -> "May"
            Calendar.JUNE -> "Jun"
            Calendar.JULY -> "Jul"
            Calendar.AUGUST -> "Aug"
            Calendar.SEPTEMBER -> "Sep"
            Calendar.OCTOBER -> "Oct"
            Calendar.NOVEMBER -> "Nov"
            Calendar.DECEMBER -> "Dec"
            else -> ""
        }
    }

    fun getDayOfWeekString(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            Calendar.SUNDAY -> "Sun"
            else -> ""
        }
    }
}