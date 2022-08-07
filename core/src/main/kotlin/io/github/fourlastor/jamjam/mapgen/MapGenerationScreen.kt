package io.github.fourlastor.jamjam.mapgen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.viewport.ExtendViewport
import ktx.app.KtxScreen
import ktx.app.clearScreen
import squidpony.squidgrid.gui.gdx.DefaultResources
import squidpony.squidgrid.gui.gdx.FilterBatch
import squidpony.squidgrid.gui.gdx.SColor
import squidpony.squidgrid.gui.gdx.SparseLayers


class MapGenerationScreen : KtxScreen {

    /**
     * Press:
     * R to reload the map (right now disabled as the seed is fixed
     * Arrows to move around
     */

    /** Some classes in SquidLib need access to a batch to render certain things, so it's a good idea to have one. */
    private val batch = FilterBatch()

    /** gotta have a random number generator. We can seed an RNG with any [Long] we want, or even a [String]. */

    private val tcf = DefaultResources.getCrispSlabFont()

    /**
     * display is a SquidLayers object, and that class has a very large number of similar methods for placing text
     * on a grid, with an optional background color and lightness modifier per cell. It also handles animations and
     * other effects, but you don't need to use them at all. SquidLayers also automatically handles the stretchable
     * distance field fonts, which are a big improvement over fixed-size bitmap fonts and should probably be
     * preferred for new games. SquidLayers needs to know what the size of the grid is in columns and rows, how big
     * an individual cell is in pixel width and height, and lastly how to handle text, which can be a BitmapFont or
     * a TextCellFactory. Either way, it will use what is given to make its TextCellFactory, and that handles the
     * layout of text in a cell, among other things. DefaultResources stores pre-configured BitmapFont objects but
     * also some TextCellFactory objects for distance field fonts; either one can be passed to this constructor.
     * the font will try to load Inconsolata-LGC-Custom as a bitmap font with a distance field effect.
     */
    private val display =
        SparseLayers(GRID_WIDTH * 3, GRID_HEIGHT * 3, CELL_WIDTH.toFloat(), CELL_HEIGHT.toFloat(), tcf).apply {
            setPosition(0f, 0f)
        }

    private val bgColor: Color = SColor.BLACK_DYE

    //Here we make sure our Stage, which holds any text-based grids we make, uses our Batch.
    private val stage: Stage =
        Stage(ExtendViewport((GRID_WIDTH * CELL_WIDTH).toFloat(), ((GRID_HEIGHT) * CELL_HEIGHT).toFloat()), batch)
            .apply { addActor(display) }

    private val chunked = ChunkedCoordinate(CHUNK_SIZE, MAP_WIDTH)

    private val noises = Noises(
        1.0,
        "map-generation",
    )

    private val map: IntMap<Color> = IntMap(GRID_HEIGHT * GRID_WIDTH * 9)

    private val ui = UI(
        worldWidth = Gdx.graphics.width.toFloat() - Gdx.graphics.height.toFloat(),
        worldHeight = Gdx.graphics.height.toFloat(),
        onUpdate = { seed ->
            noises.updateSeed(seed)
            rebuild()
        }
    )

    override fun show() {
        rebuild()
        Gdx.input.inputProcessor = InputMultiplexer(stage, ui, object : InputAdapter() {

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
                stage.viewport.camera.translate(
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
        })
    }

    override fun hide() {
        super.hide()
        Gdx.input.inputProcessor = null
    }

    private var localChunkX = 1
    private var localChunkY = 1

    private val mountain = SColor.SLATE_GRAY
    private val hill = SColor.AURORA_CRICKET
    private val grass = SColor.KELLY_GREEN
    private val forest = SColor.GREEN_BAMBOO
    private val deepWater = SColor.DARK_BLUE_LAPIS_LAZULI
    private val water = SColor.BONDI_BLUE
    private val sand = SColor.TAN

    private fun rebuild() {
        onMap { x, y, coordId ->
            val altitude = noises.altitude(x, y)
            val temperature = noises.temperature(x, y)
            val color = when {
                altitude < 0.1 -> deepWater
                altitude < 0.5 -> water
                altitude < 0.8 -> sand
                altitude < 1.0 -> grass
                altitude < 1.5 -> if (temperature > 0.6) grass else forest
                altitude < 1.7 -> hill
                else -> mountain
            }
            map.put(coordId, color)
        }
    }

    /**
     * Draws the map, applies any highlighting for the path to the cursor, and then draws the player.
     */
    private fun drawMap() {
        onMap { x, y, coordId ->
            display.put(
                x - ((localChunkX - 1) * CHUNK_SIZE),
                y - ((localChunkY - 1) * CHUNK_SIZE),
                '#',
                map.get(coordId),
            )
        }
    }


    private inline fun onMap(onCoordinate: (x: Int, y: Int, coordId: Int) -> Unit) {
        chunksX { chunkX ->
            chunksY { chunkY ->
                forX { x ->
                    forY { y ->
                        onCoordinate(
                            chunked.x(chunkX, x),
                            chunked.y(chunkY, y),
                            chunked.coordId(chunkX, chunkY, x, y)
                        )
                    }
                }
            }
        }
    }

    private inline fun chunksX(onX: (chunkX: Int) -> Unit) {
        for (currentChunkX in localChunkX - 1..localChunkX + 1) {
            onX(currentChunkX)
        }
    }

    private inline fun chunksY(onY: (chunkY: Int) -> Unit) {
        for (currentChunkY in localChunkY - 1..localChunkY + 1) {
            onY(currentChunkY)
        }
    }

    private inline fun forX(onX: (x: Int) -> Unit) {
        for (x in 0 until GRID_WIDTH) {
            onX(x)
        }
    }

    private inline fun forY(onY: (y: Int) -> Unit) {
        for (y in 0 until GRID_HEIGHT) {
            onY(y)
        }
    }

    override fun render(delta: Float) {
        // standard clear the background routine for libGDX
        clearScreen(bgColor.r, bgColor.g, bgColor.b, 1.0f, false)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // need to display the map every frame, since we clear the screen to avoid artifacts.
        drawMap()

        // stage has its own batch and must be explicitly told to draw().
        stage.viewport.apply()
        stage.draw()
        stage.act()
        ui.update()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)

        stage.viewport.setScreenBounds(
            0,
            0,
            height,
            height,
        )

        ui.viewport.setScreenBounds(
            height,
            0,
            width - height,
            height,
        )
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
