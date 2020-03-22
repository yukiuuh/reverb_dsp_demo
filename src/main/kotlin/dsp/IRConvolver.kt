package dsp

import dsp.util.*
import java.io.File
import javax.sound.sampled.AudioFormat

class IRConvolver2(val partSize: Int = 128, val format: AudioFormat = defaultFormat) {
    var ir1s = Array(1, { Spectral(partSize * 2) })
    var ir2s = Array(1, { Spectral(partSize * 2) })
    var sig1tBuf = Array(2, { Signal(partSize * 2) })
    var sig2tBuf = Array(2, { Signal(partSize * 2) })
    var sigtBufNum = 0 // 0 or 1
    var sig1s = Array(1, { Spectral(partSize * 2) })
    var sig2s = Array(1, { Spectral(partSize * 2) })
    var signalNumber = 0 // 0 until sig1s.size   0,1, .. ,sig1s.size-1,0,1, ..

    //lateinit var irs1 :Spectral
    //lateinit var irs2 :Spectral
    fun setIR(file: File) {
        val dsIR = getDoubleArrayFromBytes(SoundIO().loadFromFile(file))
        val gain: Double = 1.0 / Short.MAX_VALUE.toDouble()
        val dsIR1 = dsIR.first.map { it * gain }.toDoubleArray() //normalize
        val dsIR2 = dsIR.second.map { it * gain }.toDoubleArray() //normalize

        val bufNum = dsIR1.size / partSize
        ir1s = Array(bufNum + 1, { Spectral(partSize * 2) })
        ir2s = Array(bufNum + 1, { Spectral(partSize * 2) })
        sig1s = Array(bufNum + 1, { Spectral(partSize * 2) })
        sig2s = Array(bufNum + 1, { Spectral(partSize * 2) })
        println(bufNum)
        for (i in 0..bufNum) {
            val i0 = i * partSize
            val i1 = if ((i + 1) * partSize < dsIR1.size) (i + 1) * partSize else dsIR1.size
            val rBuf = DoubleArray(partSize)
            for (j in 0 until i1 - i0) {
                rBuf[j] = dsIR1.sliceArray(i0 until i1)[j]
            }
            ir1s[i] = Signal(rBuf, true).applyFFT()
            for (j in 0 until i1 - i0) {
                rBuf[j] = dsIR2.sliceArray(i0 until i1)[j]
            }
            ir2s[i] = Signal(rBuf, true).applyFFT()
        }
        println("IR loaded...")
    }

    fun saveInput(bin: ByteArray) {
        //bin.size = partSize
        if (bin.size / format.frameSize != partSize) IllegalArgumentException("bin size?")
        val din = getDoubleArrayFromBytes(bin)
        val din1 = din.first
        val din2 = din.second
        val i1: Int
        when (signalNumber) {
            sig1s.size - 1 -> {
                i1 = sig1s.size - 1

                signalNumber = 0
            }
            else -> {
                i1 = signalNumber

                signalNumber++
            }
        }
        val j0: Int
        val j1: Int
        when (sigtBufNum) {
            0 -> {
                j0 = 1
                j1 = 0
                sigtBufNum = 1
            }
            else -> {
                j0 = 0
                j1 = 1
                sigtBufNum = 0
            }
        }
        for (i in 0 until partSize) {
            sig1tBuf[j1][i] = sig1tBuf[j0][i + partSize]
            sig1tBuf[j1][i + partSize] = din1[i]
            sig2tBuf[j1][i] = sig2tBuf[j0][i + partSize]
            sig2tBuf[j1][i + partSize] = din2[i]
        }
        sig1s[i1] = sig1tBuf[j1].applyFFT()
        sig2s[i1] = sig2tBuf[j1].applyFFT()
    }

    fun sigNumPrev(i: Int): Int {
        return when (i) {
            0 -> sig1s.size - 1
            else -> i - 1
        }
    }

    fun sigNumNext(i: Int): Int {
        return when (i) {
            sig1s.size - 1 -> 0
            else -> i + 1
        }
    }

