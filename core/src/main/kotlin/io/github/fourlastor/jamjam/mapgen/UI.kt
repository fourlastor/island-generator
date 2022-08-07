package io.github.fourlastor.jamjam.mapgen

import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ScalingViewport
import com.badlogic.gdx.utils.viewport.Viewport
import ktx.actors.onChange
import ktx.scene2d.actors
import ktx.scene2d.container
import ktx.scene2d.vis.visLabel
import ktx.scene2d.vis.visScrollPane
import ktx.scene2d.vis.visTable
import ktx.scene2d.vis.visTextButton
import ktx.scene2d.vis.visTextField

class UI(
    worldWidth: Float,
    worldHeight: Float,
    private val onUpdate: (seed: String) -> Unit,
    private val stage: Stage = Stage(
        ScalingViewport(
            Scaling.stretch,
            worldWidth,
            worldHeight,
        )
    ),
) : InputProcessor by stage {
    val viewport: Viewport
        get() = stage.viewport

    init {
        stage.actors {
            visScrollPane {
                setFillParent(true)

                visTable(defaultSpacing = true) {
                    defaults()
                        .top()
                    visLabel("Temperature")
                    row()
                    visLabel("Min: ")
                    val minTempField = visTextField("0.0")
                    visLabel("Max:")
                    val maxTempField = visTextField("150.0")
                    row()
                    visLabel("Humidity")
                    row()
                    visLabel("Min:")
                    val minHumidityField = visTextField("0.0")
                    visLabel("max:")
                    val maxHumidityField = visTextField("1.0")
                    row()
                    visLabel("Altitude")
                    row()
                    visLabel("Min:")
                    val minAltitudeField = visTextField("-5")
                    visLabel("max:")
                    val maxAltitudeField = visTextField("200")
                    row()
                    visLabel("Seed:")
                    val seedField = visTextField("daniele")

                    row()
                    visTextButton("Update") {
                        onChange {
                            val seed = seedField.text ?: return@onChange
                            onUpdate(
                                seed,
                            )
                        }
                    }
                    pack()
                }
            }
            container {
                pad(8f)
                pack()
            }
        }
    }

    fun update() {
        stage.viewport.apply()
        stage.act()
        stage.draw()
    }

}
