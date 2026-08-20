package com.example.core.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object MathUtils {

    private val priceFormat8 = DecimalFormat("#,##0.00000000")
    private val priceFormat4 = DecimalFormat("#,##0.0000")
    private val priceFormat2 = DecimalFormat("#,##0.00")
    private val volumeFormat2 = DecimalFormat("#,##0.00")
    private val integerFormat = DecimalFormat("#,##0")
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val shortTimeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun generateUniqueId(): String {
        return UUID.randomUUID().toString().substring(0, 8)
    }

    fun lerp(start: Float, stop: Float, fraction: Float): Float {
        return start + (stop - start) * fraction.coerceIn(0f, 1f)
    }

    fun lerpDouble(start: Double, stop: Double, fraction: Double): Double {
        return start + (stop - start) * fraction.coerceIn(0.0, 1.0)
    }

    fun exponentialDecay(current: Float, target: Float, decayRate: Float, dtSeconds: Float): Float {
        val factor = kotlin.math.exp(-decayRate * dtSeconds)
        return target + (current - target) * factor
    }

    fun exponentialDecayDouble(current: Double, target: Double, decayRate: Double, dtSeconds: Double): Double {
        val factor = kotlin.math.exp(-decayRate * dtSeconds)
        return target + (current - target) * factor
    }

    fun clamp(value: Double, minVal: Double, maxVal: Double): Double {
        return max(minVal, min(maxVal, value))
    }

    fun clampFloat(value: Float, minVal: Float, maxVal: Float): Float {
        return max(minVal, min(maxVal, value))
    }

    fun calculateMean(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        return values.sum() / values.size
    }

    fun calculateStdDev(values: List<Double>, mean: Double = calculateMean(values)): Double {
        if (values.size <= 1) return 0.0
        val variance = values.map { (it - mean).pow(2) }.sum() / (values.size - 1)
        return sqrt(variance)
    }

    fun calculateZScore(value: Double, mean: Double, stdDev: Double): Double {
        if (stdDev == 0.0) return 0.0
        return (value - mean) / stdDev
    }

    fun createLogarithmicThresholds(minVolume: Double, maxVolume: Double, numBuckets: Int): DoubleArray {
        val safeMin = max(0.0001, minVolume)
        val safeMax = max(safeMin * 2, maxVolume)
        val logMin = ln(safeMin)
        val logMax = ln(safeMax)
        val step = (logMax - logMin) / numBuckets

        val thresholds = DoubleArray(numBuckets + 1)
        for (i in 0..numBuckets) {
            thresholds[i] = kotlin.math.exp(logMin + i * step)
        }
        return thresholds
    }

    fun findBucketIndex(volume: Double, thresholds: DoubleArray): Int {
        val numBuckets = thresholds.size - 1
        if (numBuckets <= 0) return 0
        if (volume <= thresholds[0]) return 0
        if (volume >= thresholds[numBuckets]) return numBuckets - 1

        var low = 0
        var high = numBuckets - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (volume >= thresholds[mid] && volume < thresholds[mid + 1]) {
                return mid
            } else if (volume < thresholds[mid]) {
                high = mid - 1
            } else {
                low = mid + 1
            }
        }
        return (low - 1).coerceIn(0, numBuckets - 1)
    }

    fun formatPrice(price: Double): String {
        return when {
            price >= 1000.0 -> priceFormat2.format(price)
            price >= 1.0 -> priceFormat4.format(price)
            else -> priceFormat8.format(price)
        }
    }

    fun formatVolume(volume: Double): String {
        return when {
            volume >= 1_000_000.0 -> String.format(Locale.US, "%.2fM", volume / 1_000_000.0)
            volume >= 1_000.0 -> String.format(Locale.US, "%.2fK", volume / 1_000.0)
            volume >= 1.0 -> volumeFormat2.format(volume)
            else -> String.format(Locale.US, "%.4f", volume)
        }
    }

    fun formatUsd(amount: Double): String {
        return when {
            amount >= 1_000_000.0 -> String.format(Locale.US, "$%.2fM", amount / 1_000_000.0)
            amount >= 1_000.0 -> String.format(Locale.US, "$%.1fK", amount / 1_000.0)
            else -> String.format(Locale.US, "$%.2f", amount)
        }
    }

    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    fun formatShortTime(timestamp: Long): String {
        return shortTimeFormat.format(Date(timestamp))
    }
}