    fun apply(bin: ByteArray): ByteArray {
        val bout: ByteArray
        val out1: DoubleArray
        val out2: DoubleArray
        val out1Buf = Array(sig1s.size, { DoubleArray(partSize) })
        val out2Buf = Array(sig2s.size, { DoubleArray(partSize) })
        saveInput(bin)
        var j = signalNumber
        for (i in 0 until sig1s.size) {
            j = sigNumPrev(j)
            //println("${sig1s[j].dataRe.size},${ir1s[i].dataRe.size}")
            val start = System.currentTimeMillis()
            val ss1 = (sig1s[j] * ir1s[i]).applyIFFT()
            val ss2 = (sig2s[j] * ir2s[i]).applyIFFT()
            for (k in 0 until partSize) {
                (out1Buf[i])[k] = ss1.dataRe[k]
                (out2Buf[i])[k] = ss2.dataRe[k]
            }
            println("aa")

        }

        out1 = sumArray(*out1Buf)
        out2 = sumArray(*out2Buf)
        //out1 = DoubleArray(partSize)
        //out2 = DoubleArray(partSize)

        bout = getBytesFromIntArray(out1.map { it.toInt() }.toIntArray(), out2.map { it.toInt() }.toIntArray())

        return bout
    }
}

class IRConvolver(val partSize: Int = 128, val format: AudioFormat = defaultFormat) {
    private fun isPowerOfTwo(i: Int): Boolean {
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

    var ir1sr = Array(1, { DoubleArray(partSize * 2) })
    var ir1si = Array(1, { DoubleArray(partSize * 2) })
    var ir2sr = Array(1, { DoubleArray(partSize * 2) })
    var ir2si = Array(1, { DoubleArray(partSize * 2) })
    var sig1tBuf = Array(2, { DoubleArray(partSize * 2) })
    var sig2tBuf = Array(2, { DoubleArray(partSize * 2) })
    var sigtBufNum = 0 // 0 or 1
    var sig1sr = Array(1, { DoubleArray(partSize * 2) })
    var sig1si = Array(1, { DoubleArray(partSize * 2) })
    var sig2sr = Array(1, { DoubleArray(partSize * 2) })
    var sig2si = Array(1, { DoubleArray(partSize * 2) })
    var signalNumber = 0 // 0 until sig1s.size   0,1, .. ,sig1s.size-1,0,1, ..
    val fft = FFT(partSize * 2)
    val ifft = IFFT(partSize * 2)
    var mixer = Mixer(partSize, 1)
    var cm = ComplexMulti(partSize * 2)

    fun setIR(file: File) {
        val dsIR = getDoubleArrayFromBytes(SoundIO().loadFromFile(file))
        val bufNum = dsIR.first.size / partSize
        val gain: Double = 1.0 / 10 / Short.MAX_VALUE.toDouble()

        val dsIR1 = dsIR.first.map { it * gain }.toDoubleArray() //normalize
        val dsIR2 = dsIR.second.map { it * gain }.toDoubleArray() //normalize


        mixer = Mixer(partSize, bufNum + 1)
        ir1sr = Array(bufNum + 1, { DoubleArray(partSize * 2) })
        ir1si = Array(bufNum + 1, { DoubleArray(partSize * 2) })
        ir2sr = Array(bufNum + 1, { DoubleArray(partSize * 2) })
        ir2si = Array(bufNum + 1, { DoubleArray(partSize * 2) })
        sig1sr = Array(bufNum + 1, { DoubleArray(partSize * 2) })
        sig1si = Array(bufNum + 1, { DoubleArray(partSize * 2) })
        sig2sr = Array(bufNum + 1, { DoubleArray(partSize * 2) })
        sig2si = Array(bufNum + 1, { DoubleArray(partSize * 2) })
        println(bufNum)
        for (i in 0..bufNum) {
            val i0 = i * partSize
            val i1 = if ((i + 1) * partSize < dsIR1.size) (i + 1) * partSize else dsIR1.size
            val rBuf = DoubleArray(partSize)
            for (j in 0 until i1 - i0) {
                rBuf[j] = dsIR1.sliceArray(i0 until i1)[j]
            }
            val t1 = fft.execute(DoubleArray(partSize * 2, {
                if (it < partSize) {
                    0.0
                } else {
                    rBuf[it - partSize]
                }
            }))
            ir1sr[i] = t1.first
            ir1si[i] = t1.second

            for (j in 0 until i1 - i0) {
                rBuf[j] = dsIR2.sliceArray(i0 until i1)[j]
            }
            val t2 = fft.execute(DoubleArray(partSize * 2, {
                if (it < partSize) {
                    0.0
                } else {
                    rBuf[it - partSize]
                }
            }))
            ir2sr[i] = t2.first
            ir2si[i] = t2.second

        }
        println("IR loaded...")
    }

    fun saveInput(bin: ByteArray) {
        //bin.size = partSize
        if (bin.size / format.frameSize != partSize) IllegalArgumentException("bin size?")
        val din = getDoubleArrayFromBytes(bin)
        val din1 = din.first
        val din2 = din.second
        val i1: Int
        when (signalNumber) {
            sig1sr.size - 1 -> {
                i1 = sig1sr.size - 1

                signalNumber = 0
            }
            else -> {
                i1 = signalNumber

                signalNumber++
            }
        }
        val j0: Int
        val j1: Int
        when (sigtBufNum) {
            0 -> {
                j0 = 1
                j1 = 0
                sigtBufNum = 1
            }
            else -> {
                j0 = 0
                j1 = 1
                sigtBufNum = 0
            }
        }
        for (i in 0 until partSize) {
            sig1tBuf[j1][i] = sig1tBuf[j0][i + partSize]
            sig1tBuf[j1][i + partSize] = din1[i]
            sig2tBuf[j1][i] = sig2tBuf[j0][i + partSize]
            sig2tBuf[j1][i + partSize] = din2[i]
        }
        var t = fft.execute(sig1tBuf[j1])
        sig1sr[i1] = t.first
        sig1si[i1] = t.second
        t = fft.execute(sig2tBuf[j1])
        sig2sr[i1] = t.first
        sig2si[i1] = t.second
    }

    fun sigNumPrev(i: Int): Int {
        return when (i) {
            0 -> sig1sr.size - 1
            else -> i - 1
        }
    }

    fun sigNumNext(i: Int): Int {
        return when (i) {
            sig1sr.size - 1 -> 0
            else -> i + 1
        }
    }

    fun apply(bin: ByteArray): ByteArray {
        val bout: ByteArray
        val out1: DoubleArray
        val out2: DoubleArray
        val out1Buf = Array(sig1sr.size, { DoubleArray(partSize) })
        val out2Buf = Array(sig2sr.size, { DoubleArray(partSize) })
        saveInput(bin)
        var j = signalNumber
        for (i in 0 until sig1sr.size) {
            j = sigNumPrev(j)
            //println("${sig1s[j].dataRe.size},${ir1s[i].dataRe.size}")
            //val start = System.currentTimeMillis()
            var t = cm.execute(sig1sr[j], sig1si[j], ir1sr[i], ir1si[i])
            val ss1 = ifft.execute(t.first, t.second)
            //val ss1 = (sig1r[j] * ir1s[i]).applyIFFT()
            t = cm.execute(sig2sr[j], sig2si[j], ir2sr[i], ir2si[i])
            val ss2 = ifft.execute(t.first, t.second)
            for (k in 0 until partSize) {
                (out1Buf[i])[k] = ss1[k]
                (out2Buf[i])[k] = ss2[k]
            }
        }
        out1 = mixer.mix(*out1Buf)
        //out1 = sumArray(*out1Buf)
        out2 = mixer.mix(*out2Buf)
        //out2 = sumArray(*out2Buf)
        //out1 = DoubleArray(partSize)
        //out2 = DoubleArray(partSize)

        bout = getBytesFromIntArray(out1.map { it.toInt() }.toIntArray(), out2.map { it.toInt() }.toIntArray())

        return bout
    }
}

class IRConvolverMono(val partSize: Int = 128, val format: AudioFormat = defaultFormat) {
    private fun isPowerOfTwo(i: Int): Boolean {
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

    var ir1sr = Array(1, { DoubleArray(partSize * 2) })
    var ir1si = Array(1, { DoubleArray(partSize * 2) })

    //var ir2sr = Array(1,{DoubleArray(partSize*2)})
    //var ir2si = Array(1,{DoubleArray(partSize*2)})
    var sig1tBuf = Array(2, { DoubleArray(partSize * 2) })

    //var sig2tBuf = Array(2,{DoubleArray(partSize*2)})
    var sigtBufNum = 0 // 0 or 1
    var sig1sr = Array(1, { DoubleArray(partSize * 2) })
    var sig1si = Array(1, { DoubleArray(partSize * 2) })

    //var sig2sr = Array(1,{DoubleArray(partSize*2)})
    //var sig2si = Array(1,{DoubleArray(partSize*2)})
    var signalNumber = 0 // 0 until sig1s.size   0,1, .. ,sig1s.size-1,0,1, ..
    val fft = FFT(partSize * 2)
    val ifft = IFFT(partSize * 2)
    var mixer = Mixer(partSize, 1)
    var cm = ComplexMulti(partSize * 2)

    fun setIR(file: File) {
        val dsIR = getDoubleArrayFromBytes(SoundIO().loadFromFile(file))
        val bufNum = dsIR.first.size / partSize
        val gain: Double = 1.0 / (bufNum + 1) / Short.MAX_VALUE.toDouble()
        val dsIR1 = dsIR.first.map { it * gain }.toDoubleArray() //normalize
        //val dsIR2 = dsIR.second.map{it*gain}.toDoubleArray() //normalize
        mixer = Mixer(partSize, bufNum + 1)
        ir1sr = Array(bufNum + 1, { DoubleArray(partSize * 2) })
        ir1si = Array(bufNum + 1, { DoubleArray(partSize * 2) })
        //ir2sr = Array(bufNum+1,{DoubleArray(partSize*2)})
        //ir2si = Array(bufNum+1,{DoubleArray(partSize*2)})
        sig1sr = Array(bufNum + 1, { DoubleArray(partSize * 2) })
        sig1si = Array(bufNum + 1, { DoubleArray(partSize * 2) })
        //sig2sr = Array(bufNum+1,{DoubleArray(partSize*2)})
        //sig2si = Array(bufNum+1,{DoubleArray(partSize*2)})
        println(bufNum)
        for (i in 0..bufNum) {
            val i0 = i * partSize
            val i1 = if ((i + 1) * partSize < dsIR1.size) (i + 1) * partSize else dsIR1.size
            val rBuf = DoubleArray(partSize)
            for (j in 0 until i1 - i0) {
                rBuf[j] = dsIR1.sliceArray(i0 until i1)[j]
            }
            val t1 = fft.execute(DoubleArray(partSize * 2, {
                if (it < partSize) {
                    0.0
                } else {
                    rBuf[it - partSize]
                }
            }))
            ir1sr[i] = t1.first
            ir1si[i] = t1.second
/*
            for(j in 0 until i1-i0){
                rBuf[j] = dsIR2.sliceArray(i0 until i1)[j]
            }
            val t2 = fft.execute(DoubleArray(partSize*2,{
                if(it < partSize){
                    0.0
                }else{
                    rBuf[it - partSize]
                }}))
            ir2sr[i] = t2.first
            ir2si[i] = t2.second*/

        }
        println("IR loaded...")
    }

    fun saveInput(bin: ByteArray) {
        //bin.size = partSize
        if (bin.size / format.frameSize != partSize) IllegalArgumentException("bin size?")
        val din = getDoubleArrayFromBytes(bin)
        val din1 = din.first
        //val din2 = din.second
        val i1: Int
        when (signalNumber) {
            sig1sr.size - 1 -> {
                i1 = sig1sr.size - 1

                signalNumber = 0
            }
            else -> {
                i1 = signalNumber

                signalNumber++
            }
        }
        val j0: Int
        val j1: Int
        when (sigtBufNum) {
            0 -> {
                j0 = 1
                j1 = 0
                sigtBufNum = 1
            }
            else -> {
                j0 = 0
                j1 = 1
                sigtBufNum = 0
            }
        }
        for (i in 0 until partSize) {
            sig1tBuf[j1][i] = sig1tBuf[j0][i + partSize]
            sig1tBuf[j1][i + partSize] = din1[i]
            //sig2tBuf[j1][i] = sig2tBuf[j0][i + partSize]
            //sig2tBuf[j1][i + partSize] = din2[i]
        }
        val t = fft.execute(sig1tBuf[j1])
        sig1sr[i1] = t.first
        sig1si[i1] = t.second
        /*t = fft.execute(sig2tBuf[j1])
        sig2sr[i1] = t.first
        sig2si[i1] = t.second*/
    }

    fun sigNumPrev(i: Int): Int {
        return when (i) {
            0 -> sig1sr.size - 1
            else -> i - 1
        }
    }

    fun sigNumNext(i: Int): Int {
        return when (i) {
            sig1sr.size - 1 -> 0
            else -> i + 1
        }
    }

    fun apply(bin: ByteArray): ByteArray {
        val bout: ByteArray
        val out1: DoubleArray
        val out2: DoubleArray
        val out1Buf = Array(sig1sr.size, { DoubleArray(partSize) })
        //val out2Buf = Array(sig1sr.size,{DoubleArray(partSize)})
        saveInput(bin)
        var j = signalNumber
        for (i in 0 until sig1sr.size) {
            j = sigNumPrev(j)
            //println("${sig1s[j].dataRe.size},${ir1s[i].dataRe.size}")
            //val start = System.currentTimeMillis()
            var t = cm.execute(sig1sr[j], sig1si[j], ir1sr[i], ir1si[i])
            val ss1 = ifft.execute(t.first, t.second)
            //val ss1 = (sig1r[j] * ir1s[i]).applyIFFT()
            /*
            t = cm.execute(sig2sr[j],sig2si[j],ir2sr[i],ir2si[i])
            val ss2 = ifft.execute(t.first,t.second)
            */
            for (k in 0 until partSize) {
                (out1Buf[i])[k] = ss1[k]
                //(out2Buf[i])[k] = ss1[k]
            }
        }
        out1 = mixer.mix(*out1Buf)
        //out1 = sumArray(*out1Buf)
        out2 = out1
        //out2 = mixer.mix(*out2Buf)
        //out1 = DoubleArray(partSize)
        //out2 = DoubleArray(partSize)
        bout =
            getBytesFromIntArray(IntArray(out1.size, { out1[it].toInt() }), IntArray(out2.size, { out2[it].toInt() }))
        return bout
    }
}

class IRConvolverMono2(val partSize: Int = 128, val format: AudioFormat = defaultFormat) {
    var ir1s = Array(1, { Spectral(partSize * 2) })

