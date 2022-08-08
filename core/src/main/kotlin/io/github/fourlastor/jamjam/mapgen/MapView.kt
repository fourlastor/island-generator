package io.github.fourlastor.jamjam.mapgen

import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.Viewport
import ktx.scene2d.actor
import ktx.scene2d.actors
import squidpony.squidgrid.gui.gdx.DefaultResources
import squidpony.squidgrid.gui.gdx.FilterBatch
import squidpony.squidgrid.gui.gdx.SparseLayers

class MapView(
    gridWidth: Int,
    gridHeight: Int,
    cellWidth: Float,
    cellHeight: Float,
    viewport: Viewport,
    private val stage: Stage = Stage(viewport, FilterBatch())
): InputProcessor by stage {
    private val tcf = DefaultResources.getCrispSlabFont()

    private val display =
        SparseLayers(
            gridWidth * 3,
            gridHeight * 3,
            cellWidth,
            cellHeight,
            tcf
        ).apply {
            setPosition(0f, 0f)
        }

    init {
        stage.actors { actor(display) }
    }

    val viewport: Viewport
        get() = stage.viewport

    fun fillArea(color: Color, x: Int, y: Int, width: Int, height: Int) {
        display.fillArea(color, x, y, width, height)
    }

    fun update() {
        viewport.apply()
        stage.draw()
        stage.act()
    }
}
