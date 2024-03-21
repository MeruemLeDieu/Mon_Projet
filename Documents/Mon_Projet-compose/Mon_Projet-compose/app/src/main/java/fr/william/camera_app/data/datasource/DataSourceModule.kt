package fr.william.camera_app.data.datasource

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import fr.william.camera_app.data.datasource.labels.LabelsDatasource
import fr.william.camera_app.data.datasource.labels.impl.LabelsDatasourceImpl


@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    @Binds
    abstract fun bindsLabelsDataSource(
        impl: LabelsDatasourceImpl,
    ): LabelsDatasource
}
