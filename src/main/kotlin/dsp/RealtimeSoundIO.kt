package dsp

import java.io.File
import javax.sound.sampled.*
import kotlin.concurrent.thread

open class RealtimeSoundIO(val bufferSize: Int = 1024, var format: AudioFormat = defaultFormat) {
    private enum class Status {
        FILE,
        PLAY
    }

    private var status: Status = Status.PLAY
    var output: SourceDataLine
    var input: TargetDataLine
    var iBuffer: Array<ByteArray>
    var oBuffer: Array<ByteArray>
    var iNum = 0
    var oNum = 0
    var running = false
    var active = true
    var bufferUpdate = false
    var inputUpdate = false
    var fBufferSize = format.frameSize * bufferSize
    val lineBufferSize = fBufferSize

    init {
        output = AudioSystem.getLine(DataLine.Info(SourceDataLine::class.java, format)) as SourceDataLine
        output.open(format, lineBufferSize)
        output.start()
        input = AudioSystem.getLine(DataLine.Info(TargetDataLine::class.java, format)) as TargetDataLine
        input.open(format, lineBufferSize)
        input.start()
        iBuffer = Array(2, { ByteArray(fBufferSize) })
        oBuffer = Array(2, { ByteArray(fBufferSize) })
        thread { pDaemon() }
        thread { rDaemon() }
    }

    fun pDaemon() {
        bufferUpdate = true
        while (active) {
            if (running) {
                when (status) {
                    Status.FILE, Status.PLAY -> {
                        output.write(oBuffer[oNum], 0, fBufferSize)

                        oNum = when (oNum == 0) {
                            true -> 1
                            false -> 0
                        }
                        bufferUpdate = true
                    }
                }
            } else {
                Thread.sleep(50)
            }

        }
    }

    fun rDaemon() {
        inputUpdate = true
        while (active) {
            if (running && status == Status.PLAY) {
                input.read(iBuffer[iNum], 0, fBufferSize)
                iNum = when (iNum == 0) {
                    true -> 1
                    false -> 0
                }
                println("ideal")
                inputUpdate = true

            } else {
                Thread.sleep(50)
            }

        }
    }

    fun bufferCopy(bin: ByteArray, bout: ByteArray) {
        if (bin.size > bout.size) throw IllegalArgumentException("buffer size?")
        for (i in bin.indices) {
            bout[i] = bin[i]
        }
    }

    fun bufferCopy(bin: ByteArray, oNum: Int) {
        if (bin.size > fBufferSize) throw IllegalArgumentException("buffer size?")
        for (i in bin.indices) {
            oBuffer[oNum][i] = bin[i]
        }
    }

    fun bufferCopy(iNum: Int, bout: ByteArray) {
        if (bout.size < fBufferSize) throw IllegalArgumentException("buffer size?")
        for (i in 0 until fBufferSize) {
            bout[i] = iBuffer[iNum][i]
        }
    }

    fun start() {
        status = Status.PLAY
        running = true
    }

    fun dataWrite(data: ByteArray) {
        //data size = Buffer Size
        if (fBufferSize != data.size) throw IllegalArgumentException("data size?")
        status = Status.PLAY
        while (true) {
            if (bufferUpdate) {
                val i = when (oNum) {
                    1 -> 0
                    else -> 1
                }
                bufferCopy(data, i)
                println("actual write")
                bufferUpdate = false
                return
            } else {
                Thread.sleep(1)
            }
        }
    }

    fun dataRead(): ByteArray {
        //data size = Buffer Size
        //if(bufferSize != data.size) throw IllegalArgumentException("data size?")
        val data = ByteArray(fBufferSize)
        status = Status.PLAY
        while (true) {
            if (inputUpdate) {
                val i = when (iNum) {
                    1 -> 0
                    else -> 1
                }
                bufferCopy(i, data)
                inputUpdate = false
                return data

            } else {

                Thread.sleep(10)
            }
        }
    }

    fun playFileDry(file: File) {
        status = Status.FILE
        val audioSource = AudioSystem.getAudioInputStream(file)
        while (true) {
            if (bufferUpdate) {
                val i = when (oNum) {
                    1 -> 0
                    else -> 1
                }
                val isRead = audioSource.read(oBuffer[i], 0, fBufferSize)
                if (isRead < 0) return
                bufferUpdate = false
            } else {
                Thread.sleep(0)
            }
        }
    }

    fun playDataDry(data: ByteArray) {
        status = Status.PLAY
        var i = 0
        while (i >= 0 && running) {
            val i1 = i * fBufferSize
            val i2: Int
            when ((i + 1) * fBufferSize >= data.size) {
                true -> {
                    i2 = data.size
                    i = -1
                }
                false -> {
                    i2 = (i + 1) * fBufferSize
                    i++
                }
            }
            dataWrite(data.sliceArray(i1 until i2))

        }
    }

    fun playFileWet(file: File) {
        status = Status.FILE
        val audioSource = AudioSystem.getAudioInputStream(file)
        val data = ByteArray(fBufferSize)
        while (true) {

            val isRead = audioSource.read(data, 0, fBufferSize)
            if (isRead < 0) return

            val start = System.currentTimeMillis()

            val end = System.currentTimeMillis()
            println("${end - start} m sec Effect")
            val data1 = effect(data)
            dataWrite(data1)
            bufferUpdate = false

        }
    }

    fun playDataWet(data: ByteArray) {
        status = Status.PLAY
        var i = 0
        while (i >= 0 && running) {
            val i1 = i * fBufferSize
            val i2: Int
            when ((i + 1) * fBufferSize >= data.size) {
                true -> {
                    i2 = data.size
                    i = -1
                }
                false -> {
                    i2 = (i + 1) * fBufferSize
                    i++
                }
            }
            dataWrite(effect(data.sliceArray(i1 until i2)))

        }
    }

    fun playWet() {
        status = Status.PLAY
        while (running) {
            val t0 = System.currentTimeMillis()
            dataWrite(effect(dataRead()))
            val t1 = System.currentTimeMillis()
            println("${t1 - t0} msec")
            //print(oBuffer[0].sliceArray(0..4).map{print("${it.toInt()} ")})
            //print(" ; ")
            //println(oBuffer[1].sliceArray(0..4).map{print("${it.toInt()} ")})

        }
    }

    fun playDry() {
        status = Status.PLAY
        while (running) {
            dataWrite(dataRead())
        }
    }

    fun stop() {
        running = false
    }

    fun rec() {

    }

    fun close() {
        active = false
    }

    var effect: (ByteArray) -> ByteArray = ::through

    fun through(iBuf: ByteArray): ByteArray {
        val oBuf = ByteArray(fBufferSize)
        bufferCopy(iBuf, oBuf)
        return oBuf
    }

}