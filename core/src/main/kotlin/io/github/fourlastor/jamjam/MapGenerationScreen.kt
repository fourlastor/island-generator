package io.github.fourlastor.jamjam

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.FitViewport
import ktx.app.KtxScreen
import ktx.graphics.use
import squidpony.squidgrid.gui.gdx.DefaultResources
import squidpony.squidgrid.gui.gdx.FilterBatch
import squidpony.squidgrid.gui.gdx.SColor
import squidpony.squidgrid.gui.gdx.SparseLayers
import squidpony.squidgrid.gui.gdx.SquidInput
import squidpony.squidgrid.gui.gdx.SquidMouse
import squidpony.squidmath.Noise.Noise2D
import squidpony.squidmath.Noise.Scaled2D
import squidpony.squidmath.RNG
import squidpony.squidmath.ValueNoise

class MapGenerationScreen : KtxScreen {

    /**
     * Press:
     * R to reload the map (right now disabled as the seed is fixed
     * Arrows to move around
     */

    /** Some classes in SquidLib need access to a batch to render certain things, so it's a good idea to have one. */
    private val batch = FilterBatch()

    /** gotta have a random number generator. We can seed an RNG with any [Long] we want, or even a [String]. */
    private val rng = RNG("SquidLib!")

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
        SparseLayers(GRID_WIDTH, GRID_HEIGHT, CELL_WIDTH.toFloat(), CELL_HEIGHT.toFloat(), tcf).apply {
            /* this makes animations very fast, which is good for multi-cell movement but bad for attack animations. */
//        display.setAnimationDuration(0.125f);
//        display.setLightingColor(SColor.PAPAYA_WHIP);
            /*
             * These need to have their positions set before adding any entities if there is an offset involved.
             * There is no offset used here, but it's still a good practice here to set positions early on.
            */
            setPosition(0f, 0f)
        }

    private val input: SquidInput = manageInput()

    private val bgColor: Color = SColor.BLACK_DYE

    //Here we make sure our Stage, which holds any text-based grids we make, uses our Batch.
    private val stage: Stage =
        Stage(FitViewport((GRID_WIDTH * CELL_WIDTH).toFloat(), ((GRID_HEIGHT) * CELL_HEIGHT).toFloat()), batch)
            .apply { addActor(display) }

    private val noise = ChunkedNoiseGenerator(
        initialSeed = 124L,
        noise = Scaled2D(ValueNoise(), 0.1),
        chunkSize = CHUNK_SIZE
    )

    private val map: Array<Array<Color>> = Array(GRID_WIDTH) { Array(GRID_HEIGHT) { Color() } }

    private val mountainColor = SColor.THOUSAND_YEAR_OLD_BROWN
    private val hillColor = SColor.AURORA_CRICKET
    private val grassColor = SColor.KELLY_GREEN
    private val waterColor = SColor.BONDI_BLUE
    private val font = BitmapFont()

    init {
        rebuild()
    }

