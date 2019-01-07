package net.corda.option.base.pricingmodel

//_________________________________________________________________________
//
//    Copyright (C) 2011  Prof. Jayanth R. Varma, jrvarma@iima.ac.in,
//    Indian Institute of Management, Ahmedabad 380 015, INDIA
//
//    This program is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 2 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program (see file COPYING); if not, write to the
//    Free Software Foundation, Inc., 59 Temple Place, Suite 330,
//    Boston, MA  02111-1307  USA
//
//_________________________________________________________________________

// This defines a number of functions related to the Black-Scholes
// option pricing formula. This includes Black-Scholes call and put
// prices, Black-Scholes call and put implied volatilities and the various
// option greeks - delta, gamma, vega, theta and rho
class BlackScholes @JvmOverloads constructor(private var s: Double, private var X: Double, private var r: Double, private var Sigma: Double, private var t: Double, div_yield_: Double = 0.0) {
    internal var d1 = 0.toDouble()
    internal var d2 = 0.toDouble()
    internal var g = 0.toDouble()
    private var div_yield = 0.0

    private fun BSSafeD1(): Double {
        // This computes the BlackScholes quantity d1 safely i.e.
        // no division by zero and no log of zero
        val s0: Double
        if (Sigma == 0.0 || t == 0.0) {
            s0 = s * Math.exp((g + Sigma * Sigma / 2) * t)
            if (s0 > X) {
                return vHigh
            }
            if (s0 < X) {
                return -vHigh
            }
            return 0.0 // if (s0 == X)
        } else {
            return if (X == 0.0) {
                vHigh
            } else {
                //Below is the BlackScholes formula for d1
                (Math.log(s / X) + (g + Sigma * Sigma / 2) * t) / (Sigma * Math.sqrt(t))
            }
        }
    }

    internal constructor() : this(0.0, 0.0, 0.0, 0.0, 0.0, 0.0) {}

    init {
        div_yield = div_yield_
        g = r - div_yield
        d1 = BSSafeD1()
        d2 = d1 - Sigma * Math.sqrt(t)
    }

    @JvmOverloads internal fun reset(s_: Double, X_: Double, r_: Double, Sigma_: Double, t_: Double, div_yield_: Double = 0.0) {
        s = s_
        X = X_
        r = r_
        Sigma = Sigma_
        t = t_
        div_yield = div_yield_
        g = r - div_yield
        d1 = BSSafeD1()
        d2 = d1 - Sigma * Math.sqrt(t)
    }

    internal fun reset(Sigma_: Double) {
        Sigma = Sigma_
        d1 = BSSafeD1()
        d2 = d1 - Sigma * Math.sqrt(t)
    }

    fun BSCall(): Double {
        // Black Scholes call price
        return Math.exp(-r * t) * (s * Math.exp(g * t) * NormalCDF(d1) - X * NormalCDF(d2))
    }

    fun BSPut(): Double {
        // Black Scholes put price
        return Math.exp(-r * t) * (-s * Math.exp(g * t) * NormalCDF(-d1) + X * NormalCDF(-d2))
    }

    internal fun BSCallDelta(): Double {
        // Black Scholes call delta
        return Math.exp(-div_yield * t) * NormalCDF(d1)
    }

    internal fun BSPutDelta(): Double {
        return -Math.exp(-div_yield * t) * NormalCDF(-d1)
    }

    internal fun BSCallTheta(): Double {
        // Black Scholes call theta
        val a: Double = if (t == 0.0) {
            if (Math.abs(d1) == vHigh || Sigma == 0.0) {
                0.0
            } else {
                -vvHigh
            }
        } else {
            -s * NormOrdinate(d1) * Sigma * Math.exp(-div_yield * t) / (2 * Math.sqrt(t))
        }
        val b: Double = r * X * Math.exp(-r * t) * NormalCDF(d2)
        val c: Double = div_yield * s * NormalCDF(d1) * Math.exp(-div_yield * t)
        return a - b + c
    }

    internal fun BSPutTheta(): Double {
        // Black Scholes put theta
        val a = if (t == 0.0) {
            if (Math.abs(d1) == vHigh || Sigma == 0.0) {
                0.0
            } else {
                -vvHigh
            }
        } else {
            -s * NormOrdinate(d1) * Sigma * Math.exp(-div_yield * t) / (2 * Math.sqrt(t))
        }
        val b = r * X * Math.exp(-r * t) * NormalCDF(-d2)
        val c = div_yield * s * NormalCDF(-d1) * Math.exp(-div_yield * t)
        return a + b - c
    }

