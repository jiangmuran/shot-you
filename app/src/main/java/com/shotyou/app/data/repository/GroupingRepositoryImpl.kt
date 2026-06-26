package com.shotyou.app.data.repository

import com.shotyou.app.domain.ai.AiProviderFactory
import com.shotyou.app.domain.ai.VlmGroup
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
 * Classifies selected photos with the VLM. Large selections are processed with a
 * **sliding window** (size = `settings.vlmBatchSize`) so we never exceed the model's
 * per-request image limit. Consecutive windows **overlap**, and clusters that share any
 * photo across windows are merged (union-find) — so a group is never split across two
 * requests. Each group carries a category and a "recommended" flag from the VLM.
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
        val distinctUris = uris.distinct()
        if (distinctUris.isEmpty()) return emptyList()

        val settings = settingsRepository.current()
        val provider = providerFactory.vlm(settings)
        val providerLabel = hostLabel(settings.apiBaseUrl)

        // Ask the VLM to write titles/reasons in the app language.
        val langDirective = if (com.shotyou.app.util.LangUtil.isChinese()) {
            "Write each group's title and reason in Simplified Chinese."
        } else {
            null
        }
        val effectiveInstruction = listOfNotNull(instruction?.takeIf { it.isNotBlank() }, langDirective)
            .joinToString("\n").takeIf { it.isNotBlank() }

        val windowSize = settings.vlmBatchSize.coerceAtLeast(2)
        val overlap = (windowSize / 4).coerceIn(1, windowSize - 1)

        // Union-find over uris.
        val parent = HashMap<String, String>().apply { distinctUris.forEach { put(it, it) } }
        fun find(x: String): String {
            var r = x
            while (parent[r] != r) r = parent[r]!!
            var c = x
            while (parent[c] != c) { val n = parent[c]!!; parent[c] = r; c = n }
            return r
        }
        fun union(a: String, b: String) { parent[find(a)] = find(b) }

        val contributing = mutableListOf<VlmGroup>()
        var promptTokens = 0
        var completionTokens = 0

        for (window in slidingWindows(distinctUris.size, windowSize, overlap)) {
            val windowUris = distinctUris.subList(window.first, window.last + 1)
            val images = windowUris.map { photoRepository.loadAiImage(it) }
            val result = provider.groupSimilar(images, effectiveInstruction)
            promptTokens += result.usage.promptTokens
            completionTokens += result.usage.completionTokens

            result.groups.forEach { g ->
                val members = g.memberIds.filter { parent.containsKey(it) }
                if (members.isNotEmpty()) {
                    contributing += g.copy(memberIds = members)
                    members.drop(1).forEach { union(members.first(), it) }
                }
            }
        }

        // Assemble final groups by union-find root, preserving original photo order.
        val rootToUris = LinkedHashMap<String, MutableList<String>>()
        distinctUris.forEach { rootToUris.getOrPut(find(it)) { mutableListOf() }.add(it) }

        val bestMetaByRoot = HashMap<String, VlmGroup>()
        val refsByRoot = HashMap<String, LinkedHashSet<String>>()
        contributing.forEach { g ->
            val root = find(g.memberIds.first())
            val cur = bestMetaByRoot[root]
            if (cur == null || g.memberIds.size > cur.memberIds.size) bestMetaByRoot[root] = g
            refsByRoot.getOrPut(root) { LinkedHashSet() }.addAll(g.referenceIds)
        }

        val finalGroups = rootToUris.entries.mapIndexed { index, (root, groupUris) ->
            val meta = bestMetaByRoot[root]
            val refs = (refsByRoot[root]?.filter { it in groupUris } ?: emptyList())
                .ifEmpty { groupUris.take(1) }
            PhotoGroup(
                id = UUID.randomUUID().toString(),
                title = meta?.title?.takeIf { it.isNotBlank() } ?: "Group ${index + 1}",
                reason = meta?.reason.orEmpty(),
                photoUris = groupUris,
                referenceUris = refs,
                category = meta?.category,
                recommended = meta?.recommended ?: true,
            )
        }.ifEmpty {
            listOf(
                PhotoGroup(
                    id = UUID.randomUUID().toString(),
                    title = "All photos",
                    reason = "The model did not propose any groups, so all photos were kept together.",
                    photoUris = distinctUris,
                    referenceUris = distinctUris.take(1),
                ),
            )
        }

        usageRepository.record(
            UsageRecord(
                provider = providerLabel,
                model = provider.model,
                operation = AiOperation.GROUPING,
                promptTokens = promptTokens,
                completionTokens = completionTokens,
                imageCount = 0, // grouping analyzes input photos (billed via tokens), generates none
                timestampMs = clock.now(),
            ),
        )

        return finalGroups
    }

    /** Overlapping inclusive index ranges covering all n photos. */
    private fun slidingWindows(n: Int, size: Int, overlap: Int): List<IntRange> {
        if (n <= size) return listOf(0..(n - 1))
        val step = (size - overlap).coerceAtLeast(1)
        val ranges = mutableListOf<IntRange>()
        var start = 0
        while (start < n) {
            val end = minOf(start + size, n)
            ranges += start..(end - 1)
            if (end >= n) break
            start += step
        }
        return ranges
    }

    /** Short usage label derived from the configured API host, e.g. "api.openai.com". */
    private fun hostLabel(apiBaseUrl: String): String =
        runCatching { java.net.URI(apiBaseUrl.trim()).host }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "openai"
}
