package io.github.fourlastor.jamjam

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.FillViewport
import ktx.app.KtxScreen
import squidpony.squidai.DijkstraMap
import squidpony.squidgrid.gui.gdx.DefaultResources
import squidpony.squidgrid.gui.gdx.FilterBatch
import squidpony.squidgrid.gui.gdx.MapUtility
import squidpony.squidgrid.gui.gdx.SColor
import squidpony.squidgrid.gui.gdx.SparseLayers
import squidpony.squidgrid.gui.gdx.SquidInput
import squidpony.squidgrid.gui.gdx.SquidMouse
import squidpony.squidgrid.gui.gdx.TextCellFactory
import squidpony.squidgrid.mapping.DungeonUtility
import squidpony.squidgrid.mapping.FlowingCaveGenerator
import squidpony.squidgrid.mapping.SectionDungeonGenerator
import squidpony.squidgrid.mapping.SerpentMapGenerator
import squidpony.squidgrid.mapping.styled.TilesetType
import squidpony.squidmath.Coord
import squidpony.squidmath.GreasedRegion
import squidpony.squidmath.OrderedMap
import squidpony.squidmath.RNG

class DungeonDemoScreen : KtxScreen {

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

    private val bgColor: Color = SColor.DB_INK

    //Here we make sure our Stage, which holds any text-based grids we make, uses our Batch.
    private val stage: Stage =
        Stage(FillViewport((GRID_WIDTH * CELL_WIDTH).toFloat(), ((GRID_HEIGHT) * CELL_HEIGHT).toFloat()), batch)
            .apply { addActor(display) }
    private val playerToCursor = DijkstraMap(DefaultResources.getGuiRandom())
    private val initialCursor = Coord.get(-1, -1)
    private val costs: OrderedMap<Char, Double> = OrderedMap<Char, Double>().apply {
        set('£', DijkstraMap.WALL)
        set('¢', 4.0)
        set('£', 2.0)
    }

    private lateinit var player: Coord
    private var playerGlyph: TextCellFactory.Glyph? = null
    private lateinit var decoDungeon: Array<CharArray>
    private lateinit var bareDungeon: Array<CharArray>
    private lateinit var lineDungeon: Array<CharArray>
    private lateinit var colorIndices: Array<FloatArray>
    private lateinit var bgColorIndices: Array<FloatArray>
    private lateinit var res: Array<DoubleArray>

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
                    translateX.toFloat(),
                    translateY.toFloat(),
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

