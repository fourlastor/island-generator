package io.github.fourlastor.jamjam.mapgen

class ChunkedCoordinate(
    private val chunkSize: Int,
    private val mapWidth: Int,
) {
    fun x(chunkX: Int, x: Int) = chunkX * chunkSize + x
    fun y(chunkY: Int, y: Int) = chunkY * chunkSize + y

    fun coordId(chunkX: Int, chunkY: Int, x: Int, y: Int) = x(chunkX, x) + y(chunkY, y) * mapWidth
}
