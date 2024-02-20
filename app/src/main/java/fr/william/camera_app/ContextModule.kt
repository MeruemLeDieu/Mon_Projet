package fr.william.camera_app

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn


@Module
@InstallIn(ApplicationComponent::class)
object ContextModule {
    @Provides
    fun provideContext(application: CameraAppApplication): Context {
        return application.applicationContext
    }
}