    internal fun BSGamma(): Double {
        // Black Scholes call/put gamma
        return if (Sigma == 0.0 || t == 0.0) {
            if (Math.abs(d1) == vHigh) {
                0.0
            } else {
                vvHigh
            }
        } else {
            NormOrdinate(d1) * Math.exp(-div_yield * t) / (s * Sigma * Math.sqrt(t))
        }
    }


    internal fun BSVega(): Double {
        // Black Scholes call/put vega
        return s * Math.sqrt(t) * NormOrdinate(d1) * Math.exp(-div_yield * t)
    }

    internal fun BSCallRho(): Double {
        return X * t * Math.exp(-r * t) * NormalCDF(d2)
    }

    internal fun BSFuturesCallRho(): Double {
        return -t * BSCall()
    }

    internal fun BSFuturesPutRho(): Double {
        return -t * BSPut()
    }

    internal fun BSPutRho(): Double {
        // Black Scholes put rho
        return -X * t * Math.exp(-r * t) * NormalCDF(-d2)
    }

    internal fun BSCallProb(): Double {
        // Black Scholes risk neutral probability of put exercise
        return NormalCDF(d2)
    }

    internal fun BSPutProb(): Double {
        // Black Scholes risk neutral probability of put exercise
        return NormalCDF(-d2)
    }


    internal fun NormOrdinate(z: Double): Double {
        // The normal ordinate (probability density function)
        return Math.exp(-0.5 * z * z) / Math.sqrt(2 * Pi)
    }

    internal fun NormalCDF_old(y: Double): Double {
        // Computes normal integral by using a rational polynomial approximation
        // This approximation is from Example 9.7.3 of
        // Fike, C.T. (1968), Computer Evaluation of Mathematical Functions
        // Englewood Cliffs, N.J., Prentice Hall
        // Let P(x) be the integral of the normal density from 0 to x. {,
        // the best minimax approximation R(x) to P(x) in the range [0,infinity)
        // among the class of rational functions V5,5[0,infinity) satisfying
        // R(0) = P(0) = 0, and
        // lim x tends to infinity R(x) = lim x tends to infinity P(x) = 0.5
        // is the function:
        //       a1 + a2*x + a3*x^2 + a4*x^3  + a5*x^4  + a6*x^5
        //     ----------------------------------------------------
        //       b1 + b2*x + b3*x^2 + b4*x^3  + b5*x^4  + b6*x^5
        // where the constants a1, a2, ..., a6 and b1, b2, ..., b6
        // are as defined below
        // The maximum absolute error of this approximation is 0.46x10^-4
        // i.e. 0.000046. Therefore, this approximation has the same accuracy
        // as the 4 place tables commonly found in statistics test books

        val a1 = 0.0
        val a2 = 9.050508
        val a3 = 0.767742
        val a4 = 1.666902
        val a5 = -0.624298
        val a6 = 0.5
        val b1 = 22.601228
        val B2 = 2.776898
        val b3 = 5.148169
        val b4 = 2.995582
        val b5 = -1.238661
        val b6 = 1.0

        // We now compute R(abs(y)) as an approximation to P(abs(y))

        val X = Math.abs(y)
        val temp1 = ((((a6 * X + a5) * X + a4) * X + a3) * X + a2) * X + a1
        val temp2 = ((((b6 * X + b5) * X + b4) * X + b3) * X + B2) * X + b1
        val Temp = temp1 / temp2

        // We now compute the normal integral N(y) from P(abs(y))

        if (y < 0) {
            return 0.5 - Temp
        } else {
            return 0.5 + Temp
        }
    }

    internal fun NormalCDF(x: Double): Double {
        val a1 = 0.319381530
        val a2 = -0.356563782
        val a3 = 1.781477937
        val a4 = -1.821255978
        val a5 = 1.330274429

        val gamma = 0.2316419
        val precision = 1e-6

        if (x >= 0.0) {
            val k = 1.0 / (1.0 + gamma * x)
            var temp = NormOrdinate(x) * k *
                    (a1 + k * (a2 + k * (a3 + k * (a4 + k * a5))))
            if (temp < precision)
                return 1.0
            temp = 1.0 - temp
            if (temp < precision)
                return 0.0
            return temp
        } else { // if(x < 0.0)
            return 1.0 - NormalCDF(-x)
        }
    }

    // replicates Sgn as in visual basic, the signum of a real number
    internal fun sgn(x: Double): Double {
        return when {
            x > 0 -> 1.0
            x < 0 -> -1.0
            else -> 0.0
        }
    }

    companion object {
        internal var vHigh = 100.0
        internal var vvHigh = 1E+30
        // The constant pi
        internal var Pi = 3.141592653589793
    }

}