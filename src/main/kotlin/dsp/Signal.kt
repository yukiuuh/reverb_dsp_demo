package dsp

import dsp.util.ExponentialTable
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


val et = ExponentialTable(8192)

//val etf= ExponentialTableF(8192)
class Signal {
    var data: DoubleArray
    fun isPowerOfTwo(i: Int): Boolean {
        var x = i
        while (x != 2) {
            if (x % 2 != 0) {
                println("this is not power of two")
                return false
            }
            x /= 2
        }
        return true
    }

    operator fun get(i: Int): Double {
        if (i >= data.size || i < 0) throw IllegalArgumentException("index error")
        return data[i]
    }

    operator fun set(i: Int, value: Double) {
        if (i >= data.size || i < 0) throw IllegalArgumentException("index error")
        data[i] = value
    }

    constructor(size: Int) {
        data = DoubleArray(size)
    }

    constructor(ds: DoubleArray) {
        if (isPowerOfTwo(ds.size)) {
            data = ds
        } else {
            var x = 2
            while (ds.size > x) {
                x *= 2
            }
            data = DoubleArray(x)
            for (i in ds.indices) {
                data[i] = ds[i]
            }
            println("${data.size} samples")
        }
    }

    constructor(ds: DoubleArray, isIR: Boolean) {
        if (!isIR) Signal(ds)
        if (isPowerOfTwo(ds.size)) {

            data = DoubleArray(ds.size * 2)
            for (i in ds.indices) {
                data[ds.size + i] = ds[i]
            }
        } else {
            var x = 2
            while (ds.size > x) {
                x *= 2
            }
            data = DoubleArray(x * 2)
            for (i in ds.indices) {
                data[x + i] = ds[i]
            }
            println("${data.size} samples as IR")
        }
    }

    constructor(ins: IntArray) {
        data = ins.map { it -> it.toDouble() }.toDoubleArray()
    }

    init {

    }

    fun applyDFT(): Spectral {
        var out = Spectral(data.size)
        for (k in data.indices) {
            for (n in data.indices) {
                out.dataRe[k] += data[n] * cos(2.0 * PI * n.toDouble() * k.toDouble() / data.size)
                out.dataIm[k] += -data[n] * sin(2.0 * PI * n.toDouble() * k.toDouble() / data.size)

            }

        }
        return out
    }

    fun applyFFT2(): Spectral {
        //Stockham
        var a = 1

        val snum = data.size
        var b = snum / 2

        val f = Spectral(data.copyOf(), DoubleArray(snum, { 0.0 }))
        val f1 = Spectral(snum)


        while (a < snum) {
            for (n in 0 until b) {
                for (k in 0 until a) {
                    val real: Double
                    val imag: Double
                    if (data.size == 1024) {
                        real =
                            f.dataRe[a * n + snum / 2 + k] * et.getRe(b * k) - f.dataIm[a * n + snum / 2 + k] * et.getIm(
                                b * k
                            )
                        imag =
                            f.dataRe[a * n + snum / 2 + k] * et.getIm(b * k) + f.dataIm[a * n + snum / 2 + k] * et.getRe(
                                b * k
                            )
                    } else {
                        real =
                            f.dataRe[a * n + snum / 2 + k] * cos(-2.0 * PI * (b * k).toDouble() / snum.toDouble()) - f.dataIm[a * n + snum / 2 + k] * sin(
                                -2.0 * PI * (b * k).toDouble() / snum.toDouble()
                            )
                        imag =
                            f.dataRe[a * n + snum / 2 + k] * sin(-2.0 * PI * (b * k).toDouble() / snum.toDouble()) + f.dataIm[a * n + snum / 2 + k] * cos(
                                -2.0 * PI * (b * k).toDouble() / snum.toDouble()
                            )
                    }
                    f1.dataRe[2 * a * n + k] = f.dataRe[a * n + k] + real
                    f1.dataIm[2 * a * n + k] = f.dataIm[a * n + k] + imag
                    f1.dataRe[2 * a * n + k + a] = f.dataRe[a * n + k] - real
                    f1.dataIm[2 * a * n + k + a] = f.dataIm[a * n + k] - imag

                }
            }
            a *= 2
            b /= 2
            for (i in 0 until snum) f[i] = f1[i]
        }
        return f1
    }

    fun applyFFT(): Spectral {
        //Stockham
        var a = 1

        val snum = data.size
        var b = snum / 2

        var f = Spectral(data.copyOf(), DoubleArray(snum, { 0.0 }))
        var f1 = Spectral(snum)


        while (a < snum) {
            val loop = { b0: Int, b1: Int ->
                for (n in b0 until b1) {

                    for (k in 0 until a) {

                        val real: Double
                        val imag: Double
                        if (data.size == et.n) {

                            real =
                                f.dataRe[a * n + snum / 2 + k] * et.getRe(b * k) - f.dataIm[a * n + snum / 2 + k] * et.getIm(
                                    b * k
                                )
                            imag =
                                f.dataRe[a * n + snum / 2 + k] * et.getIm(b * k) + f.dataIm[a * n + snum / 2 + k] * et.getRe(
                                    b * k
                                )
                        } else {
                            real =
                                f.dataRe[a * n + snum / 2 + k] * cos(-2.0 * PI * (b * k).toDouble() / snum.toDouble()) - f.dataIm[a * n + snum / 2 + k] * sin(
                                    -2.0 * PI * (b * k).toDouble() / snum.toDouble()
                                )
                            imag =
                                f.dataRe[a * n + snum / 2 + k] * sin(-2.0 * PI * (b * k).toDouble() / snum.toDouble()) + f.dataIm[a * n + snum / 2 + k] * cos(
                                    -2.0 * PI * (b * k).toDouble() / snum.toDouble()
                                )
                        }
                        f1.dataRe[2 * a * n + k] = f.dataRe[a * n + k] + real
                        f1.dataIm[2 * a * n + k] = f.dataIm[a * n + k] + imag
                        f1.dataRe[2 * a * n + k + a] = f.dataRe[a * n + k] - real
                        f1.dataIm[2 * a * n + k + a] = f.dataIm[a * n + k] - imag

                    }
                }

            }
            val latch = CountDownLatch(4)
            thread {
                loop(0, b / 4)
                latch.countDown()
            }
            thread {
                loop(b / 4, b / 2)
                latch.countDown()
            }
            thread {
                loop(b / 2, b * 3 / 4)
                latch.countDown()
            }
            thread {
                loop(b * 3 / 4, b)
                latch.countDown()
            }
            latch.await()
            a *= 2
            b /= 2
            //for (i in 0 until snum) f[i] = f1[i]
            val ft = f
            f = f1
            f1 = ft
        }


        return f1
    }

}