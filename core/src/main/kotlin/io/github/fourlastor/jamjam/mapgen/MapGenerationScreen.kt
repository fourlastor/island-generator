package io.github.fourlastor.jamjam.mapgen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.IntMap
import io.github.fourlastor.jamjam.viewport.PercentageWidthViewport
import ktx.app.KtxScreen
import ktx.app.clearScreen
import squidpony.squidgrid.gui.gdx.SColor
import kotlin.math.roundToInt


class MapGenerationScreen : KtxScreen {

    private val bgColor: Color = SColor.BLACK_DYE

    private val mapView = MapView(
        GRID_WIDTH,
        GRID_HEIGHT,
        CELL_WIDTH.toFloat(),
        CELL_HEIGHT.toFloat(),
        PercentageWidthViewport(
            (GRID_WIDTH * CELL_WIDTH).toFloat(),
            ((GRID_HEIGHT) * CELL_HEIGHT).toFloat(),
            width = 0.7f,
            offset = 0f,
        )
    ).apply {
        viewport.setScreenBounds(
            0,
            0,
            mapWidth.roundToInt(),
            Gdx.graphics.height,
        )
    }

    private val ui = UI(
        viewport = PercentageWidthViewport(
            worldWidth = uiWidth,
            worldHeight = Gdx.graphics.height.toFloat(),
            width = 0.3f,
            offset = 0.7f,
        ),
        onUpdate = { seed ->
            noises.updateSeed(seed)
            rebuild()
        }
    )
        .apply {
            viewport.setScreenBounds(
                uiWidth.toInt(),
                0,
                uiWidth.roundToInt(),
                Gdx.graphics.height,
            )
        }

    private val uiWidth: Float
        get() = Gdx.graphics.width.toFloat() * 0.3f

    private val mapWidth: Float
        get() = Gdx.graphics.width.toFloat() * 0.7f


    private val noises = Noises(
        1.0,
        "map-generation",
    )

    private val map: IntMap<Color> = IntMap(GRID_HEIGHT * GRID_WIDTH * 9)

    override fun show() {
        rebuild()
        Gdx.input.inputProcessor = InputMultiplexer(mapView, ui, inputProcessor)
    }

    private val inputProcessor = object : InputAdapter() {

        private var x = -1
        private var y = -1

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            x = screenX
            y = screenY
            return true
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            if (x < 0 || y < 0) return false

            val translateX = (x - screenX).toFloat() / 2
            val translateY = (screenY - y).toFloat() / 2
            mapView.viewport.camera.translate(
                translateX,
                translateY,
                0f
            )

            x = screenX
            y = screenY

            return true
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            x = -1
            y = -1
            return true
        }
    }

    override fun hide() {
        super.hide()
        Gdx.input.inputProcessor = null
    }

    private fun rebuild() {
        onMap { x, y, coordId ->
            val altitude = noises.altitude(x, y)
            val temperature = noises.temperature(x, y)
            val color = when {
                altitude < 0.1 -> Colors.deepWater
                altitude < 0.5 -> Colors.water
                altitude < 0.8 -> Colors.sand
                altitude < 1.0 -> Colors.grass
                altitude < 1.5 -> if (temperature > 0.6) Colors.grass else Colors.forest
                altitude < 1.7 -> Colors.hill
                else -> Colors.mountain
            }
            map.put(coordId, color)
        }
    }

    private var localChunkX = 1
    private var localChunkY = 1

    private fun drawMap() {
        onMap { x, y, coordId ->
            val targetX = x - ((localChunkX - 1) * CHUNK_SIZE)
            val targetY = y - ((localChunkY - 1) * CHUNK_SIZE)
            mapView.fillArea(map.get(coordId), targetX, targetY, CELL_WIDTH, CELL_HEIGHT)
        }
    }

    private val chunked = ChunkedCoordinateCalculator(CHUNK_SIZE, MAP_WIDTH)

    private inline fun onMap(onCoordinate: (x: Int, y: Int, coordId: Int) -> Unit) {
        for (currentChunkX in localChunkX - 1..localChunkX + 1) {
            for (currentChunkY in localChunkY - 1..localChunkY + 1) {
                for (x in 0 until GRID_WIDTH) {
                    for (y in 0 until GRID_HEIGHT) {
                        onCoordinate(
                            chunked.x(currentChunkX, x),
                            chunked.y(currentChunkY, y),
                            chunked.coordId(currentChunkX, currentChunkY, x, y)
                        )
                    }
                }
            }
        }
    }

    override fun render(delta: Float) {
        clearScreen(bgColor.r, bgColor.g, bgColor.b, 1.0f, false)

        drawMap()

        mapView.update()
        ui.update()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        mapView.viewport.update(mapWidth.toInt(), height)
        ui.viewport.update(uiWidth.toInt(), height)
    }

    companion object {
        const val CHUNK_SIZE = 40

        /** In number of cells  */
        const val GRID_WIDTH = CHUNK_SIZE

        const val MAP_WIDTH = GRID_WIDTH * 3

        /** In number of cells  */
        const val GRID_HEIGHT = CHUNK_SIZE

        /** The pixel width of a cell  */
        const val CELL_WIDTH = 10

        /** The pixel height of a cell  */
        const val CELL_HEIGHT = 10
    }
}
