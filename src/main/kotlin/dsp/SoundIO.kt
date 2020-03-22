package dsp

import java.io.ByteArrayInputStream
import java.io.File
import java.net.URL
import javax.sound.sampled.*
import kotlin.concurrent.thread

val defaultFormat = AudioFormat(44100.0f, 16, 2, true, false)

class SoundIO(val bufferSize: Int = 1024, var format: AudioFormat = defaultFormat) {

    private var output: SourceDataLine
    private var input: TargetDataLine

    init {
        output = AudioSystem.getLine(DataLine.Info(SourceDataLine::class.java, format)) as SourceDataLine
        output.open(format, format.frameSize * bufferSize)
        output.start()
        input = AudioSystem.getLine(DataLine.Info(TargetDataLine::class.java, format)) as TargetDataLine
        input.open(format, format.frameSize * bufferSize)
        input.start()
    }

    fun play(data: ByteArray) {
        output.write(data, 0, data.size)
    }

    fun play(data: ByteArray, length: Int) {
        output.write(data, 0, length)
    }

    fun playFile(fileName: URL) {
        val testFile = AudioSystem.getAudioInputStream(fileName)
        val tempFormat = this.format
        this.format = testFile.format
        output.drain()
        output.close()
        output.open(format, format.frameSize * bufferSize)
        output.start()
        var data = ByteArray(bufferSize)
        var isRead = 0
        thread {
            while (isRead != -1) {
                isRead = testFile.read(data, 0, data.size)
                if (isRead >= 0) this.play(data, isRead)
            }
            this.format = tempFormat
            output.drain()
            output.close()
            output.open(format, format.frameSize * bufferSize)
            output.start()
        }
    }

    fun loadFromFile(fileName: URL): ByteArray {
        val testFile = AudioSystem.getAudioInputStream(fileName)
        val tempFormat = this.format
        this.format = testFile.format

        var data = ByteArray(format.frameSize * testFile.frameLength.toInt())
        var isRead = testFile.read(data, 0, data.size)
        println(isRead)
        this.format = tempFormat
        return data
    }

    fun loadFromFile(fileName: File): ByteArray {
        val testFile = AudioSystem.getAudioInputStream(fileName)
        val tempFormat = this.format
        this.format = testFile.format

        var data = ByteArray(format.frameSize * testFile.frameLength.toInt())
        var isRead = testFile.read(data, 0, data.size)
        println(isRead)
        this.format = tempFormat
        return data
    }

    fun loadFromFile(fileName: URL, n: Int): ByteArray {
        //load file n samples
        val testFile = AudioSystem.getAudioInputStream(fileName)
        val tempFormat = this.format
        this.format = testFile.format

        var data = ByteArray(n * format.frameSize)
        var isRead = testFile.read(data, 0, n * format.frameSize)
        println(isRead)
        return data
    }

    fun stop() {
        output.stop()

        output.flush()
        output.drain()
        output.start()
    }

    fun saveBytes(file: File, bs: ByteArray) {
        val bis = ByteArrayInputStream(bs)
        val ais = AudioInputStream(bis, format, bs.size.toLong())
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, file)
        ais.close()
    }


    fun rec(sec: Double): ByteArray {
        println(format.frameSize)
        println(format.sampleRate * format.frameSize)
        val samplenum = (format.frameRate * sec).toInt() * format.frameSize
        val recBuffer = ByteArray(samplenum)
        println("rec start")
        input.read(recBuffer, 0, recBuffer.size)
        println("rec end")
        return recBuffer
    }

    fun rec(sample: Int): ByteArray {
        println(format.frameSize)
        println(format.sampleRate * format.frameSize)
        val samplenum = format.frameSize * sample
        val recBuffer = ByteArray(samplenum)
        println("rec start")
        input.read(recBuffer, 0, recBuffer.size)
        println("rec end")
        return recBuffer
    }
}