import dsp.Signal
import dsp.SoundIO
import dsp.Spectral
import dsp.util.getBytesFromIntArray
import dsp.util.getDoubleArrayFromBytes
import dsp.util.getIntFromFrame
import dsp.util.hamWindow
import java.io.File
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.math.absoluteValue

object AppModel {
    var sio = SoundIO()

    //var rsio = RealtimeSoundIO()
    var ch1ary: DoubleArray = doubleArrayOf(0.0)
    var ch2ary: DoubleArray = doubleArrayOf(0.0)
    var reAry: DoubleArray = doubleArrayOf(0.0)
    var imAry: DoubleArray = doubleArrayOf(0.0)
    var recordedData: ByteArray = byteArrayOf(0) //ソース波形の生データ
    var irRecordedData: ByteArray = byteArrayOf(0) //インパルスレスポンス波形の生データ
    var wetData: ByteArray = byteArrayOf(0) //エフェクト処理後波形の生データ
    var r1ary = doubleArrayOf(0.0)//ソースのチャンネル1の時間信号
    var r2ary = doubleArrayOf(0.0)//ソースのチャンネル2の時間信号
    var rout1aryI = intArrayOf(0)//エフェクト処理後のチャンネル1の時間信号
    var rout2aryI = intArrayOf(0)//エフェクト処理後のチャンネル2の時間信号
    var rfft1 = doubleArrayOf(0.0)
    var rfft2 = doubleArrayOf(0.0)
    var ir1ary = doubleArrayOf(0.0)//IR波形のチャンネル1の時間信号
    var ir2ary = doubleArrayOf(0.0)//IR波形のチャンネル2の時間信号

    lateinit var irSpec: Spectral
    fun startRec() {

        val data = sio.rec(1024)
        println(data.size)
        ch1ary = DoubleArray(data.size / 4)
        ch2ary = DoubleArray(data.size / 4)
        for (i in 0 until (data.size / 4)) {
            val (i1, i2) = getIntFromFrame(data.sliceArray(i * 4..(i * 4 + 3)))
            ch1ary[i] = i1.toDouble()
            ch2ary[i] = i2.toDouble()
        }
        println(ch1ary.size)

    }

    fun stopPlay() {
        sio.stop()
    }

    fun impulse(t: Double, w: Int) {
        val x = Short.MAX_VALUE.toInt()
        val latch = CountDownLatch(2)
        val wait = w.toLong()
        thread {
            //Thread.sleep(wait)
            sio.play(getBytesFromIntArray(intArrayOf(0, x, x, 0), intArrayOf(0, x, x, 0)))
            latch.countDown()
        }
        thread {
            Thread.sleep(wait)
            irRecordedData = sio.rec(t)

            val rs = getDoubleArrayFromBytes(irRecordedData)
            ir1ary = rs.first
            ir2ary = rs.second
            latch.countDown()
        }
        latch.await()
    }

    /*
        fun record() {
            //recordedData = sio.rec(1.0)
            recordedData = sio.rec(10.0)

        }*/
    fun record(t: Double) {
        recordedData = sio.rec(t)


    }

    fun openAudioFile(file: File) {
        recordedData = sio.loadFromFile(file)
    }

    /*
        fun test() {
            val r1s = Signal(r1ary).applyFFT()
            println("r1s size")
            println(r1s.dataRe.size)
            val r1t = r1s.applyIFFT()
            println("r1s ifft end")
            val r2s = Signal(r2ary).applyFFT()
            val r2t = r2s.applyIFFT()
            rfft1 = r1s.dataRe
            rfft2 = r2s.dataRe

            rout1aryI = r1t.dataRe.map { it -> it.toInt() }.toIntArray()
            rout2aryI = r2t.dataRe.map { it -> it.toInt() }.toIntArray()
            wetData = getBytesFromIntArray(rout1aryI, rout2aryI)
            println(wetData.size)

        }
    */
    fun saveIR(file: File) {
        sio.saveBytes(file, irRecordedData)
    }

