
plugins {
    kotlin("jvm")
    id("com.diffplug.spotless")
}

spotless {
    isEnforceCheck = false
    kotlin {
        ktfmt("0.37")
    }
}

@Suppress("UnstableApiUsage")
dependencies {
    api(project(":ldtk"))
    api(libs.artemis)
    api(libs.controllers)
    api(libs.gdx)
    api(libs.gdxAi)
    api(libs.gdxBox2d)
    api(libs.ktxActors)
    api(libs.ktxApp)
    api(libs.ktxBox2d)
    api(libs.ktxGraphics)
    api(libs.ktxMath)
    api(libs.ktxVis)
}
