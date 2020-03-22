import javafx.animation.AnimationTimer
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.TextField
import javafx.stage.FileChooser
import java.net.URL
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.absoluteValue


class AppController : Initializable {

    val model = AppModel

    @FXML
    lateinit var irch: LineChart<Number, Number>

    @FXML
    lateinit var drych: LineChart<Number, Number>

    @FXML
    lateinit var wetch: LineChart<Number, Number>

    @FXML
    lateinit var irx: NumberAxis

    @FXML
    lateinit var dryx: NumberAxis

    @FXML
    lateinit var wetx: NumberAxis

    @FXML
    lateinit var iry: NumberAxis

    @FXML
    lateinit var dryy: NumberAxis

    @FXML
    lateinit var wety: NumberAxis


    @FXML
    lateinit var btn_laf: Button

    @FXML
    lateinit var btn_rc: Button

    @FXML
    lateinit var btn_lir: Button

    @FXML
    lateinit var btn_rv: Button

    @FXML
    lateinit var btn_ir_rec: Button

    @FXML
    lateinit var btn_ir_save: Button

    @FXML
    lateinit var btn_dry: Button

    @FXML
    lateinit var btn_stop: Button

    //@FXML lateinit var btn_rt:Button
    @FXML
    lateinit var statusLabel: Label

    @FXML
    lateinit var irLabel: Label

    @FXML
    lateinit var sourceLabel: Label

    @FXML
    lateinit var tf_ir_rec: TextField

    @FXML
    lateinit var tf_au_rec: TextField

    @FXML
    lateinit var tf_ir_del: TextField

    @FXML
    lateinit var prog: ProgressIndicator
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        irch.createSymbols = false
        drych.createSymbols = false
        wetch.createSymbols = false
        var aFlag = false

        // model.loadIR()
        // model.fft2()
        //model.fft()
        val irw = WaveViewer(irch, irx, iry)
        val dryw = WaveViewer(drych, dryx, dryy)
        val wetw = WaveViewer(wetch, wetx, wety)
        prog.isVisible = false
        this.btn_dry.onAction = EventHandler {
            statusLabel.text = "Status:Playing dry sound"
            btn_rc.isDisable = true
            btn_dry.isDisable = true
            btn_rv.isDisable = true
            btn_laf.isDisable = true
            prog.isVisible = true
            btn_lir.isDisable = true
            btn_ir_rec.isDisable = true
            btn_ir_save.isDisable = true
            val playDryTask = object : Task<Boolean>() {
                override fun call(): Boolean {
                    model.playDry()
                    Platform.runLater {
                        statusLabel.text = "Status:Played"
                        btn_rc.isDisable = false
                        btn_dry.isDisable = false
                        btn_rv.isDisable = false
                        prog.isVisible = false
                        btn_laf.isDisable = false
                        btn_lir.isDisable = false
                        btn_ir_rec.isDisable = false
                        btn_ir_save.isDisable = false
                    }
                    return true
                }

            }
            //chartUpdate()
            thread { playDryTask.run() }
        }
        this.btn_stop.onAction = EventHandler {

            val stopTask = object : Task<Boolean>() {
                override fun call(): Boolean {
                    model.stopPlay()
                    Platform.runLater {
                        statusLabel.text = "Status:Stop"
                        btn_rc.isDisable = false
                        btn_dry.isDisable = false
                        btn_rv.isDisable = false
                        btn_laf.isDisable = false
                        prog.isVisible = false
                        btn_lir.isDisable = false

                    }
                    return true
                }

            }
            //chartUpdate()
            thread { stopTask.run() }

        }

