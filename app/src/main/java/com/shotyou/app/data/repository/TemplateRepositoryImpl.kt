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
        val set = if (isChineseLocale()) BuiltInTemplates.zh else BuiltInTemplates.en
        set.forEach { (name, prompt, tags) ->
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

    /** True when the active app/device language is Chinese. Prefers the per-app locale. */
    private fun isChineseLocale(): Boolean {
        val appLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
        val tag = if (!appLocales.isEmpty) appLocales[0]?.language else null
        val language = tag ?: java.util.Locale.getDefault().language
        return language == "zh"
    }
}

private object BuiltInTemplates {
    val en: List<Triple<String, String, List<String>>> = listOf(
        Triple(
            "Pro Portrait Retouch",
            "Studio-quality portrait of the same person; preserve their exact identity and features. " +
                "Flawless even-toned skin that keeps natural texture and pores (never plastic), subtle " +
                "brightening, bright clear eyes, neat lashes and brows, gently refined nose and lips, " +
                "light natural makeup. 85mm lens look, soft directional light, gentle bokeh, vertical " +
                "orientation. Beautiful yet completely believable — not over-processed. Photorealistic.",
            listOf("portrait", "retouch", "people"),
        ),
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

    val zh: List<Triple<String, String, List<String>>> = listOf(
        Triple(
            "专业人像精修",
            "为同一个人生成影楼级人像,严格保留其真实身份与五官特征。肤色均匀无瑕但保留自然肤质与毛孔" +
                "(绝不塑料感),适度提亮,双眼明亮清澈,睫毛与眉形干净,鼻形与唇形微调更精致,淡雅自然" +
                "妆容。85mm 镜头质感,柔和定向光,自然背景虚化,竖向构图。美而真实可信,不过度处理。写实风格。",
            listOf("人像", "精修", "人物"),
        ),
        Triple(
            "最佳人像",
            "根据同一个人的参考照片，生成一张无瑕的人像：" +
                "保留其真实身份与面部特征，选择最自然、最讨喜的表情，" +
                "双眼明亮有神，发丝整齐，对焦清晰，柔和的自然光，背景干净。写实风格。",
            listOf("人像", "人物"),
        ),
        Triple(
            "合影优选",
            "将参考合影融合在一起，让每个人都展现最佳表情（睁眼、自然微笑）。" +
                "保留每个人的身份、服装与站位。光线均匀，构图平衡，写实风格。",
            listOf("合影", "人物"),
        ),
        Triple(
            "旅行大片",
            "把这些相似的旅行照片合成一张出彩的画面：人物呈现最佳姿态，" +
                "取景最具风景之美，黄金时刻的光线，色彩鲜艳又自然，细节清晰锐利。写实风格。",
            listOf("旅行", "风景"),
        ),
        Triple(
            "干净简约",
            "依据参考照片重塑主体，呈现干净简约的美感：" +
                "背景简洁不杂乱，光线柔和均匀，色调中性，表情平和自然。写实风格。",
            listOf("简约", "美学"),
        ),
    )
}
