package com.JohanesAndika.ingatservismotor

import java.util.concurrent.TimeUnit

object CalculationUtils {
    fun calculateDailyAverageSMA(weeklyDistances: List<Int>, period: Int): Int {
        if (weeklyDistances.isEmpty()) return 0
        
        val recentData = if (weeklyDistances.size > period) {
            weeklyDistances.takeLast(period)
        } else {
            weeklyDistances
        }
        
        val averageWeekly = recentData.average()
        return (averageWeekly / 7).toInt().coerceAtLeast(0)
    }

    fun calculateAverageDailyKm(newOdo: Long, oldOdo: Long, lastTimeMillis: Long): Int {
        val diffOdo = (newOdo - oldOdo).coerceAtLeast(0L)
        val diffTime = (System.currentTimeMillis() - lastTimeMillis).coerceAtLeast(1L)
        val days = TimeUnit.MILLISECONDS.toDays(diffTime).coerceAtLeast(1L)
        return (diffOdo / days).toInt().coerceAtLeast(0)
    }
}