package fr.william.camera_app.data.datasource.labels

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface LabelsDatasource {
    suspend fun getLabels(): Flow<List<Label>>

    suspend fun addLabel(label: Label) :  Flow<Instant>

    suspend fun updateLabel(label: Label)
}

data class Label(
    /**
     * Unique identifier of the label.
     */
    @DocumentId
    val uid: String? = null,
    /**
     * Name of the label.
     */
    val name: String,
    /**
     * Timestamp of the label creation.
     */
    val timestamp: Timestamp,

    /**
     * Position of the label.
     */
    val position: Position
)

data class Position(
    /**
     * X coordinate of the label.
     */
    val x: Int,
    /**
     * Y coordinate of the label.
     */
    val y: Int
)