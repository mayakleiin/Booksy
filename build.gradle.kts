plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.navigation.safeargs) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

buildscript {
    dependencies {

        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:${libs.versions.navigation.get()}")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