    override fun show() {
        super.show()
        Gdx.input.inputProcessor = InputMultiplexer(stage, input, object : InputAdapter() {

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

    private var chunkX = 0
    private var chunkY = 0

    private fun rebuild() {
        println("Building $chunkX, $chunkY...")
        forX { x ->
            forY { y ->
                val value = noise.noiseAt(chunkX, chunkY, x, y)
                val color = when {
                    value > 0.8 -> mountainColor
                    value > 0.7 -> hillColor
                    value > 0.5 -> grassColor
                    value >= 0 -> waterColor
                    else -> Color.YELLOW
                }
                map[x][y].set(color)
            }
        }
    }

    private fun manageInput() = SquidInput(
        /**
         * this is a big one.
         * SquidInput can be constructed with a KeyHandler (which just processes specific keypresses), a SquidMouse
         * (which is given an InputProcessor implementation and can handle multiple kinds of mouse move), or both.
         * keyHandler is meant to be able to handle complex, modified key input, typically for games that distinguish
         * between, say, 'q' and 'Q' for 'quaff' and 'Quip' or whatever obtuse combination you choose. The
         * implementation here handles hjkl keys (also called vi-keys), numpad, arrow keys, and wasd for 4-way movement.
         * Shifted letter keys produce capitalized chars when passed to KeyHandler.handle(), but we don't care about
         * that so we just use two case statements with the same body, i.e. one for 'A' and one for 'a'.
         * You can also set up a series of future moves by clicking within FOV range, using mouseMoved to determine the
         * path to the mouse position with a DijkstraMap (called playerToCursor), and using touchUp to actually trigger
         * the event when someone clicks.
         */
        { key, alt, ctrl, shift ->
            when (key) {
                'Q', 'q', SquidInput.ESCAPE -> {
                    Gdx.app.exit()
                }

                'R', 'r' -> {
                    rebuild()
                }

                SquidInput.UP_ARROW -> {
                    chunkY -= 1
                    rebuild()
                }

                SquidInput.DOWN_ARROW -> {
                    chunkY += 1
                    rebuild()
                }

                SquidInput.LEFT_ARROW -> {
                    chunkX -= 1
                    rebuild()
                }

                SquidInput.RIGHT_ARROW -> {
                    chunkX += 1
                    rebuild()
                }
            }
        },
        //The second parameter passed to a SquidInput can be a SquidMouse, which takes mouse or touchscreen
        //input and converts it to grid coordinates (here, a cell is 12 wide and 24 tall, so clicking at the
        // pixel position 15,51 will pass screenX as 1 (since if you divide 15 by 12 and round down you get 1),
        // and screenY as 2 (since 51 divided by 24 rounded down is 2)).
        SquidMouse(
            CELL_WIDTH.toFloat(),
            CELL_HEIGHT.toFloat(),
            GRID_WIDTH.toFloat(),
            GRID_HEIGHT.toFloat(),
            0,
            0,
            object : InputAdapter() {
                // if the user clicks and there are no awaitedMoves queued up, generate toCursor if it
                // hasn't been generated already by mouseMoved, then copy it over to awaitedMoves.
                override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    return false
                }

                override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                    return super.touchDown(screenX, screenY, pointer, button)
                }
            })
    )


    /**
     * Draws the map, applies any highlighting for the path to the cursor, and then draws the player.
     */
    private fun drawMap() {
        forX { x ->
            forY { y ->
                display.put(
                    x,
                    y,
                    '#',
                    map[x][y]
                )
            }
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
        Gdx.gl.glClearColor(bgColor.r, bgColor.g, bgColor.b, 1.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // need to display the map every frame, since we clear the screen to avoid artifacts.
        drawMap()
        if (input.hasNext() && !display.hasActiveAnimations()) {
            input.next()
        }

        // stage has its own batch and must be explicitly told to draw().
        stage.viewport.apply()
        stage.draw()
        stage.act()
        batch.use {
            font.draw(it, "($chunkX, $chunkY)", 25f, 50f)
        }
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        //very important to have the mouse behave correctly if the user fullscreens or resizes the game!
        input.mouse.reinitialize(
            width.toFloat() / GRID_WIDTH, height.toFloat() / (GRID_HEIGHT),
            GRID_WIDTH.toFloat(), GRID_HEIGHT.toFloat(), 0, 0
        )
        stage.viewport.update(width, height, true)
    }

    companion object {
        const val CHUNK_SIZE = 80

        /** In number of cells  */
        const val GRID_WIDTH = CHUNK_SIZE

        /** In number of cells  */
        const val GRID_HEIGHT = CHUNK_SIZE

        /** The pixel width of a cell  */
        const val CELL_WIDTH = 16

        /** The pixel height of a cell  */
        const val CELL_HEIGHT = 16

    }
}


class ChunkedNoiseGenerator(
    initialSeed: Long,
    private val noise: Noise2D,
    private val chunkSize: Int,
) {
    private var seed = initialSeed

    fun updateSeed(seed: Long) {
        this.seed = seed
    }

    fun noiseAt(chunkX: Int, chunkY: Int, x: Int, y: Int) = (noise.getNoiseWithSeed(
        (chunkX * chunkSize + x).toDouble(),
        (chunkY * chunkSize + y).toDouble(),
        seed,
    ) + 1.0) / 2
}