        this.btn_rv.onAction = EventHandler {
            statusLabel.text = "Status:Playing wet sound"
            btn_rc.isDisable = true
            btn_dry.isDisable = true
            btn_rv.isDisable = true
            prog.isVisible = true
            btn_laf.isDisable = true
            btn_ir_rec.isDisable = true
            btn_ir_save.isDisable = true
            btn_lir.isDisable = true
            val playWetTask = object : Task<Boolean>() {
                override fun call(): Boolean {
                    model.playWet()
                    Platform.runLater {
                        statusLabel.text = "Status:Played"
                        btn_rc.isDisable = false
                        btn_dry.isDisable = false
                        btn_rv.isDisable = false
                        btn_laf.isDisable = false
                        prog.isVisible = false
                        btn_lir.isDisable = false
                        btn_ir_rec.isDisable = false
                        btn_ir_save.isDisable = false
                    }
                    return true
                }

            }
            //chartUpdate()
            thread { playWetTask.run() }

        }
        this.btn_ir_rec.onAction = EventHandler {
            if (tf_ir_rec.text.toDoubleOrNull() == null || tf_ir_del.text.toIntOrNull() == null) {

            } else {

                statusLabel.text = "Status:IR Recording..."
                btn_rc.isDisable = true
                btn_dry.isDisable = true
                btn_rv.isDisable = true
                btn_laf.isDisable = true
                btn_lir.isDisable = true
                prog.isVisible = true


                btn_ir_rec.isDisable = true
                btn_ir_save.isDisable = true
                val recIRTask = object : Task<Boolean>() {
                    override fun call(): Boolean {
                        model.impulse(tf_ir_rec.text.toDouble(), tf_ir_del.text.toInt())
                        Platform.runLater {
                            statusLabel.text = "Status:Processed"
                            irLabel.text = "IR:[Recorded IR]"
                            btn_rc.isDisable = false

                            prog.isVisible = false

                            btn_laf.isDisable = false
                            btn_lir.isDisable = false
                            btn_ir_rec.isDisable = false
                            btn_ir_save.isDisable = false
                            irw.c1a = model.ir1ary
                            irw.c2a = model.ir2ary
                            irw.updateYAxis()
                            irw.update()

                        }
                        return true
                    }

                }
                thread { recIRTask.run() }

            }
        }
        this.btn_lir.onAction = EventHandler {
            val fc = FileChooser()
            fc.title = "Open IR File"


            val file = fc.showOpenDialog(this.btn_dry.scene.window)

            val loadIRTask = object : Task<Boolean>() {
                override fun call(): Boolean {
                    btn_laf.isDisable = true
                    btn_lir.isDisable = true

                    Platform.runLater {
                        statusLabel.text = "Status:IR Loading and Processing..."

                    }
                    model.loadIR(file)
                    Platform.runLater {
                        statusLabel.text = "Status:Processed"
                        irLabel.text = "IR:${file.name}"
                        btn_rc.isDisable = false
                        btn_laf.isDisable = false
                        btn_laf.isDisable = false
                        btn_lir.isDisable = false
                        prog.isVisible = false
                        //chart1Update()
                        irw.c1a = model.ir1ary
                        irw.c2a = model.ir2ary
                        irw.updateYAxis()
                        irw.update()
                    }
                    return true
                }
            }
            if (file != null) {

                thread { loadIRTask.run() }
            }
        }
        this.btn_ir_save.onAction = EventHandler {
            val fc = FileChooser()
            fc.title = "Save IR File"


            val file = fc.showSaveDialog(this.btn_dry.scene.window)

            val saveIRTask = object : Task<Boolean>() {
                override fun call(): Boolean {
                    btn_laf.isDisable = true
                    btn_lir.isDisable = true

                    Platform.runLater { statusLabel.text = "Status:IR Saving..." }
                    model.saveIR(file)
                    Platform.runLater {
                        statusLabel.text = "Status:Processed"
                        irLabel.text = "IR:${file.name}"
                        btn_rc.isDisable = false
                        btn_laf.isDisable = false
                        btn_laf.isDisable = false
                        btn_lir.isDisable = false
                        prog.isVisible = false
                        //chart1Update()
                        irw.c1a = model.ir1ary
                        irw.c2a = model.ir2ary
                        irw.updateYAxis()
                        irw.update()
                    }
                    return true
                }
            }
            if (file != null) {

                thread { saveIRTask.run() }
            }
        }
        this.btn_laf.onAction = EventHandler {
            statusLabel.text = "Status:Loading Audio File..."

            btn_rc.isDisable = true
            btn_dry.isDisable = true
            btn_rv.isDisable = true
            btn_laf.isDisable = true
            btn_lir.isDisable = true
            btn_stop.isDisable = true
            btn_ir_rec.isDisable = true
            btn_ir_save.isDisable = true
            prog.isVisible = true
            val fc = FileChooser()
            fc.title = "Open Source Audio File"

            val file = fc.showOpenDialog(this.btn_dry.scene.window)
            //Thread.sleep(100)
            //model.startRec()
            //model.fft()
            //model.ifft()
            val loadingAudioTask = object : Task<Boolean>() {
                override fun call(): Boolean {
                    model.openAudioFile(file)
                    Platform.runLater { statusLabel.text = "Status:Processing..." }
                    model.applyReverb()
                    Platform.runLater {
                        statusLabel.text = "Status:Processed"
                        sourceLabel.text = "Source:${file.name}"
                        btn_rc.isDisable = false
                        btn_dry.isDisable = false
                        prog.isVisible = false
                        btn_rv.isDisable = false
                        btn_laf.isDisable = false
                        btn_lir.isDisable = false
                        btn_stop.isDisable = false
                        btn_stop.isDisable = false
                        btn_ir_rec.isDisable = false
                        btn_ir_save.isDisable = false
                        //chart2Update()
                        //chart3Update()
                        wetw.c1a = model.rout1aryI.map { it.toDouble() }.toDoubleArray()
                        wetw.c2a = model.rout1aryI.map { it.toDouble() }.toDoubleArray()
                        dryw.c1a = model.r1ary
                        dryw.c2a = model.r2ary
                        wetw.updateYAxis()
                        dryw.updateYAxis()
                        wetw.update()
                        dryw.update()

                    }
                    return true
                }

            }
            //chartUpdate()
            thread { loadingAudioTask.run() }


            //model.test()
            //chartUpdate()

        }
        this.irch.onScroll = EventHandler { sc ->
            irw.slide(sc.deltaX)
            irw.zoom(sc.deltaY)
            irw.update()
        }
        this.drych.onScroll = EventHandler { sc ->
            dryw.slide(sc.deltaX)
            dryw.zoom(sc.deltaY)
            dryw.update()
        }
        this.wetch.onScroll = EventHandler { sc ->
            wetw.slide(sc.deltaX)
            wetw.zoom(sc.deltaY)
            wetw.update()
        }
        this.btn_rc.onAction = EventHandler {
            if (tf_au_rec.text.toDoubleOrNull() == null) {


            } else {
                statusLabel.text = "Status:Recording..."


                btn_rc.isDisable = true
                btn_dry.isDisable = true
                btn_rv.isDisable = true
                btn_laf.isDisable = true
                btn_lir.isDisable = true
                prog.isVisible = true
                //Thread.sleep(100)
                //model.startRec()
                //model.fft()
                //model.ifft()
                val recordingTask = object : Task<Boolean>() {
                    override fun call(): Boolean {
                        model.record(tf_au_rec.text.toDouble())
                        Platform.runLater { statusLabel.text = "Status:Processing..." }
                        model.applyReverb()
                        Platform.runLater {
                            statusLabel.text = "Status:Processed"
                            sourceLabel.text = "Source:[Recorded Sound]"
                            btn_rc.isDisable = false
                            btn_dry.isDisable = false
                            prog.isVisible = false
                            btn_rv.isDisable = false
                            btn_laf.isDisable = false
                            btn_lir.isDisable = false
                            btn_stop.isDisable = false
                            //chart2Update()
                            wetw.c1a = model.rout1aryI.map { it.toDouble() }.toDoubleArray()
                            wetw.c2a = model.rout1aryI.map { it.toDouble() }.toDoubleArray()
                            dryw.c1a = model.r1ary
                            dryw.c2a = model.r2ary
                            wetw.updateYAxis()
                            dryw.updateYAxis()
                            wetw.update()
                            dryw.update()

                            //chart3Update()
                        }
                        return true
                    }

                }
                //chartUpdate()
                thread { recordingTask.run() }


                //model.test()
                //chartUpdate()

            }
        }

