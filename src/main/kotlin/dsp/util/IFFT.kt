package dsp.util


class IFFT(private val size: Int, private val tNum: Int = 1) {
    private val wRe: DoubleArray
    private val wIm: DoubleArray
    private val re: Array<DoubleArray>
    private val im: Array<DoubleArray>
    private val stepNum: Int
    private val index: Array<IntArray>
    private val bitRev: IntArray
    private val cr: Array<DoubleArray>
    private val ci: Array<DoubleArray>

    init {
        if (Integer.highestOneBit(size) != size) {
            throw RuntimeException("N is not a power of 2: " + size)
        }
        this.re = Array(2) { DoubleArray(size) }
        this.im = Array(2) { DoubleArray(size) }
        this.wIm = DoubleArray(size / 2)
        this.wRe = DoubleArray(size / 2)
        stepNum = Integer.numberOfTrailingZeros(size)
        index = Array(stepNum) { IntArray(size) }
        bitRev = IntArray(size)
        cr = Array(stepNum) { DoubleArray(size) }
        ci = Array(stepNum) { DoubleArray(size) }
        createBR()
        createW()
        createB()
        println("size = " + size)
        println("stepNum = " + stepNum)
        /*
        for (int i = 0; i < stepNum; i++) {
            for (int j = 0; j < size; j++) {
                System.out.print(index[i][j] + " ");
            }
            System.out.println("");
        }
        for (int i = 0; i < stepNum; i++) {
            for (int j = 0; j < size; j++) {
                System.out.print(cr[i][j] + " ");
            }
            System.out.println("");
            for (int j = 0; j < size; j++) {
                System.out.print(ci[i][j] + " ");
            }
            System.out.println("\n");
        }
    */
    }

    fun execute(inr: DoubleArray, ini: DoubleArray): DoubleArray {
        if (inr.size != size) throw IllegalArgumentException("input length?")
        val outr = DoubleArray(size)
        //var outi = DoubleArray(size)
        for (i in 0 until size) {
            im[0][i] = -ini[bitRev[i]]
            re[0][i] = inr[bitRev[i]]
        }
        exec(size, stepNum)

        for (i in 0 until ini.size) {
            outr[i] = re[stepNum % 2][i] / size
            //outi[i] = -im[stepNum % 2][i] / size
        }
        return outr
    }

    fun exec(range: Int, step: Int) {
/*
        for(k in 0 until step) {
            val latch = CountDownLatch(tNum)
            for (i in 0 until tNum) {
                thread {
                    for (j in (size / tNum) * i until (size / tNum) * (i + 1)) {
                        run(j, k)
                    }
                    latch.countDown()
                }
            }
            latch.await()
        }*/
        for (k in 0 until step) {

            for (i in 0 until size) {
                run(i, k)
            }
        }

    }

    fun run(i: Int, m: Int) {

        val j1 = m % 2
        //int j2 = j1 == 1 ? 0 : 1;
        val j2 = j1.inv() and 1
        val p2m = 1 shl m
        if (i % (p2m * 2) < p2m) {
            re[j2][i] = re[j1][i] + cr[m][i] * re[j1][index[m][i]] - ci[m][i] * im[j1][index[m][i]]
            im[j2][i] = im[j1][i] + ci[m][i] * re[j1][index[m][i]] + cr[m][i] * im[j1][index[m][i]]
        } else {
            re[j2][i] = re[j1][index[m][i]] + cr[m][i] * re[j1][i] - ci[m][i] * im[j1][i]
            im[j2][i] = im[j1][index[m][i]] + ci[m][i] * re[j1][i] + cr[m][i] * im[j1][i]
        }
    }

    private fun createBR() {
        bitRev[0] = 0
        var a = 1
        var b = size / 2
        while (a < size) {
            for (i in 0 until a) {
                bitRev[i + a] = bitRev[i] + b
            }
            a *= 2
            b /= 2
        }
    }

    private fun createB() {
        for (i in 0 until stepNum) {
            val p2m = 1 shl i
            //int x = (stepNum+1)/p2m;
            val dk = size / (1 shl i + 1)

            for (j in 0 until size) {
                if (j % (p2m * 2) < p2m) {
                    index[i][j] = j + p2m
                    cr[i][j] = wRe[dk * (j % (p2m * 2))]
                    ci[i][j] = wIm[dk * (j % (p2m * 2))]
                } else {
                    index[i][j] = j - p2m
                    //cr[i][j] = -wRe[x*(j % (p2m*2))];
                    //ci[i][j] = -wIm[x*(j % (p2m*2))];
                    cr[i][j] = -cr[i][j - p2m]
                    ci[i][j] = -ci[i][j - p2m]
                }
            }
        }
    }


    private fun createW() {
        for (i in 0 until size / 2) {
            val angle = -2.0 * i.toDouble() * Math.PI / size
            wRe[i] = Math.cos(angle).toDouble()
            wIm[i] = Math.sin(angle).toDouble()
        }
    }
}