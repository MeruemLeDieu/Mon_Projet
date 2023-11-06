import de.undercouch.gradle.tasks.download.Download

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.crashlytics)
    id("androidx.navigation.safeargs")
    id ("jacoco")
}
android {
    namespace = "fr.william.camera_app"
    compileSdk = 34

    defaultConfig {
        applicationId = "fr.william.camera_app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    packaging {
        resources {
            excludes += listOf("/META-INF/{AL2.0,LGPL2.1}", "META-INF/versions/9/previous-compilation-data.bin", "META-INF/LICENSE-notice.md", "META-INF/LICENSE.md")
            merges += "META-INF/gradle/incremental.annotation.processors"
        }
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}
val ASSET_DIR = "$projectDir/src/main/assets"
val TEST_ASSETS_DIR = "$projectDir/src/androidTest/assets"

tasks.register<Download>("downloadTestFile") {
    src("https://storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/image_segmentation/android/lite-model_deeplabv3_1_metadata_2.tflite")
    dest("$TEST_ASSETS_DIR/deeplabv3.tflite")
    overwrite(false)
}

tasks.register<Download>("downloadModelFile") {
    src("https://storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/image_segmentation/android/lite-model_deeplabv3_1_metadata_2.tflite")
    dest("$ASSET_DIR/deeplabv3.tflite")
    overwrite(false)
}

tasks.named("preBuild") {
    dependsOn(tasks.getByName("downloadModelFile"), tasks.getByName("downloadTestFile"))
}


dependencies {

    //region Androidx
    implementation (libs.bundles.androidx)
    implementation (libs.bundles.coroutine)
    //endregion

    //region Hilt
    implementation (libs.bundles.hilt)
    kapt(libs.hilt.compiler)
    implementation (libs.androidx.recyclerview)
    androidTestImplementation (libs.hilt.testing)
    //endregion

    //region Compose
    implementation(libs.bundles.material)
    //endregion

    //region Firebase
    implementation (platform(libs.firebase.bom))
    implementation (libs.bundles.firebase)
    //endregion

    // region Room
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)
    androidTestImplementation (libs.room.testing)
    //endregion

    testImplementation (libs.junit)
    testImplementation (libs.coroutine.test)
    debugImplementation (libs.mockk)
    androidTestImplementation (libs.robolectric)

    androidTestImplementation (libs.androidx.junit)
    androidTestImplementation (libs.androidx.test.runner)
    androidTestImplementation (libs.hilt.testing)
    kaptAndroidTest (libs.hilt.compiler)

    //tensorflow
    implementation(libs.bundles.tensorflow)

    //camera
    implementation(libs.bundles.camera)

}