        //a.start()
    }


}

class WaveViewer(val ch: LineChart<Number, Number>, val chx: NumberAxis, val chy: NumberAxis) {

    var scale = 1.0
        set(value) {
            if (value > 1.0) {
                field = value
                if (shift > value - 1.0) {
                    shift = 0.0

                }
            }
        }
    private var dScale = 0.01
    private var dShift = 0.001

    var c1a = doubleArrayOf(1.0)
    var c2a = doubleArrayOf(1.0)


    private var n = 1500
    fun zoomUp() {
        scale = scale + dScale
    }

    fun zoomOut() {
        scale = scale - dScale
    }

    fun zoom(s: Double) {
        scale = scale - dScale * s
    }

    fun slide(s: Double) {
        shift = shift - dShift * s
    }

    fun shiftLeft() {
        shift = shift - dShift
    }

    fun shiftRight() {
        shift = shift + dShift
    }

    var shift = 0.0
        set(value) {
            if (value < scale - 1.0 && value >= 0) {
                field = value

            }
        }

    fun updateYAxis() {
        val m1 = c1a.maxBy { it.absoluteValue }
        val m2 = c2a.maxBy { it.absoluteValue }
        val m: Double
        if (m1 is Double && m2 is Double) {

            if (m1 > m2) m = m1.absoluteValue else m = m2.absoluteValue
        } else {
            m = 0.0
        }
        chy.isAutoRanging = false
        chy.upperBound = m
        chy.lowerBound = -m
    }

