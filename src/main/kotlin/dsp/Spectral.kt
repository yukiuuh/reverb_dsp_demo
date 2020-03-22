package dsp

import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class Spectral {

    var dataRe: DoubleArray
    var dataIm: DoubleArray
    operator fun get(i: Int): Pair<Double, Double> {
        if (i >= dataRe.size || i < 0) throw IllegalArgumentException("index error")
        return Pair(dataRe[i], dataIm[i])
    }

    operator fun set(i: Int, value: Pair<Double, Double>) {
        if (i >= dataRe.size || i < 0) throw IllegalArgumentException("index error")
        dataRe[i] = value.first
        dataIm[i] = value.second
    }

    constructor(size: Int) {
        dataRe = DoubleArray(size)
        dataIm = DoubleArray(size)
    }

    constructor(dsr: DoubleArray, dsi: DoubleArray) {
        dataRe = dsr
        dataIm = dsi
    }

    fun applyIDFT(): Spectral {
        var out = Spectral(dataRe.size)
        for (k in dataRe.indices) {
            for (n in dataRe.indices) {
                out.dataRe[k] += this.dataRe[n] * cos(2.0 * PI * n.toDouble() * k.toDouble() / dataRe.size) -
                        this.dataIm[n] * sin(2.0 * PI * n.toDouble() * k.toDouble() / dataRe.size)
                out.dataIm[k] += this.dataRe[n] * sin(2.0 * PI * n.toDouble() * k.toDouble() / dataRe.size) +
                        this.dataIm[n] * cos(2.0 * PI * n.toDouble() * k.toDouble() / dataRe.size)

            }
            out.dataRe[k] = out.dataRe[k] / dataRe.size
            out.dataIm[k] = out.dataIm[k] / dataIm.size

        }
        return out
    }

    fun applyIDFT2(): Signal {
        //imの計算を省略
        var out = Signal(dataRe.size)
        for (k in dataRe.indices) {
            for (n in dataRe.indices) {
                out.data[k] += this.dataRe[n] * cos(2.0 * PI * n.toDouble() * k.toDouble() / dataRe.size) -
                        this.dataIm[n] * sin(2.0 * PI * n.toDouble() * k.toDouble() / dataRe.size)


            }
            out.data[k] = out.data[k] / dataRe.size


        }
        return out
    }

    operator fun times(other: Spectral): Spectral {
        if (this.dataRe.size != other.dataRe.size) throw IllegalArgumentException("spectral length??")
        val out = Spectral(dataRe.size)
        val b = dataRe.size
        val loop = { i0: Int, i1: Int ->

            for (i in i0 until i1) {
                out.dataRe[i] = this.dataRe[i] * other.dataRe[i] - this.dataIm[i] * other.dataIm[i]
                out.dataIm[i] = this.dataRe[i] * other.dataIm[i] + this.dataIm[i] * other.dataRe[i]
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
        return out
    }

    fun complexMult(other: Spectral): Signal {
        if (this.dataRe.size != other.dataRe.size) throw IllegalArgumentException("spectral length??")
        val out = Signal(dataRe.size)

        for (i in dataRe.indices) {
            out.data[i] = this.dataRe[i] * other.dataRe[i] - this.dataIm[i] * other.dataIm[i]

        }


        return out
    }

    fun applyIFFT(): Spectral {
        //Stockham

        var a = 1

        val snum = dataRe.size
        var b = snum / 2

        var f = Spectral(dataRe.copyOf(), dataIm.copyOf())
        var f1 = Spectral(snum)


        while (a < snum) {
            val loop = { b0: Int, b1: Int ->
                for (n in b0 until b1) {
                    for (k in 0 until a) {
                        val real: Double
                        val imag: Double
                        //val real = f.dataRe[a*n + snum/2 + k] * et.getRe(b*k) + f.dataIm[a*n + snum/2 + k] *et.getIm(b*k)
                        //val imag = -f.dataRe[a*n + snum/2 + k] * et.getIm(b*k) + f.dataIm[a*n + snum/2 + k] * et.getRe(b*k)
                        if (dataRe.size == et.n) {
                            //println("use")
                            real =
                                f.dataRe[a * n + snum / 2 + k] * et.getRe(b * k) + f.dataIm[a * n + snum / 2 + k] * et.getIm(
                                    b * k
                                )
                            imag =
                                -f.dataRe[a * n + snum / 2 + k] * et.getIm(b * k) + f.dataIm[a * n + snum / 2 + k] * et.getRe(
                                    b * k
                                )
                        } else {
                            real =
                                f.dataRe[a * n + snum / 2 + k] * cos(-2.0 * PI * (b * k).toDouble() / snum.toDouble()) + f.dataIm[a * n + snum / 2 + k] * sin(
                                    -2.0 * PI * (b * k).toDouble() / snum.toDouble()
                                )
                            imag =
                                -f.dataRe[a * n + snum / 2 + k] * sin(-2.0 * PI * (b * k).toDouble() / snum.toDouble()) + f.dataIm[a * n + snum / 2 + k] * cos(
                                    -2.0 * PI * (b * k).toDouble() / snum.toDouble()
                                )
                        }
                        f1.dataRe[2 * a * n + k] = (f.dataRe[a * n + k] + real)
                        f1.dataIm[2 * a * n + k] = (f.dataIm[a * n + k] + imag)
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
        for (i in 0 until snum) {
            f1.dataRe[i] = f1.dataRe[i] / snum
            f1.dataIm[i] = f1.dataIm[i] / snum

        }

        return f1
    }

    fun applyIFFT2(): Spectral {
        //Stockham
        var a = 1

        val snum = dataRe.size
        var b = snum / 2

        val f = Spectral(dataRe.copyOf(), dataIm.copyOf())
        val f1 = Spectral(snum)


        while (a < snum) {
            for (n in 0 until b) {
                for (k in 0 until a) {
                    val real: Double
                    val imag: Double
                    //val real = f.dataRe[a*n + snum/2 + k] * et.getRe(b*k) + f.dataIm[a*n + snum/2 + k] *et.getIm(b*k)
                    //val imag = -f.dataRe[a*n + snum/2 + k] * et.getIm(b*k) + f.dataIm[a*n + snum/2 + k] * et.getRe(b*k)
                    if (dataRe.size == 1024) {
                        real =
                            f.dataRe[a * n + snum / 2 + k] * et.getRe(b * k) + f.dataIm[a * n + snum / 2 + k] * et.getIm(
                                b * k
                            )
                        imag =
                            -f.dataRe[a * n + snum / 2 + k] * et.getIm(b * k) + f.dataIm[a * n + snum / 2 + k] * et.getRe(
                                b * k
                            )
                    } else {
                        real =
                            f.dataRe[a * n + snum / 2 + k] * cos(-2.0 * PI * (b * k).toDouble() / snum.toDouble()) + f.dataIm[a * n + snum / 2 + k] * sin(
                                -2.0 * PI * (b * k).toDouble() / snum.toDouble()
                            )
                        imag =
                            -f.dataRe[a * n + snum / 2 + k] * sin(-2.0 * PI * (b * k).toDouble() / snum.toDouble()) + f.dataIm[a * n + snum / 2 + k] * cos(
                                -2.0 * PI * (b * k).toDouble() / snum.toDouble()
                            )
                    }
                    f1.dataRe[2 * a * n + k] = (f.dataRe[a * n + k] + real)
                    f1.dataIm[2 * a * n + k] = (f.dataIm[a * n + k] + imag)
                    f1.dataRe[2 * a * n + k + a] = f.dataRe[a * n + k] - real
                    f1.dataIm[2 * a * n + k + a] = f.dataIm[a * n + k] - imag

                }
            }
            a *= 2
            b /= 2
            for (i in 0 until snum) f[i] = f1[i]
        }
        for (i in 0 until snum) {
            f1.dataRe[i] = f1.dataRe[i] / snum
            f1.dataIm[i] = f1.dataIm[i] / snum

        }
        return f1
    }
}
/*
class SpectralF {

        var dataRe: FloatArray
        var dataIm: FloatArray
        operator fun get(i: Int): Pair<Float, Float> {
            if (i >= dataRe.size || i < 0) throw IllegalArgumentException("index error")
            return Pair(dataRe[i], dataIm[i])
        }

        operator fun set(i: Int, value: Pair<Float, Float>) {
            if (i >= dataRe.size || i < 0) throw IllegalArgumentException("index error")
            dataRe[i] = value.first
            dataIm[i] = value.second
        }

        constructor(size: Int) {
            dataRe = FloatArray(size)
            dataIm = FloatArray(size)
        }

        constructor(dsr: FloatArray, dsi: FloatArray) {
            dataRe = dsr
            dataIm = dsi
        }


        operator fun times(other: SpectralF): SpectralF {
            if (this.dataRe.size != other.dataRe.size) throw IllegalArgumentException("spectral length??")
            val out = SpectralF(dataRe.size)
            val b = dataRe.size
            val loop = { i0: Int, i1: Int ->

                for (i in i0 until i1) {
                    out.dataRe[i] = this.dataRe[i] * other.dataRe[i] - this.dataIm[i] * other.dataIm[i]
                    out.dataIm[i] = this.dataRe[i] * other.dataIm[i] + this.dataIm[i] * other.dataRe[i]
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
            return out
        }



        fun applyIFFT(): SpectralF {
            //Stockham
            var a = 1

            val snum = dataRe.size
            var b = snum / 2

            val f = SpectralF(dataRe.copyOf(), dataIm.copyOf())
            val f1 = SpectralF(snum)


            while (a < snum) {
                val loop = { b0: Int, b1: Int ->
                    for (n in b0 until b1) {
                        for (k in 0 until a) {
                            val real: Float
                            val imag: Float
                            //val real = f.dataRe[a*n + snum/2 + k] * et.getRe(b*k) + f.dataIm[a*n + snum/2 + k] *et.getIm(b*k)
                            //val imag = -f.dataRe[a*n + snum/2 + k] * et.getIm(b*k) + f.dataIm[a*n + snum/2 + k] * et.getRe(b*k)
                            if (dataRe.size == 2048) {
                                //println("use")
                                real = f.dataRe[a * n + snum / 2 + k] * etf.getRe(b * k) + f.dataIm[a * n + snum / 2 + k] * etf.getIm(b * k)
                                imag = -f.dataRe[a * n + snum / 2 + k] * etf.getIm(b * k) + f.dataIm[a * n + snum / 2 + k] * etf.getRe(b * k)
                            } else {
                                real = f.dataRe[a * n + snum / 2 + k] * cos(-2.0f * PI.toFloat() * (b * k).toFloat() / snum.toFloat()) + f.dataIm[a * n + snum / 2 + k] * sin(-2.0f * PI.toFloat() * (b * k).toFloat() / snum.toFloat())
                                imag = -f.dataRe[a * n + snum / 2 + k] * sin(-2.0f * PI.toFloat() * (b * k).toFloat() / snum.toFloat()) + f.dataIm[a * n + snum / 2 + k] * cos(-2.0f * PI.toFloat() * (b * k).toFloat() / snum.toFloat())
                            }
                            f1.dataRe[2 * a * n + k] = (f.dataRe[a * n + k] + real)
                            f1.dataIm[2 * a * n + k] = (f.dataIm[a * n + k] + imag)
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
                for (i in 0 until snum) f[i] = f1[i]
            }
            for (i in 0 until snum) {
                f1.dataRe[i] = f1.dataRe[i] / snum
                f1.dataIm[i] = f1.dataIm[i] / snum

            }
            return f1
        }

        fun applyIFFT2(): SpectralF {
            //Stockham
            var a = 1

            val snum = dataRe.size
            var b = snum / 2

            val f = SpectralF(dataRe.copyOf(), dataIm.copyOf())
            val f1 = SpectralF(snum)


            while (a < snum) {
                for (n in 0 until b) {
                    for (k in 0 until a) {
                        val real: Float
                        val imag: Float
                        //val real = f.dataRe[a*n + snum/2 + k] * et.getRe(b*k) + f.dataIm[a*n + snum/2 + k] *et.getIm(b*k)
                        //val imag = -f.dataRe[a*n + snum/2 + k] * et.getIm(b*k) + f.dataIm[a*n + snum/2 + k] * et.getRe(b*k)
                        if (dataRe.size == 1024) {
                            real = f.dataRe[a * n + snum / 2 + k] * et.getRe(b * k) + f.dataIm[a * n + snum / 2 + k] * et.getIm(b * k)
                            imag = -f.dataRe[a * n + snum / 2 + k] * et.getIm(b * k) + f.dataIm[a * n + snum / 2 + k] * et.getRe(b * k)
                        } else {
                            real = f.dataRe[a * n + snum / 2 + k] * cos(-2.0 * PI * (b * k).toFloat() / snum.toFloat()) + f.dataIm[a * n + snum / 2 + k] * sin(-2.0 * PI * (b * k).toFloat() / snum.toFloat())
                            imag = -f.dataRe[a * n + snum / 2 + k] * sin(-2.0 * PI * (b * k).toFloat() / snum.toFloat()) + f.dataIm[a * n + snum / 2 + k] * cos(-2.0 * PI * (b * k).toFloat() / snum.toFloat())
                        }
                        f1.dataRe[2 * a * n + k] = (f.dataRe[a * n + k] + real)
                        f1.dataIm[2 * a * n + k] = (f.dataIm[a * n + k] + imag)
                        f1.dataRe[2 * a * n + k + a] = f.dataRe[a * n + k] - real
                        f1.dataIm[2 * a * n + k + a] = f.dataIm[a * n + k] - imag

                    }
                }
                a *= 2
                b /= 2
                for (i in 0 until snum) f[i] = f1[i]
            }
            for (i in 0 until snum) {
                f1.dataRe[i] = f1.dataRe[i] / snum
                f1.dataIm[i] = f1.dataIm[i] / snum

            }
            return f1
        }
    }

        */