    fun applyReverb() {
        if (ir1ary.isNotEmpty() && r1ary.isNotEmpty()) {
            /*r1ary = DoubleArray(recordedData.size / 4)
            r2ary = DoubleArray(recordedData.size / 4)
            for (i in 0 until (recordedData.size / 4)) {
                val (i1, i2) = getIntFromFrame(recordedData.sliceArray(i * 4..(i * 4 + 3)))
                r1ary[i] = i1.toDouble()
                r2ary[i] = i2.toDouble()
            }*/
            val rs = getDoubleArrayFromBytes(recordedData)
            r1ary = rs.first
            r2ary = rs.second
            println(r1ary.size)
            val irSp1 = Signal(ir1ary, true).applyFFT()
            val irSp2 = Signal(ir2ary, true).applyFFT()
            val irSize = irSp1.dataRe.size
            println("IR size: ${irSize}")
            println("r1arysize: ${r1ary.size}")
            val frameNum = ((r1ary.size / (irSize / 2)) + 1)
            println("frameNum : $frameNum")
            val sigSize = (frameNum + 1) * irSize / 2
            val newR1ary = DoubleArray(sigSize)
            val newR2ary = DoubleArray(sigSize)
            for (i in r1ary.indices) {
                newR1ary[i + irSize / 2] = r1ary[i]
                newR2ary[i + irSize / 2] = r2ary[i]
            }

            println("new R1 size: ${newR1ary.size}")

            val rout1ary = DoubleArray(sigSize - irSize / 2)
            val rout2ary = DoubleArray(sigSize - irSize / 2)


            for (i in 0 until frameNum) {
                val s1 = Signal(newR1ary.sliceArray(i * irSize / 2 until (i) * irSize / 2 + irSize))
                val s2 = Signal(newR2ary.sliceArray(i * irSize / 2 until (i) * irSize / 2 + irSize))
                val s1s = s1.applyFFT()
                val s2s = s2.applyFFT()
                println("s1s.max = ${s1s.dataRe.max()}")
                //val out1t = ((irSp1*s1s).applyIFFT()).dataRe.sliceArray(0     until irSize/2)
                //val out2t = ((irSp2*s2s).applyIFFT()).dataRe.sliceArray(0    until irSize/2 )
                //
                val out1t = ((irSp1 * s1s).applyIFFT()).dataRe.sliceArray(0 until irSize / 2)
                val out2t = ((irSp2 * s2s).applyIFFT()).dataRe.sliceArray(0 until irSize / 2)
                for (j in 0 until (irSize / 2)) {
                    rout1ary[j + irSize / 2 * i] = out1t[j]
                    rout2ary[j + irSize / 2 * i] = out2t[j]
                }
            }
            val maxRo1 = rout1ary.maxBy { it -> it.absoluteValue }
            val maxRo2 = rout2ary.maxBy { it -> it.absoluteValue }
            val maxR1 = r1ary.maxBy { it -> it.absoluteValue }
            val maxR2 = r2ary.maxBy { it -> it.absoluteValue }
            val norm: Double
            if (maxR1 is Double && maxR2 is Double && maxRo1 is Double && maxRo2 is Double) {

                if (maxRo1 > maxRo2) {
                    norm = (maxR1 / maxRo1).absoluteValue * 0.98

                } else {
                    norm = (maxR2 / maxRo2).absoluteValue * 0.98
                }
            } else {
                norm = 0.0
            }

            rout1aryI = rout1ary.map { it -> (it * norm).toInt() }.toIntArray()
            println("rout1aryI : ${rout1aryI.max()}")
            rout2aryI = rout2ary.map { it -> (it * norm).toInt() }.toIntArray()
            wetData = getBytesFromIntArray(rout1aryI, rout2aryI)
            println("IR.max = ${irSp1.dataRe.maxBy { it -> it.absoluteValue }}")

            println("newR1ary.max = ${newR1ary.max()}")
        } else {
            throw IllegalArgumentException("array is empty")
        }
    }

    fun playDry() {
        sio.play(recordedData)
    }

    fun playWet() {
        sio.play(wetData)
    }

    fun applyHamWindow() {
        ch1ary = hamWindow(ch1ary)
        ch2ary = hamWindow(ch2ary)
    }


/*
    fun loadIR() {
        val data = sio.loadFromFile(this.javaClass.getResource("tw.wav"))
        ir1ary = DoubleArray(data.size / 4)
        ir2ary = DoubleArray(data.size / 4)
        for (i in 0 until (data.size / 4)) {
            val (i1, i2) = getIntFromFrame(data.sliceArray(i * 4..(i * 4 + 3)))
            ir1ary[i] = i1.toDouble()
            ir2ary[i] = i2.toDouble()
        }
    }
    */

    fun loadIR(file: File) {
        val data = sio.loadFromFile(file)
/*ir1ary = DoubleArray(data.size/4)
ir2ary = DoubleArray(data.size/4)
for(i in 0 unt(data.size / 4)){
    val (i1,i2) = getIntFromFrame(data.sliceArray(i*4 ..(i*4+3)))
    ir1ary[i] = i1.toDouble()
    ir2ary[i] = i2.toDouble()
}*/
        val rs = getDoubleArrayFromBytes(data)
        ir1ary = rs.first
        ir2ary = rs.second
    }
/*
    fun loadIRAndFFT() {
        val data = sio.loadFromFile(this.javaClass.getResource("cb1.wav"))
        val ch1ary_ = DoubleArray(data.size / 4)
        val ch2ary_ = DoubleArray(data.size / 4)
        for (i in 0 until (data.size / 4)) {
            val (i1, i2) = getIntFromFrame(data.sliceArray(i * 4..(i * 4 + 3)))
            ch1ary_[i] = i1.toDouble()
            ch2ary_[i] = i2.toDouble()
        }
        val s = Signal(ch1ary_, true)
        val t0 = System.nanoTime()
        val sp = s.applyFFT()
        val t1 = System.nanoTime()
        println("FFT process:${t1 - t0}ns")
        irSpec = sp
        reAry = sp.dataRe
        imAry = sp.dataIm

    }

    fun dft() {
        val sig = Signal(ch1ary)

        val sp = sig.applyDFT()
        reAry = sp.dataRe
        imAry = sp.dataIm
    }

    fun fft() {

        val sig = Signal(ir1ary)
        val t0 = System.nanoTime()
        val sp = sig.applyFFT()
        val t1 = System.nanoTime()
        println("FFT process:${t1 - t0}ns")
        reAry = sp.dataRe
        imAry = sp.dataIm
    }

    fun fft2() {
        val sig = Signal(ir1ary)
        val t0 = System.nanoTime()
        val sp = sig.applyFFT2()
        val t1 = System.nanoTime()
        println("FFT2 process:${t1 - t0}ns")
        reAry = sp.dataRe
        imAry = sp.dataIm
    }

    fun idft2() {
        val sp = Spectral(reAry, imAry)
        val sig = sp.applyIDFT2()
        reAry = sig.data
        imAry = DoubleArray(sig.data.size, { 0.0 })
    }

    fun ifft() {
        val sp = Spectral(reAry, imAry)
        val sp2 = sp.applyIFFT()
        reAry = sp2.dataRe
        imAry = sp2.dataIm
    }

    fun idft() {
        val sp = Spectral(reAry, imAry)
        val sp2 = sp.applyIDFT()
        reAry = sp2.dataRe
        imAry = sp2.dataIm
    }*/
}