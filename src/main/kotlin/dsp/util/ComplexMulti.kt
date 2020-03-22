package dsp.util

class ComplexMulti(val size: Int, val tNum: Int = 1) {

    fun execute(r1: DoubleArray, i1: DoubleArray, r2: DoubleArray, i2: DoubleArray): Pair<DoubleArray, DoubleArray> {
        if (size != r1.size) throw IllegalArgumentException("complexes size?")
        return Pair(DoubleArray(size, {
            r1[it] * r2[it] - i1[it] * i2[it]
        }), DoubleArray(size, {
            r1[it] * i2[it] + r2[it] * i1[it]
        }))
    }
}