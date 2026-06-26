package com.shotyou.app.data.repository

import com.shotyou.app.data.local.GenerationJobDao
import com.shotyou.app.data.local.toDomain
import com.shotyou.app.domain.model.GenerationJob
import com.shotyou.app.domain.repository.GenerationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enqueues and observes background generation jobs.
 *
 * P1 skeleton — observe* are wired to Room already; enqueue/retry/cancel are implemented
 * by the Queue agent in P2 (WorkManager). Constructor deps may be added freely.
 */
@Singleton
class GenerationRepositoryImpl @Inject constructor(
    private val jobDao: GenerationJobDao,
) : GenerationRepository {

    override fun observeJobs(): Flow<List<GenerationJob>> =
        jobDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeJob(id: String): Flow<GenerationJob?> =
        jobDao.observeById(id).map { it?.toDomain() }

    override suspend fun enqueue(
        groupId: String?,
        groupTitle: String?,
        prompt: String,
        referenceUris: List<String>,
    ): String = TODO("Implemented in P2 by the Queue agent")

    override suspend fun retry(id: String) = TODO("Implemented in P2 by the Queue agent")

    override suspend fun cancel(id: String) = TODO("Implemented in P2 by the Queue agent")

    override suspend fun clearFinished() = jobDao.clearFinished()
}
