package fr.william.camera_app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class CameraAppApplication : Application(){
    /*val appComponent: AppComponent by lazy {
        DaggerAppComponent.builder()
            .contextModule(ContextModule(this))
            .build()
    }*/
}