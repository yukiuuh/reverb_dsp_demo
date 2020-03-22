package dsp.util

import kotlin.math.PI
import kotlin.math.cos

fun hamWindow(f: DoubleArray): DoubleArray {
    return f.mapIndexed { i, x -> x * (0.54 - 0.46 * cos(2.0 * PI * i.toDouble() / f.size.toDouble())) }.toDoubleArray()
}

fun hamWindow(f: IntArray): DoubleArray {
    return f.mapIndexed { i, x -> x * (0.54 - 0.46 * cos(2.0 * PI * i.toDouble() / f.size.toDouble())) }.toDoubleArray()
}