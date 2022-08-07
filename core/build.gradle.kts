
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
    api(libs.artemis)
    api(libs.gdx)
    api(libs.gdxAi)
    api(libs.ktxActors)
    api(libs.ktxApp)
    api(libs.ktxGraphics)
    api(libs.ktxMath)
    api(libs.ktxVis)
}