    //var ir2s = Array(1,{DoubleArray(partSize*2)})
    var sig1tBuf = Array(2, { Signal(partSize * 2) })
    var sig2tBuf = Array(2, { Signal(partSize * 2) })
    var sigtBufNum = 0 // 0 or 1
    var sig1s = Array(1, { Spectral(partSize * 2) })
    var sig2s = Array(1, { Spectral(partSize * 2) })
    var signalNumber = 0 // 0 until sig1s.size   0,1, .. ,sig1s.size-1,0,1, ..

    //lateinit var irs1 :Spectral
    //lateinit var irs2 :Spectral
    fun setIR(file: File) {
        val dsIR = getDoubleArrayFromBytes(SoundIO().loadFromFile(file))
        val gain: Double = 1.0 / Short.MAX_VALUE.toDouble()
        val dsIR1 = dsIR.first.map { it * gain }.toDoubleArray() //normalize
        val dsIR2 = dsIR.second.map { it * gain }.toDoubleArray() //normalize

        val bufNum = dsIR1.size / partSize
        ir1s = Array(bufNum + 1, { Spectral(partSize * 2) })
        //ir2s = Array(bufNum+1,{Spectral(partSize*2)})
        sig1s = Array(bufNum + 1, { Spectral(partSize * 2) })
        sig2s = Array(bufNum + 1, { Spectral(partSize * 2) })
        println(bufNum)
        for (i in 0..bufNum) {
            val i0 = i * partSize
            val i1 = if ((i + 1) * partSize < dsIR1.size) (i + 1) * partSize else dsIR1.size
            val rBuf = DoubleArray(partSize)
            for (j in 0 until i1 - i0) {
                rBuf[j] = dsIR1.sliceArray(i0 until i1)[j]
            }
            ir1s[i] = Signal(rBuf, true).applyFFT()
            //for(j in 0 until i1-i0){
            //  rBuf[j] = dsIR2.sliceArray(i0 until i1)[j]
            //}
            //ir2s[i] = Signal(rBuf,true).applyFFT()
        }
        println("IR loaded...")
    }

