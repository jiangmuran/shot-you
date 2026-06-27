package com.shotyou.app.data.local

import com.shotyou.app.domain.model.AiOperation
import com.shotyou.app.domain.model.GenerationJob
import com.shotyou.app.domain.model.GenerationSession
import com.shotyou.app.domain.model.JobStatus
import com.shotyou.app.domain.model.PhotoGroup
import com.shotyou.app.domain.model.SessionStage
import com.shotyou.app.domain.model.Template
import com.shotyou.app.domain.model.UsageRecord
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

fun TemplateEntity.toDomain() = Template(
    id = id,
    name = name,
    prompt = prompt,
    tags = tags,
    builtIn = builtIn,
    createdAtMs = createdAtMs,
    updatedAtMs = updatedAtMs,
)

fun Template.toEntity() = TemplateEntity(
    id = id,
    name = name,
    prompt = prompt,
    tags = tags,
    builtIn = builtIn,
    createdAtMs = createdAtMs,
    updatedAtMs = updatedAtMs,
)

fun GenerationJobEntity.toDomain() = GenerationJob(
    id = id,
    batchId = batchId,
    variantIndex = variantIndex,
    variantLabel = variantLabel,
    groupId = groupId,
    groupTitle = groupTitle,
    prompt = prompt,
    referenceUris = referenceUris,
    status = runCatching { JobStatus.valueOf(status) }.getOrDefault(JobStatus.QUEUED),
    resultUri = resultUri,
    errorMessage = errorMessage,
    provider = provider,
    model = model,
    attempt = attempt,
    createdAtMs = createdAtMs,
    updatedAtMs = updatedAtMs,
)

fun GenerationJob.toEntity() = GenerationJobEntity(
    id = id,
    batchId = batchId,
    variantIndex = variantIndex,
    variantLabel = variantLabel,
    groupId = groupId,
    groupTitle = groupTitle,
    prompt = prompt,
    referenceUris = referenceUris,
    status = status.name,
    resultUri = resultUri,
    errorMessage = errorMessage,
    provider = provider,
    model = model,
    attempt = attempt,
    createdAtMs = createdAtMs,
    updatedAtMs = updatedAtMs,
)

fun UsageRecordEntity.toDomain() = UsageRecord(
    id = id,
    provider = provider,
    model = model,
    operation = runCatching { AiOperation.valueOf(operation) }.getOrDefault(AiOperation.GROUPING),
    promptTokens = promptTokens,
    completionTokens = completionTokens,
    imageCount = imageCount,
    estimatedCostUsd = estimatedCostUsd,
    success = success,
    timestampMs = timestampMs,
)

private val sessionJson = Json { ignoreUnknownKeys = true }
private val groupListSerializer = ListSerializer(PhotoGroup.serializer())

fun SessionEntity.toDomain() = GenerationSession(
    id = id,
    stage = runCatching { SessionStage.valueOf(stage) }.getOrDefault(SessionStage.FAILED),
    photoUris = photoUris,
    groups = if (groupsJson.isBlank()) emptyList()
    else runCatching { sessionJson.decodeFromString(groupListSerializer, groupsJson) }.getOrDefault(emptyList()),
    error = error,
    createdAtMs = createdAtMs,
    updatedAtMs = updatedAtMs,
)

fun GenerationSession.toEntity() = SessionEntity(
    id = id,
    stage = stage.name,
    photoUris = photoUris,
    groupsJson = sessionJson.encodeToString(groupListSerializer, groups),
    error = error,
    createdAtMs = createdAtMs,
    updatedAtMs = updatedAtMs,
)

fun UsageRecord.toEntity() = UsageRecordEntity(
    id = id,
    provider = provider,
    model = model,
    operation = operation.name,
    promptTokens = promptTokens,
    completionTokens = completionTokens,
    imageCount = imageCount,
    estimatedCostUsd = estimatedCostUsd,
    success = success,
    timestampMs = timestampMs,
)
