package com.japaneixxx.freya.ui

import com.japaneixxx.freya.web.BaseMath
import com.japaneixxx.freya.web.WebBridge

object System {
    fun currentTimeMillis(): Long = WebBridge.nowMillis()
    fun nanoTime(): Long = WebBridge.nowMillis() * 1_000_000L
}

open class SecurityException(message: String? = null) : Exception(message)

object Math {
    fun round(a: Double): Long = BaseMath.round(a)
    fun round(a: Float): Int = BaseMath.round(a)
    fun abs(a: Double): Double = BaseMath.abs(a)
    fun abs(a: Float): Float = BaseMath.abs(a)
    fun abs(a: Int): Int = BaseMath.abs(a)
    fun abs(a: Long): Long = BaseMath.abs(a)
    fun ceil(a: Double): Double = BaseMath.ceil(a)
    fun floor(a: Double): Double = BaseMath.floor(a)
    fun pow(a: Double, b: Double): Double = BaseMath.pow(a, b)
    fun sqrt(a: Double): Double = BaseMath.sqrt(a)
    fun max(a: Int, b: Int): Int = if (a > b) a else b
    fun max(a: Long, b: Long): Long = if (a > b) a else b
    fun max(a: Double, b: Double): Double = BaseMath.max(a, b)
    fun min(a: Int, b: Int): Int = if (a < b) a else b
    fun min(a: Long, b: Long): Long = if (a < b) a else b
    fun min(a: Double, b: Double): Double = BaseMath.min(a, b)
    fun toDegrees(a: Double): Double = BaseMath.toDegrees(a)
}
