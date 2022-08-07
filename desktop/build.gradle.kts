plugins {
    id("application")
    kotlin("jvm")
    application
    id("com.diffplug.spotless")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

spotless {
    isEnforceCheck = false
    kotlin {
        ktfmt("0.37")
    }
}

group = "io.github.fourlastor"
version = "1.0"

application {
    mainClass.set("io.github.fourlastor.jamjam.DesktopLauncherKt")
}

@Suppress("UnstableApiUsage")
dependencies {
    api(project(":core"))
    api("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-desktop")
    api("com.badlogicgames.gdx:gdx-box2d-platform:${libs.versions.gdx.get()}:natives-desktop")
    api(libs.gdxBackendLwjgl3)
    api(libs.controllersDesktop)
}
