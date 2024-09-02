// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.autonomousapps.dependency-analysis") version "1.10.0"
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    id("com.google.devtools.ksp") version "1.9.0-1.0.11" apply false
}