    fun saveInput(bin: ByteArray) {
        //bin.size = partSize
        if (bin.size / format.frameSize != partSize) IllegalArgumentException("bin size?")
        val din = getDoubleArrayFromBytes(bin)
        val din1 = din.first
        val din2 = din.second
        val i1: Int
        when (signalNumber) {
            sig1s.size - 1 -> {
                i1 = sig1s.size - 1

                signalNumber = 0
            }
            else -> {
                i1 = signalNumber

                signalNumber++
            }
        }
        val j0: Int
        val j1: Int
        when (sigtBufNum) {
            0 -> {
                j0 = 1
                j1 = 0
                sigtBufNum = 1
            }
            else -> {
                j0 = 0
                j1 = 1
                sigtBufNum = 0
            }
        }
        for (i in 0 until partSize) {
            sig1tBuf[j1][i] = sig1tBuf[j0][i + partSize]
            sig1tBuf[j1][i + partSize] = din1[i]
            sig2tBuf[j1][i] = sig2tBuf[j0][i + partSize]
            sig2tBuf[j1][i + partSize] = din2[i]
        }
        sig1s[i1] = sig1tBuf[j1].applyFFT()
        sig2s[i1] = sig2tBuf[j1].applyFFT()
    }

    fun sigNumPrev(i: Int): Int {
        return when (i) {
            0 -> sig1s.size - 1
            else -> i - 1
        }
    }

