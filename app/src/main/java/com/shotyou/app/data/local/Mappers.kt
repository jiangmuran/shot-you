package com.shotyou.app.data.local

import com.shotyou.app.domain.model.AiOperation
import com.shotyou.app.domain.model.GenerationJob
import com.shotyou.app.domain.model.JobStatus
import com.shotyou.app.domain.model.Template
import com.shotyou.app.domain.model.UsageRecord

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
