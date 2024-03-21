package fr.william.camera_app.data.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.william.camera_app.data.repository.labels.LabelsRepository
import fr.william.camera_app.data.repository.labels.LabelsRepositoryImpl


@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindsLabelsRepository(
        impl: LabelsRepositoryImpl,
    ): LabelsRepository
}