    fun sigNumNext(i: Int): Int {
        return when (i) {
            sig1s.size - 1 -> 0
            else -> i + 1
        }
    }

    fun apply(bin: ByteArray): ByteArray {
        val bout: ByteArray
        val out1: DoubleArray
        val out2: DoubleArray
        val out1Buf = Array(sig1s.size, { DoubleArray(partSize) })
        val out2Buf = Array(sig2s.size, { DoubleArray(partSize) })
        saveInput(bin)
        var j = signalNumber
        for (i in 0 until sig1s.size) {
            j = sigNumPrev(j)
            //println("${sig1s[j].dataRe.size},${ir1s[i].dataRe.size}")
            val start = System.currentTimeMillis()
            val ss1 = (sig1s[j] * ir1s[i]).applyIFFT()
            //val ss2 = (sig2s[j] * ir1s[i]).applyIFFT()
            for (k in 0 until partSize) {
                (out1Buf[i])[k] = ss1.dataRe[k]
                //(out2Buf[i])[k] = ss2.dataRe[k]
            }


        }

        out1 = sumArray(*out1Buf)
        out2 = sumArray(*out2Buf)
        //out1 = DoubleArray(partSize)
        //out2 = DoubleArray(partSize)

        bout = getBytesFromIntArray(out1.map { it.toInt() }.toIntArray(), out2.map { it.toInt() }.toIntArray())

        return bout
    }
}