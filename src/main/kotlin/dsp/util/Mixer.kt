package dsp.util

class Mixer(val size: Int, val num: Int) {
    var buf: Array<DoubleArray>

    init {
        if (num % 2 == 0) {
            buf = Array(num / 2, { DoubleArray(size, { 0.0 }) })
        } else {
            buf = Array((num + 1) / 2, { DoubleArray(size, { 0.0 }) })
        }

    }

    fun mix(vararg ds: DoubleArray): DoubleArray {
        if (ds.size != num) throw IllegalArgumentException("mix num?")
        var s0 = num
        var s1 = buf.size
        if (s0 % 2 == 0) {

            for (k in 0 until s1) {
                for (j in 0 until size) {
                    buf[k][j] = ds[k * 2][j] + ds[k * 2 + 1][j]
                }
            }


        } else {

            for (k in 0 until s1) {
                for (j in 0 until size) {
                    if (k == s1 - 1) {
                        buf[k][j] = ds[s0 - 1][j]
                        //println("${k * 2}")
                    } else {
                        //println("${k * 2},${k * 2 + 1}")
                        buf[k][j] = ds[k * 2][j] + ds[k * 2 + 1][j]

                    }
                }
            }


        }
        s0 = s1
        s1 = if (s0 % 2 == 0) {
            s0 / 2
        } else {
            (s0 + 1) / 2
        }
        while (s0 > 2) {
            //println("fes")
            if (s0 % 2 == 0) {
                for (k in 0 until s1) {
                    for (j in 0 until size) {
                        buf[k][j] = buf[k * 2][j] + buf[k * 2 + 1][j]
                    }
                }

            } else {
                for (k in 0 until s1) {
                    for (j in 0 until size) {
                        if (k == s1 - 1) {
                            buf[k][j] = buf[s0 - 1][j]
                        } else {
                            buf[k][j] = buf[k * 2][j] + buf[k * 2 + 1][j]
                        }
                    }
                }

            }
            s0 = s1
            s1 = if (s0 % 2 == 0) {
                s0 / 2
            } else {
                (s0 + 1) / 2
            }
        }
        return DoubleArray(size, { buf[0][it] + buf[1][it] })

    }

}