package io.github.fourlastor.jamjam.viewport

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ScalingViewport

/** A [Scaling.fill] viewport that takes a [width] as a percentage of the screen*/
class PercentageWidthViewport(
    worldWidth: Float,
    worldHeight: Float,
    private val width: Float,
    private val offset: Float,
) : ScalingViewport(Scaling.fill, width * worldWidth, worldHeight) {

    override fun update(screenWidth: Int, screenHeight: Int, centerCamera: Boolean) {
        super.update((width * screenWidth).toInt(), screenHeight, centerCamera)
    }

    override fun setScreenBounds(screenX: Int, screenY: Int, screenWidth: Int, screenHeight: Int) {
        super.setScreenBounds(
            (Gdx.graphics.width * offset).toInt() + screenX,
            screenY,
            screenWidth,
            screenHeight
        )
    }
}
