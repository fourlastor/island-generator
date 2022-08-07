package io.github.fourlastor.jamjam

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        val displayMode = Lwjgl3ApplicationConfiguration.getDisplayMode()
        val size = minOf(displayMode.width * 0.75f, displayMode.height * 0.75f)
        setWindowedMode(size.toInt(), size.toInt())
        setForegroundFPS(60)
    }
    Lwjgl3Application(Game(), config)
}
