package com.shotyou.app.data.repository

import com.shotyou.app.domain.model.PhotoGroup
import com.shotyou.app.domain.repository.GroupingRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs VLM grouping over selected photos.
 *
 * P1 skeleton — implemented by the AI-networking agent in P2 (uses PhotoRepository +
 * AiProviderFactory + SettingsRepository + UsageRepository, added via constructor).
 */
@Singleton
class GroupingRepositoryImpl @Inject constructor() : GroupingRepository {
    override suspend fun groupPhotos(uris: List<String>, instruction: String?): List<PhotoGroup> =
        emptyList()
}
