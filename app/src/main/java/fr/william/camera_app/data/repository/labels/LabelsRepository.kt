package fr.william.camera_app.data.repository.labels

import fr.william.camera_app.data.datasource.labels.Label
import fr.william.camera_app.data.datasource.labels.LabelsDatasource
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject


interface LabelsRepository {
    suspend fun getLabels(): Flow<List<Label>>

    suspend fun addLabel(label: Label) : Flow<Instant>

    suspend fun updateLabel(label: Label)
}

class LabelsRepositoryImpl @Inject constructor(
    private val labelsDatasource: LabelsDatasource
) : LabelsRepository {
    override suspend fun getLabels(): Flow<List<Label>> = labelsDatasource.getLabels()

    override suspend fun addLabel(label: Label) : Flow<Instant> = labelsDatasource.addLabel(label)

    override suspend fun updateLabel(label: Label) = labelsDatasource.updateLabel(label)
}