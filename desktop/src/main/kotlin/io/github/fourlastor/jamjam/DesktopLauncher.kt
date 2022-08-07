package io.github.fourlastor.jamjam

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setResizable(false)
        setWindowedMode(2880, 1620)
        setForegroundFPS(60)
    }
    Lwjgl3Application(Game(), config)
}
