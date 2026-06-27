package com.shotyou.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val prompt: String,
    val tags: List<String> = emptyList(),
    val builtIn: Boolean = false,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

@Entity(tableName = "generation_jobs")
data class GenerationJobEntity(
    @PrimaryKey val id: String,
    val batchId: String = "",
    val variantIndex: Int = 0,
    val variantLabel: String? = null,
    val groupId: String?,
    val groupTitle: String?,
    val prompt: String,
    val referenceUris: List<String>,
    val status: String,
    val resultUri: String?,
    val errorMessage: String?,
    val provider: String,
    val model: String,
    val attempt: Int,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val stage: String,
    val photoUris: List<String>,
    val groupsJson: String,
    val error: String?,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

@Entity(tableName = "usage_records")
data class UsageRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val provider: String,
    val model: String,
    val operation: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val imageCount: Int,
    val estimatedCostUsd: Double,
    val success: Boolean,
    val timestampMs: Long,
)
