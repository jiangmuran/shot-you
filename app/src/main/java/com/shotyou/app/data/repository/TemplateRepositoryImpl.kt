package com.shotyou.app.data.repository

import com.shotyou.app.data.local.TemplateDao
import com.shotyou.app.data.local.toDomain
import com.shotyou.app.data.local.toEntity
import com.shotyou.app.domain.ai.AiProviderFactory
import com.shotyou.app.domain.ai.PromptContext
import com.shotyou.app.domain.model.AiOperation
import com.shotyou.app.domain.model.Template
import com.shotyou.app.domain.model.UsageRecord
import com.shotyou.app.domain.repository.SettingsRepository
import com.shotyou.app.domain.repository.TemplateRepository
import com.shotyou.app.domain.repository.UsageRepository
import com.shotyou.app.util.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateRepositoryImpl @Inject constructor(
    private val templateDao: TemplateDao,
    private val settingsRepository: SettingsRepository,
    private val usageRepository: UsageRepository,
    private val providerFactory: AiProviderFactory,
    private val clock: Clock,
) : TemplateRepository {

    override fun observeTemplates(): Flow<List<Template>> =
        templateDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getTemplate(id: Long): Template? =
        templateDao.getById(id)?.toDomain()

    override suspend fun upsert(template: Template): Long {
        val now = clock.now()
        val toSave = template.copy(
            createdAtMs = if (template.createdAtMs == 0L) now else template.createdAtMs,
            updatedAtMs = now,
        )
        return templateDao.upsert(toSave.toEntity())
    }

    override suspend fun delete(id: Long) = templateDao.delete(id)

    override suspend fun optimizePrompt(
        rawPrompt: String,
        groupTitle: String?,
        groupReason: String?,
    ): String {
        val settings = settingsRepository.current()
        val provider = providerFactory.llm(settings)
        val result = provider.optimizePrompt(
            rawPrompt = rawPrompt,
            context = PromptContext(groupTitle = groupTitle, groupReason = groupReason),
        )
        usageRepository.record(
            UsageRecord(
                provider = providerLabel(settings.apiBaseUrl),
                model = provider.model,
                operation = AiOperation.PROMPT_OPTIMIZE,
                promptTokens = result.usage.promptTokens,
                completionTokens = result.usage.completionTokens,
                estimatedCostUsd = result.usage.estimatedCostUsd,
                timestampMs = clock.now(),
            ),
        )
        return result.prompt
    }

    override suspend fun ensureSeeded() {
        if (templateDao.count() > 0) return
        val now = clock.now()
        BuiltInTemplates.all.forEach { (name, prompt, tags) ->
            templateDao.upsert(
                Template(
                    name = name,
                    prompt = prompt,
                    tags = tags,
                    builtIn = true,
                    createdAtMs = now,
                    updatedAtMs = now,
                ).toEntity(),
            )
        }
    }
}

private object BuiltInTemplates {
    val all: List<Triple<String, String, List<String>>> = listOf(
        Triple(
            "Best Portrait",
            "From the reference photos of the same person, produce one flawless portrait: " +
                "keep their true identity and facial features, choose the most natural and " +
                "flattering expression, open and bright eyes, tidy hair, sharp focus, soft " +
                "natural lighting, clean background. Photorealistic.",
            listOf("portrait", "people"),
        ),
        Triple(
            "Group Photo — Everyone's Best",
            "Combine the reference group shots so every person shows their best expression " +
                "(eyes open, natural smile). Preserve each person's identity, clothing and " +
                "position. Even lighting, balanced composition, photorealistic.",
            listOf("group", "people"),
        ),
        Triple(
            "Travel Shot",
            "Merge these similar travel photos into one striking image: best pose of the " +
                "subject, the most scenic framing of the location, golden-hour lighting, " +
                "vivid but natural colors, crisp detail. Photorealistic.",
            listOf("travel", "scenery"),
        ),
        Triple(
            "Clean & Minimal",
            "Recreate the subject from the references with a clean minimal aesthetic: " +
                "simple uncluttered background, soft even lighting, neutral tones, calm " +
                "natural expression. Photorealistic.",
            listOf("minimal", "aesthetic"),
        ),
    )
}
