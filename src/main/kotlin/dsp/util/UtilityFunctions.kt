package dsp.util

import dsp.RealSumKernel
import dsp.Signal
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread


//16Bit,2ch -> 4bytes
fun getIntFromFrame(bs: ByteArray): Pair<Int, Int> {
    return Pair(
        (ByteBuffer.wrap(bs, 0, 2).order(ByteOrder.LITTLE_ENDIAN).short).toInt(),
        (ByteBuffer.wrap(bs, 2, 2).order(ByteOrder.LITTLE_ENDIAN).short).toInt()
    )
}

fun getDoubleArrayFromBytes(bs: ByteArray): Pair<DoubleArray, DoubleArray> {
    val c2ary = DoubleArray(bs.size / 4)
    val c1ary = DoubleArray(bs.size / 4)
    if ((bs.size / 4) % 4 == 0) {
        val loop = { a0: Int, a1: Int ->
            for (i in a0 until a1) {
                val (i1, i2) = getIntFromFrame(bs.sliceArray(i * 4..(i * 4 + 3)))
                c1ary[i] = i1.toDouble()
                c2ary[i] = i2.toDouble()
            }
        }
        val latch = CountDownLatch(4)
        thread {
            loop(0, bs.size / 16)
            latch.countDown()
        }
        thread {
            loop(bs.size / 16, bs.size / 8)
            latch.countDown()
        }
        thread {
            loop(bs.size / 8, bs.size * 3 / 16)
            latch.countDown()
        }
        thread {
            loop(bs.size * 3 / 16, bs.size / 4)
            latch.countDown()
        }
        latch.await()
    } else if ((bs.size / 4) % 2 == 0) {
        val loop = { a0: Int, a1: Int ->
            for (i in a0 until a1) {
                val (i1, i2) = getIntFromFrame(bs.sliceArray(i * 4..(i * 4 + 3)))
                c1ary[i] = i1.toDouble()
                c2ary[i] = i2.toDouble()
            }
        }
        val latch = CountDownLatch(2)
        val n = bs.size / 4
        thread {
            loop(0, n / 2)
            latch.countDown()
        }
        thread {
            loop(n / 2, n)
            latch.countDown()
        }
        latch.await()
    } else {
        for (i in 0 until (bs.size / 4)) {
            val (i1, i2) = getIntFromFrame(bs.sliceArray(i * 4..(i * 4 + 3)))
            c1ary[i] = i1.toDouble()
            c2ary[i] = i2.toDouble()
        }
    }
    return Pair(c1ary, c2ary)


}

fun getFloatArrayFromBytes(bs: ByteArray): Pair<FloatArray, FloatArray> {
    val c2ary = FloatArray(bs.size / 4)
    val c1ary = FloatArray(bs.size / 4)
    if ((bs.size / 4) % 4 == 0) {
        val loop = { a0: Int, a1: Int ->
            for (i in a0 until a1) {
                val (i1, i2) = getIntFromFrame(bs.sliceArray(i * 4..(i * 4 + 3)))
                c1ary[i] = i1.toFloat()
                c2ary[i] = i2.toFloat()
            }
        }
        val latch = CountDownLatch(4)
        thread {
            loop(0, bs.size / 16)
            latch.countDown()
        }
        thread {
            loop(bs.size / 16, bs.size / 8)
            latch.countDown()
        }
        thread {
            loop(bs.size / 8, bs.size * 3 / 16)
            latch.countDown()
        }
        thread {
            loop(bs.size * 3 / 16, bs.size / 4)
            latch.countDown()
        }
        latch.await()
    } else if ((bs.size / 4) % 2 == 0) {
        val loop = { a0: Int, a1: Int ->
            for (i in a0 until a1) {
                val (i1, i2) = getIntFromFrame(bs.sliceArray(i * 4..(i * 4 + 3)))
                c1ary[i] = i1.toFloat()
                c2ary[i] = i2.toFloat()
            }
        }
        val latch = CountDownLatch(2)
        val n = bs.size / 4
        thread {
            loop(0, n / 2)
            latch.countDown()
        }
        thread {
            loop(n / 2, n)
            latch.countDown()
        }
        latch.await()
    } else {
        for (i in 0 until (bs.size / 4)) {
            val (i1, i2) = getIntFromFrame(bs.sliceArray(i * 4..(i * 4 + 3)))
            c1ary[i] = i1.toFloat()
            c2ary[i] = i2.toFloat()
        }
    }
    return Pair(c1ary, c2ary)


}

fun getBytesFromIntArray(data1: IntArray, data2: IntArray): ByteArray {
    if (data1.size != data2.size) throw IllegalArgumentException("data1 data2 length??")
    val out = ByteArray(data1.size * 4)
    val latch = CountDownLatch(2)

    thread {
        for (i in 0 until (data1.size)) {

            val a1 = ByteBuffer.allocate(4).putInt(data1[i]).array().sliceArray(2..3)

            out[i * 4] = a1[1]
            out[i * 4 + 1] = a1[0]


        }
        latch.countDown()
    }

    thread {
        for (i in 0 until (data2.size)) {


            val a2 = ByteBuffer.allocate(4).putInt(data2[i]).array().sliceArray(2..3)

            out[i * 4 + 2] = a2[1]
            out[i * 4 + 3] = a2[0]

        }
        latch.countDown()
    }
    latch.await()
    return out

}

fun get2BytesFromInt(x: Int): ByteArray {
    val a = ByteBuffer.allocate(4).putInt(x).array().sliceArray(2..3)
    return a
}

fun sumSignal(vararg signals: Signal): Signal {

    if (signals.all { it -> it.data.size == signals[0].data.size }) throw IllegalArgumentException("signals length??")
    val out = Signal(signals[0].data.size)
    val b = signals[0].data.size
    val loop = { i0: Int, i1: Int ->

        for (i in i0 until i1) {
            for (j in 0 until signals.size) {
                out.data[i] += signals[j][i]
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
    return out

}

fun sumArray(vararg ds: DoubleArray): DoubleArray {

    //if(ds.any{it ->it.size != ds[0].size})throw IllegalArgumentException("ds length??")
    val out = DoubleArray(ds[0].size)

    val b = ds[0].size
    val loop = { i0: Int, i1: Int ->

        for (i in i0 until i1) {
            for (j in 0 until ds.size) {
                out[i] += ds[j][i] / 2.0
            }
        }
    }
    println("test")
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

fun sumArray(vararg ds: FloatArray): FloatArray {

    if (ds.any { it -> it.size != ds[0].size }) throw IllegalArgumentException("ds length??")
    val out = FloatArray(ds[0].size)
    val b = ds[0].size
    val loop = { i0: Int, i1: Int ->

        for (i in i0 until i1) {
            for (j in 0 until ds.size) {
                out[i] += ds[j][i] / 10.0f
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
    return out

}

fun sumArray3(vararg ds: DoubleArray): DoubleArray {

    if (ds.any { it -> it.size != ds[0].size }) throw IllegalArgumentException("ds length??")
    val out = DoubleArray(ds[0].size)
    val b = ds[0].size
    val loop = { i0: Int, i1: Int ->

        for (i in i0 until i1) {
            for (j in 0 until ds.size) {
                out[i] += ds[j][i]
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
    return out

}

val sk = RealSumKernel(4096)
fun sumArray2(vararg ds: DoubleArray): FloatArray {

    if (ds.any { it -> it.size != sk.size }) throw IllegalArgumentException("ds length??")


    return sk.calc(*ds)

}