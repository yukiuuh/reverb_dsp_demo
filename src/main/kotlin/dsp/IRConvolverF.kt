package dsp

/*
class IRConvolverF(val partSize:Int = 128,val format : AudioFormat = defaultFormat){
    var ir1sR = Array(1,{FloatArray(partSize*2)})
    var ir1sI = Array(1,{FloatArray(partSize*2)})
    var ir2sR = Array(1,{FloatArray(partSize*2)})
    var ir2sI = Array(1,{FloatArray(partSize*2)})
    var sig1tBuf = Array(2,{FloatArray(partSize*2)})
    var sig2tBuf = Array(2,{FloatArray(partSize*2)})
    var sigtBufNum = 0 // 0 or 1
    var sig1sR = Array(1,{FloatArray(partSize*2)})
    var sig1sI = Array(1,{FloatArray(partSize*2)})
    var sig2sR = Array(1,{FloatArray(partSize*2)})
    var sig2sI = Array(1,{FloatArray(partSize*2)})
    var signalNumber = 0 // 0 until sig1s.size   0,1, .. ,sig1s.size-1,0,1, ..

    fun setIR(file:File){
        val dsIR = getDoubleArrayFromBytes( SoundIO().loadFromFile(file))
        val gain :Double = 1.0 / Short.MAX_VALUE.toDouble()
        val dsIR1 = dsIR.first.map{it*gain}.toDoubleArray() //normalize
        val dsIR2 = dsIR.second.map{it*gain}.toDoubleArray() //normalize

        val bufNum = dsIR1.size / partSize
        ir1sR = Array(bufNum + 1,{FloatArray(partSize*2)})
        ir1sI = Array(bufNum + 1,{FloatArray(partSize*2)})
        ir2sR = Array(bufNum + 1,{FloatArray(partSize*2)})
        ir2sI = Array(bufNum + 1,{FloatArray(partSize*2)})
        sig1sR = Array(bufNum + 1,{FloatArray(partSize*2)})
        sig1sI = Array(bufNum + 1,{FloatArray(partSize*2)})
        sig2sR = Array(bufNum + 1,{FloatArray(partSize*2)})
        sig2sI = Array(bufNum + 1,{FloatArray(partSize*2)})
        println(bufNum)
        for(i in 0 ..bufNum){
            val i0 = i*partSize
            val i1 = if((i+1)*partSize < dsIR1.size)(i+1)*partSize else dsIR1.size
            val rBuf = DoubleArray(partSize)
            for(j in 0 until i1-i0){
                rBuf[j] = dsIR1.sliceArray(i0 until i1)[j]
            }
            ir1s[i] = Signal(rBuf,true).applyFFT()
            for(j in 0 until i1-i0){
                rBuf[j] = dsIR2.sliceArray(i0 until i1)[j]
            }
            ir2s[i] = Signal(rBuf,true).applyFFT()
        }
        println("IR loaded...")
    }
    fun saveInput(bin:ByteArray){
        //bin.size = partSize
        if(bin.size/format.frameSize != partSize)IllegalArgumentException("bin size?")
        val din = getDoubleArrayFromBytes(bin)
        val din1 = din.first
        val din2 = din.second
        val i1 : Int
        when(signalNumber){
            sig1s.size-1 ->{
                i1 = sig1s.size-1

                signalNumber = 0
            }
            else->{
                i1 = signalNumber

                signalNumber++
            }
        }
        val j0:Int;val j1:Int
        when(sigtBufNum){
            0->{
                j0 = 1
                j1 = 0
                sigtBufNum = 1
            }
            else->{
                j0 = 0
                j1 = 1
                sigtBufNum = 0
            }
        }
        for(i in 0 until partSize) {
            sig1tBuf[j1][i] = sig1tBuf[j0][i + partSize]
            sig1tBuf[j1][i + partSize] = din1[i]
            sig2tBuf[j1][i] = sig2tBuf[j0][i + partSize]
            sig2tBuf[j1][i + partSize] = din2[i]
        }
        sig1s[i1] = sig1tBuf[j1].applyFFT()
        sig2s[i1] = sig2tBuf[j1].applyFFT()
    }
    fun sigNumPrev(i:Int):Int{
        return when(i){
            0->sig1s.size-1
            else->i - 1
        }
    }
    fun sigNumNext(i:Int):Int{
        return when(i){
            sig1s.size-1->0
            else->i+1
        }
    }
    fun apply(bin:ByteArray):ByteArray{
        val bout :ByteArray
        val out1 :DoubleArray
        val out2 : DoubleArray
        val out1Buf = Array(sig1s.size,{DoubleArray(partSize)})
        val out2Buf = Array(sig2s.size,{DoubleArray(partSize)})
        saveInput(bin)
        var j = signalNumber
        for(i in 0 until sig1s.size) {
            j = sigNumPrev(j)
            //println("${sig1s[j].dataRe.size},${ir1s[i].dataRe.size}")
            val start = System.currentTimeMillis()
            val ss1 = (sig1s[j] * ir1s[i]).applyIFFT()
            val ss2 = (sig2s[j] * ir2s[i]).applyIFFT()
            for(k in 0 until partSize) {
                (out1Buf[i])[k] = ss1.dataRe[k]
                (out2Buf[i])[k] = ss2.dataRe[k]
            }
            println("aa")

        }

        out1 = sumArray(*out1Buf)
        out2 = sumArray(*out2Buf)
        //out1 = DoubleArray(partSize)
        //out2 = DoubleArray(partSize)

        bout = getBytesFromIntArray(out1.map{it.toInt()}.toIntArray(),out2.map{it.toInt()}.toIntArray())

        return bout
    }
}

class IRConvolverMono(val partSize:Int = 128,val format : AudioFormat = defaultFormat){
    var ir1s = Array(1,{Spectral(partSize*2)})
    //var ir2s = Array(1,{Spectral(partSize*2)})
    var sig1tBuf = Array(2,{Signal(partSize*2)})
    var sig2tBuf = Array(2,{Signal(partSize*2)})
    var sigtBufNum = 0 // 0 or 1
    var sig1s = Array(1,{Spectral(partSize*2)})
    var sig2s = Array(1,{Spectral(partSize*2)})
    var signalNumber = 0 // 0 until sig1s.size   0,1, .. ,sig1s.size-1,0,1, ..
    //lateinit var irs1 :Spectral
    //lateinit var irs2 :Spectral
    fun setIR(file:File){
        val dsIR = getDoubleArrayFromBytes( SoundIO().loadFromFile(file))
        val gain :Double = 1.0 / Short.MAX_VALUE.toDouble()
        val dsIR1 = dsIR.first.map{it*gain}.toDoubleArray() //normalize
        val dsIR2 = dsIR.second.map{it*gain}.toDoubleArray() //normalize

        val bufNum = dsIR1.size / partSize
        ir1s = Array(bufNum+1,{Spectral(partSize*2)})
        //ir2s = Array(bufNum+1,{Spectral(partSize*2)})
        sig1s = Array(bufNum+1,{Spectral(partSize*2)})
        sig2s = Array(bufNum+1,{Spectral(partSize*2)})
        println(bufNum)
        for(i in 0 ..bufNum){
            val i0 = i*partSize
            val i1 = if((i+1)*partSize < dsIR1.size)(i+1)*partSize else dsIR1.size
            val rBuf = DoubleArray(partSize)
            for(j in 0 until i1-i0){
                rBuf[j] = dsIR1.sliceArray(i0 until i1)[j]
            }
            ir1s[i] = Signal(rBuf,true).applyFFT()
            //for(j in 0 until i1-i0){
              //  rBuf[j] = dsIR2.sliceArray(i0 until i1)[j]
            //}
            //ir2s[i] = Signal(rBuf,true).applyFFT()
        }
        println("IR loaded...")
    }
    fun saveInput(bin:ByteArray){
        //bin.size = partSize
        if(bin.size/format.frameSize != partSize)IllegalArgumentException("bin size?")
        val din = getDoubleArrayFromBytes(bin)
        val din1 = din.first
        val din2 = din.second
        val i1 : Int
        when(signalNumber){
            sig1s.size-1 ->{
                i1 = sig1s.size-1

                signalNumber = 0
            }
            else->{
                i1 = signalNumber

                signalNumber++
            }
        }
        val j0:Int;val j1:Int
        when(sigtBufNum){
            0->{
                j0 = 1
                j1 = 0
                sigtBufNum = 1
            }
            else->{
                j0 = 0
                j1 = 1
                sigtBufNum = 0
            }
        }
        for(i in 0 until partSize) {
            sig1tBuf[j1][i] = sig1tBuf[j0][i + partSize]
            sig1tBuf[j1][i + partSize] = din1[i]
            sig2tBuf[j1][i] = sig2tBuf[j0][i + partSize]
            sig2tBuf[j1][i + partSize] = din2[i]
        }
        sig1s[i1] = sig1tBuf[j1].applyFFT()
        sig2s[i1] = sig2tBuf[j1].applyFFT()
    }
    fun sigNumPrev(i:Int):Int{
        return when(i){
            0->sig1s.size-1
            else->i - 1
        }
    }
    fun sigNumNext(i:Int):Int{
        return when(i){
            sig1s.size-1->0
            else->i+1
        }
    }
    fun apply(bin:ByteArray):ByteArray{
        val bout :ByteArray
        val out1 :DoubleArray
        val out2 : DoubleArray
        val out1Buf = Array(sig1s.size,{DoubleArray(partSize)})
        val out2Buf = Array(sig2s.size,{DoubleArray(partSize)})
        saveInput(bin)
        var j = signalNumber
        for(i in 0 until sig1s.size) {
            j = sigNumPrev(j)
            //println("${sig1s[j].dataRe.size},${ir1s[i].dataRe.size}")
            val start = System.currentTimeMillis()
            val ss1 = (sig1s[j] * ir1s[i]).applyIFFT()
            //val ss2 = (sig2s[j] * ir1s[i]).applyIFFT()
            for(k in 0 until partSize) {
                (out1Buf[i])[k] = ss1.dataRe[k]
                //(out2Buf[i])[k] = ss2.dataRe[k]
            }


        }

        out1 = sumArray(*out1Buf)
        out2 = sumArray(*out2Buf)
        //out1 = DoubleArray(partSize)
        //out2 = DoubleArray(partSize)

        bout = getBytesFromIntArray(out1.map{it.toInt()}.toIntArray(),out2.map{it.toInt()}.toIntArray())

        return bout
    }
}*/