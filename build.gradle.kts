// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {

    //ajout pour videoclassifier
    repositories {
        google()
        mavenCentral()
        maven { // repo for TFLite snapshot
            name = "ossrh-snapshot"
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
        }
    }

    dependencies {
        classpath(libs.gradle.download.task)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.google.ksp) apply false
    alias(libs.plugins.dagger.hilt) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.crashlytics) apply false
}