package fr.william.camera_app.data.datasource.labels.impl

import com.google.firebase.firestore.FirebaseFirestore
import fr.william.camera_app.core.coroutines.DispatcherModule
import fr.william.camera_app.data.datasource.labels.Label
import fr.william.camera_app.data.datasource.labels.LabelsDatasource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject


class LabelsDatasourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @DispatcherModule.DefaultDispatcher private val defaultContext: CoroutineDispatcher
) : LabelsDatasource {
    private companion object {
        private const val TAG = "LabelsDatasourceImpl"
        private const val LABELS_COLLECTION = "labels"
    }

    override suspend fun getLabels(): Flow<List<Label>> =
        flow {
            val labels = firestore.collection(LABELS_COLLECTION)
                .get()
                .await()
                .toObjects(Label::class.java)
            emit(labels)
        }.catch { e ->
            throw IllegalStateException("Error while fetching labels", e)

        }.flowOn(defaultContext)


    override suspend fun addLabel(label: Label) : Flow<Instant> = flow {
        firestore.collection(LABELS_COLLECTION)
            .add(label)
            .await()
        emit(Instant.now())
    }.catch { e ->
        throw IllegalStateException("Error while adding label", e)
    }.flowOn(defaultContext)

    override suspend fun updateLabel(label: Label) {
        withContext(defaultContext) {
            firestore.collection(LABELS_COLLECTION)
                .document(label.uid.toString())
                .set(label)
                .await()
        }
    }
}