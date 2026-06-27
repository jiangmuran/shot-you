package com.shotyou.app.data.repository

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.shotyou.app.data.local.SessionDao
import com.shotyou.app.data.local.toDomain
import com.shotyou.app.data.local.toEntity
import com.shotyou.app.domain.model.GenerationSession
import com.shotyou.app.domain.model.PhotoGroup
import com.shotyou.app.domain.model.SessionStage
import com.shotyou.app.domain.repository.SessionRepository
import com.shotyou.app.util.Clock
import com.shotyou.app.util.IdGenerator
import com.shotyou.app.work.ClassificationWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val workManager: WorkManager,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) : SessionRepository {

    override fun observeSessions(): Flow<List<GenerationSession>> =
        sessionDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeSession(id: String): Flow<GenerationSession?> =
        sessionDao.observeById(id).map { it?.toDomain() }

    override suspend fun getSession(id: String): GenerationSession? =
        sessionDao.getById(id)?.toDomain()

    override suspend fun startClassification(uris: List<String>): String {
        val now = clock.now()
        val id = idGenerator.newId()
        sessionDao.upsert(
            GenerationSession(
                id = id,
                stage = SessionStage.CLASSIFYING,
                photoUris = uris.distinct(),
                createdAtMs = now,
                updatedAtMs = now,
            ).toEntity(),
        )
        val request = OneTimeWorkRequestBuilder<ClassificationWorker>()
            .setInputData(workDataOf(ClassificationWorker.KEY_SESSION_ID to id))
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .build()
        workManager.enqueueUniqueWork("classify-$id", ExistingWorkPolicy.REPLACE, request)
        return id
    }

    override suspend fun completeClassification(id: String, groups: List<PhotoGroup>) =
        update(id) { it.copy(stage = SessionStage.READY_FOR_REVIEW, groups = groups, error = null) }

    override suspend fun failClassification(id: String, error: String?) =
        update(id) { it.copy(stage = SessionStage.FAILED, error = error) }

    override suspend fun markGenerating(id: String) =
        update(id) { it.copy(stage = SessionStage.GENERATING) }

    override suspend fun deleteSession(id: String) = sessionDao.delete(id)

    override fun observeProcessedUris(): Flow<Set<String>> =
        sessionDao.observeAll().map { list -> list.flatMap { it.photoUris }.toSet() }

    private suspend fun update(id: String, transform: (GenerationSession) -> GenerationSession) {
        val current = sessionDao.getById(id)?.toDomain() ?: return
        sessionDao.upsert(transform(current).copy(updatedAtMs = clock.now()).toEntity())
    }
}