    private fun rebuild() {
        val serpent = SerpentMapGenerator(GRID_WIDTH, GRID_HEIGHT, rng, rng.nextDouble(0.15))
        serpent.putWalledBoxRoomCarvers(rng.between(5, 10))
        serpent.putWalledRoundRoomCarvers(rng.between(2, 5))
        serpent.putRoundRoomCarvers(rng.between(1, 4))
        serpent.putCaveCarvers(rng.between(8, 15))
        val flowCaves = FlowingCaveGenerator(GRID_WIDTH, GRID_HEIGHT, TilesetType.DEFAULT_DUNGEON, rng)
        val dungeonGen = SectionDungeonGenerator(GRID_WIDTH, GRID_HEIGHT, rng)
        dungeonGen.addWater(SectionDungeonGenerator.CAVE, rng.between(10, 30))
        dungeonGen.addWater(SectionDungeonGenerator.ROOM, rng.between(3, 11))
        dungeonGen.addDoors(rng.between(10, 25), false)
        dungeonGen.addGrass(SectionDungeonGenerator.CAVE, rng.between(5, 25))
        dungeonGen.addGrass(SectionDungeonGenerator.ROOM, rng.between(0, 5))
        dungeonGen.addBoulders(SectionDungeonGenerator.ALL, rng.between(3, 11))
        if (rng.nextInt(3) == 0) dungeonGen.addLake(
            rng.between(5, 30),
            '£',
            '¢'
        ) else if (rng.nextInt(5) < 3) dungeonGen.addLake(
            rng.between(8, 35)
        ) else dungeonGen.addLake(0)
        when (rng.nextInt(18)) {
            0, 1, 2, 11, 12 -> decoDungeon =
                DungeonUtility.closeDoors(dungeonGen.generate(serpent.generate(), serpent.environment))

            3, 4, 5, 13 -> decoDungeon = DungeonUtility.closeDoors(dungeonGen.generate(TilesetType.DEFAULT_DUNGEON))
            6, 7 -> decoDungeon =
                DungeonUtility.closeDoors(dungeonGen.generate(TilesetType.ROUND_ROOMS_DIAGONAL_CORRIDORS))

            8 -> decoDungeon = DungeonUtility.closeDoors(dungeonGen.generate(TilesetType.REFERENCE_CAVES))
            9 -> decoDungeon = DungeonUtility.closeDoors(dungeonGen.generate(TilesetType.ROOMS_LIMIT_CONNECTIVITY))
            10 -> decoDungeon = DungeonUtility.closeDoors(dungeonGen.generate(TilesetType.CORNER_CAVES))
            14, 15, 16 -> decoDungeon =
                DungeonUtility.closeDoors(dungeonGen.generate(flowCaves.generate(), flowCaves.getEnvironment()))

            else -> decoDungeon =
                DungeonUtility.closeDoors(dungeonGen.generate(flowCaves.generate(), flowCaves.getEnvironment()))
        }

        //There are lots of options for dungeon generation in SquidLib; you can pass a TilesetType enum to generate()
        //as shown on the following lines to change the style of dungeon generated from ruined areas, which are made
        //when no argument is passed to generate or when TilesetType.DEFAULT_DUNGEON is, to caves or other styles.
        //decoDungeon = dungeonGen.generate(TilesetType.REFERENCE_CAVES); // generate caves
//        decoDungeon = dungeonGen.generate(TilesetType.ROUND_ROOMS_DIAGONAL_CORRIDORS); // generate large round rooms

        //getBareDungeon provides the simplest representation of the generated dungeon -- '#' for walls, '.' for floors.
        bareDungeon = dungeonGen.bareDungeon
        //When we draw, we may want to use a nicer representation of walls. DungeonUtility has lots of useful methods
        //for modifying char[][] dungeon grids, and this one takes each '#' and replaces it with a box-drawing character.
        lineDungeon = DungeonUtility.hashesToLines(decoDungeon, true)
        // it's more efficient to get random floors from a packed set containing only (compressed) floor positions.
        // CoordPacker is a deep and involved class, but when other classes request packed data, you usually just need
        // to give them a short array representing a region, as produced by CoordPacker.pack().
        val placement = GreasedRegion(bareDungeon, '.')
        //Coord is the type we use as a general 2D point, usually in a dungeon.
        //Because we know dungeons won't be huge, Coord is optimized for x and y values between -3 and 255, inclusive.
        //player is, here, just a Coord that stores his position. In a real game, you would probably have a class for
        //creatures, and possibly a subclass for the player.
        player = placement.retract8way().singleRandom(rng)
        val player = player
        if (!player.isWithin(GRID_WIDTH, GRID_HEIGHT)) rebuild()
        playerGlyph?.also { display.removeGlyph(it) }
        playerGlyph = display.glyph('@', SColor.RED_INCENSE, player.x, player.y)
        res = DungeonUtility.generateResistances(decoDungeon)


        //DijkstraMap is the pathfinding swiss-army knife we use here to find a path to the latest cursor position.
        playerToCursor.initialize(decoDungeon)
        playerToCursor.initializeCost(DungeonUtility.generateCostMap(decoDungeon, costs, 1.0))
        // DungeonUtility provides various ways to get default colors or other information from a dungeon char 2D array.
        colorIndices = MapUtility.generateDefaultColorsFloat(
            decoDungeon,
            '£',
            SColor.CW_PALE_GOLD.toFloatBits(),
            '¢',
            SColor.CW_BRIGHT_APRICOT.toFloatBits()
        )
        bgColorIndices = MapUtility.generateDefaultBGColorsFloat(
            decoDungeon,
            '£',
            SColor.CW_ORANGE.toFloatBits(),
            '¢',
            SColor.CW_RICH_APRICOT.toFloatBits()
        )
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
            }
        },  //The second parameter passed to a SquidInput can be a SquidMouse, which takes mouse or touchscreen
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
        for (x in 0 until GRID_WIDTH) {
            for (y in 0 until GRID_HEIGHT) {
                display.put(
                    x,
                    y,
                    lineDungeon[x][y],
                    colorIndices[x][y],
                    bgColorIndices[x][y]
                )
            }
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
        /** In number of cells  */
        const val GRID_WIDTH = 75

        /** In number of cells  */
        const val GRID_HEIGHT = 25

        /** The pixel width of a cell  */
        const val CELL_WIDTH = 16

        /** The pixel height of a cell  */
        const val CELL_HEIGHT = 16

    }
}
