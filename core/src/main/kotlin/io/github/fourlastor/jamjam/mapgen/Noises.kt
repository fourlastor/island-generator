package io.github.fourlastor.jamjam.mapgen

import squidpony.squidmath.CrossHash
import squidpony.squidmath.Noise
import squidpony.squidmath.OpenSimplex2F
import squidpony.squidmath.ValueNoise

class Noises(
    scale: Double,
    initialSeed: String,
) {
    private var seed: Long = CrossHash.hash64(initialSeed)

    private val altitudeNoise = Noise.Scaled2D(ValueNoise(), 0.1 * scale)

    private val temperatureNoise = Noise.Scaled2D(OpenSimplex2F(), 0.01 * scale)

    fun updateSeed(seed: String) {
        this.seed = CrossHash.hash64(seed)
    }

    fun altitude(x: Int, y: Int): Double = altitudeNoise.geNoise(x, y)
    fun temperature(x: Int, y: Int): Double = temperatureNoise.geNoise(x, y)

    private fun Noise.Scaled2D.geNoise(x: Int, y: Int) = getNoiseWithSeed(
            x.toDouble(),
            y.toDouble(),
            seed
        )
        .let { it + 1 }
        .let { it / 2 }
}
