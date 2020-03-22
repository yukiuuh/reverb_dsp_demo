package dsp.util

class Complex(var re: Double, var im: Double) {

    operator fun plus(other: Complex): Complex {
        return Complex(this.re + other.re, this.im + other.im)
    }

    operator fun minus(other: Complex): Complex {
        return Complex(this.re - other.re, this.im - other.im)
    }

    operator fun times(other: Complex): Complex {
        return Complex(this.re * other.re - this.im * other.im, this.re * other.im + this.im * other.re)
    }

    operator fun times(other: Double): Complex {
        return Complex(this.re * other, this.im * other)
    }

    operator fun div(other: Complex): Complex {
        return Complex(
            (this.re * other.re - this.im * other.im) / other.abs(),
            (-this.re * other.im - this.im * other.re) / other.abs()
        )
    }

    fun abs(): Double {
        return this.re * this.re + this.im * this.im
    }

    fun conj(): Complex {
        return Complex(this.re, -this.im)
    }
}