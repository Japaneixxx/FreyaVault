package com.japaneixxx.freya.web

object BaseMath {
    fun round(a: Double): Long = kotlin.math.floor(a + 0.5).toLong()
    fun round(a: Float): Int = kotlin.math.floor(a.toDouble() + 0.5).toInt()
    fun abs(a: Double): Double = if (a < 0) -a else a
    fun abs(a: Float): Float = if (a < 0) -a else a
    fun abs(a: Int): Int = if (a < 0) -a else a
    fun abs(a: Long): Long = if (a < 0) -a else a
    fun ceil(a: Double): Double = kotlin.math.ceil(a)
    fun floor(a: Double): Double = kotlin.math.floor(a)
    fun pow(a: Double, b: Double): Double = WebBridge.pow(a, b)
    fun sqrt(a: Double): Double = kotlin.math.sqrt(a)
    fun max(a: Double, b: Double): Double = if (a > b) a else b
    fun min(a: Double, b: Double): Double = if (a < b) a else b
    fun toDegrees(a: Double): Double = a * 180.0 / kotlin.math.PI
}