    fun update() {
        var dataLength = c1a.size
        val ch1series = XYChart.Series<Number, Number>()
        val ch2series = XYChart.Series<Number, Number>()
        ch.data.clear()
        ch.isDisable = false

        ch1series.name = "Ch.1"
        ch2series.name = "Ch.2"


        if (dataLength < n) {
            chx.isAutoRanging = true
            for ((i, y2) in c1a.withIndex()) {

                ch2series.data.add(XYChart.Data(i / 44100.0, y2.toDouble()))
            }
            for ((i, y1) in c2a.withIndex()) {
                ch1series.data.add(XYChart.Data(i / 44100.0, y1.toDouble()))
            }
        } else {
            chx.isAutoRanging = false
            chx.upperBound = c1a.size.toDouble() / 44100.0 / scale * (1 + shift)
            chx.lowerBound = c1a.size.toDouble() / 44100.0 / scale * shift
            for (i in 0 until n) {

                val j =
                    ((chx.upperBound - chx.lowerBound) * 44100.0 * i / n.toDouble() + chx.lowerBound * 44100.0).toInt()// time

                ch1series.data.add(XYChart.Data(j / 44100.0, c1a[j]))
            }
            for (i in 0 until n) {

                val j =
                    ((chx.upperBound - chx.lowerBound) * 44100.0 * i / n.toDouble() + chx.lowerBound * 44100.0).toInt()// time

                ch2series.data.add(XYChart.Data(j / 44100.0, c2a[j]))
            }
            chx.tickUnit = (chx.upperBound - chx.lowerBound) / 10.0
            chy.tickUnit = (chy.upperBound - chy.lowerBound) / 4.0


        }
        ch.data.addAll(ch1series)
        ch.data.addAll(ch2series)
    }

}

class animationChart : AnimationTimer {
    var p: Long
    var f: (Any?) -> Unit
    var snum: Int
    var count: Long = 0
    var sTime: Long = 0

    constructor(period: Long, sn: Int, function: (Any?) -> Unit) : super() {
        p = period
        f = function
        snum = sn

    }

    override fun handle(t: Long) {
        if (sTime == 0L) sTime = t
        if (t - sTime > p) {
            count++
            sTime += p

        }
        f(null)
        //if(count.toInt() == snum)stop()
    }

}