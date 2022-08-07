package io.github.fourlastor.jamjam

import com.badlogic.gdx.Screen
import com.kotcrab.vis.ui.VisUI
import io.github.fourlastor.jamjam.mapgen.MapGenerationScreen
import ktx.app.KtxGame
import ktx.scene2d.Scene2DSkin

class Game : KtxGame<Screen>() {


    override fun create() {

        VisUI.load(VisUI.SkinScale.X2)
        Scene2DSkin.defaultSkin = VisUI.getSkin()

        addScreen(MapGenerationScreen())
        setScreen<MapGenerationScreen>()
    }
}
