package com.shotyou.app.data.repository

import com.shotyou.app.domain.ai.AiProviderFactory
import com.shotyou.app.domain.model.AiOperation
import com.shotyou.app.domain.model.PhotoGroup
import com.shotyou.app.domain.model.UsageRecord
import com.shotyou.app.domain.repository.GroupingRepository
import com.shotyou.app.domain.repository.PhotoRepository
import com.shotyou.app.domain.repository.SettingsRepository
import com.shotyou.app.domain.repository.UsageRepository
import com.shotyou.app.util.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs VLM grouping over selected photos: decodes each uri to bytes, asks the configured VLM
 * to cluster near-duplicates, maps the result back to [PhotoGroup]s, and records usage.
 */
@Singleton
class GroupingRepositoryImpl @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val providerFactory: AiProviderFactory,
    private val settingsRepository: SettingsRepository,
    private val usageRepository: UsageRepository,
    private val clock: Clock,
) : GroupingRepository {

    override suspend fun groupPhotos(uris: List<String>, instruction: String?): List<PhotoGroup> {
        if (uris.isEmpty()) return emptyList()

        val settings = settingsRepository.current()
        val provider = providerFactory.vlm(settings)

        val images = uris.map { photoRepository.loadAiImage(it) }
        val result = provider.groupSimilar(images, instruction)

        val groups = result.groups.mapIndexed { index, g ->
            val photoUris = g.memberIds
            val referenceUris = g.referenceIds.ifEmpty { photoUris.take(1) }
            PhotoGroup(
                id = UUID.randomUUID().toString(),
                title = g.title.ifBlank { "Group ${index + 1}" },
                reason = g.reason,
                photoUris = photoUris,
                referenceUris = referenceUris,
            )
        }.filter { it.photoUris.isNotEmpty() }

        val finalGroups = groups.ifEmpty {
            listOf(
                PhotoGroup(
                    id = UUID.randomUUID().toString(),
                    title = "All photos",
                    reason = "The model did not propose any groups, so all photos were kept together.",
                    photoUris = uris,
                    referenceUris = uris.take(1),
                ),
            )
        }

        usageRepository.record(
            UsageRecord(
                provider = settings.vlmProvider.name,
                model = provider.model,
                operation = AiOperation.GROUPING,
                promptTokens = result.usage.promptTokens,
                completionTokens = result.usage.completionTokens,
                imageCount = result.usage.imageCount,
                estimatedCostUsd = result.usage.estimatedCostUsd,
                timestampMs = clock.now(),
            ),
        )

        return finalGroups
    }
}
