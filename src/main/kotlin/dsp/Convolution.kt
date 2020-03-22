package dsp

import com.aparapi.Kernel
import com.aparapi.Range


class TestKernel(val size: Int) : Kernel() {

    var real = FloatArray(size)
    var imag = FloatArray(size)
    var resultR = FloatArray(size)
    var resultI = FloatArray(size)

    init {
        executionMode = Kernel.EXECUTION_MODE.GPU

    }

    fun calc(a: FloatArray, b: FloatArray) {
        for (i in 0 until size) {
            real[i] = a[i]
            imag[i] = b[i]
        }
        this.execute(Range.create(size))

    }

    override fun run() {
        val i = globalId


    }

}

val cmk = ComplexMultiKernel(2048)

class ComplexMultiKernel(val size: Int) : Kernel() {

    var r1 = FloatArray(size)
    var i1 = FloatArray(size)
    var r2 = FloatArray(size)
    var i2 = FloatArray(size)
    var resultR = FloatArray(size)
    var resultI = FloatArray(size)

    init {
        executionMode = Kernel.EXECUTION_MODE.GPU

    }

    fun calc(ar: FloatArray, ai: FloatArray, br: FloatArray, bi: FloatArray) {
        for (i in 0 until size) {
            r1[i] = ar[i]
            i1[i] = ai[i]
            r2[i] = br[i]
            i2[i] = bi[i]
        }
        this.execute(Range.create(size))

    }
/*
    fun calc(s1: Spectral, s2: Spectral):SpectralF {

        for (i in 0 until size) {
            r1[i] = s1.dataRe[i].toFloat()
            i1[i] = s1.dataIm[i].toFloat()
            r2[i] = s2.dataRe[i].toFloat()
            i2[i] = s2.dataIm[i].toFloat()
        }
        this.execute(Range.create(size))
        return SpectralF(resultR,resultI)
    }*/

    override fun run() {
        val i = globalId
        resultR[i] = r1[i] * r2[i] - i1[i] * i2[i]
        resultI[i] = r1[i] * i2[i] + r2[i] * i1[i]
    }

}

class ComplexSumKernel(val size: Int) : Kernel() {

    var reals = Array(1, { FloatArray(size) })
    var imags = Array(1, { FloatArray(size) })

    var resultR = FloatArray(size)
    var resultI = FloatArray(size)

    init {
        executionMode = Kernel.EXECUTION_MODE.GPU

    }


    fun calc(vararg ss: Spectral) {
        reals = Array(ss.size, { FloatArray(size) })
        imags = Array(ss.size, { FloatArray(size) })
        for (i in 0 until ss.size) {
            for (j in 0 until this.size) {
                reals[i][j] = ss[i].dataRe[j].toFloat()
                imags[i][j] = ss[i].dataIm[i].toFloat()

            }
        }
        this.execute(Range.create(size))

    }

    fun calc(vararg ss: DoubleArray): Pair<FloatArray, FloatArray> {

        reals = Array(ss.size, { FloatArray(size) })
        imags = Array(ss.size, { FloatArray(size) })
        for (i in 0 until ss.size) {
            for (j in 0 until this.size) {
                reals[i][j] = ss[i][j].toFloat()
                imags[i][j] = ss[i][j].toFloat()

            }
        }
        this.execute(Range.create(size))
        return Pair(resultR, resultI)
    }

    override fun run() {
        val i = globalId
        resultR[i] = 0f
        resultI[i] = 0f
        for (j in 0 until reals.size) {
            resultR[i] += reals[j][i]
            resultI[i] += imags[j][i]
        }
    }

}

class RealSumKernel(val size: Int) : Kernel() {

    var reals = Array(1, { FloatArray(size) })
    //var imags = Array(1,{FloatArray(size)})

    var resultR = FloatArray(size)
    //var resultI = FloatArray(size)

    init {
        executionMode = Kernel.EXECUTION_MODE.GPU

    }


    fun calc(vararg ss: Spectral): FloatArray {
        reals = Array(ss.size, { FloatArray(size) })
        //imags = Array(ss.size,{FloatArray(size)})
        for (i in 0 until ss.size) {
            for (j in 0 until this.size) {
                reals[i][j] = ss[i].dataRe[j].toFloat()
                // imags[i][j] = ss[i].dataIm[i].toFloat()

            }
        }
        this.execute(Range.create(size))
        return resultR

    }

    fun calc(vararg ss: DoubleArray): FloatArray {

        reals = Array(ss.size, { FloatArray(size) })
        //imags = Array(ss.size,{FloatArray(size)})
        for (i in 0 until ss.size) {
            for (j in 0 until this.size) {
                reals[i][j] = ss[i][j].toFloat()
                //imags[i][j] = ss[i][j].toFloat()

            }
        }
        this.execute(Range.create(size))
        return resultR
    }

    override fun run() {
        val i = globalId
        resultR[i] = 0f
        //resultI[i] = 0f
        for (j in 0 until reals.size) {
            resultR[i] += reals[j][i]
            //resultI[i] += imags[j][i]
        }
    }

}