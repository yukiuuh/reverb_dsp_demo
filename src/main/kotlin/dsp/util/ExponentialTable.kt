package dsp.util

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class ExponentialTable(val n: Int = 1024) {
    val re = DoubleArray(n)
    val im = DoubleArray(n)

    init {
        for (i in 0 until n) {
            re[i] = cos(-2.0 * PI * i.toDouble() / n.toDouble())
            im[i] = sin(-2.0 * PI * i.toDouble() / n.toDouble())
        }
    }

    fun getRe(i: Int): Double {
        return re[i]
    }

    fun getIm(i: Int): Double {
        return im[i]
    }

}

class ExponentialTableF(val n: Int = 1024) {
    val re = FloatArray(n)
    val im = FloatArray(n)

    init {
        for (i in 0 until n) {
            re[i] = cos(-2.0f * PI.toFloat() * i.toFloat() / n.toFloat())
            im[i] = sin(-2.0f * PI.toFloat() * i.toFloat() / n.toFloat())
        }
    }

    fun getRe(i: Int): Float {
        return re[i]
    }

    fun getIm(i: Int): Float {
        return im[i]
    }

}