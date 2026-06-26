package com.shotyou.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY updatedAtMs DESC")
    fun observeAll(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getById(id: Long): TemplateEntity?

    @Upsert
    suspend fun upsert(entity: TemplateEntity): Long

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun count(): Int
}

@Dao
interface GenerationJobDao {
    @Query("SELECT * FROM generation_jobs ORDER BY createdAtMs DESC")
    fun observeAll(): Flow<List<GenerationJobEntity>>

    @Query("SELECT * FROM generation_jobs WHERE id = :id")
    fun observeById(id: String): Flow<GenerationJobEntity?>

    @Query("SELECT * FROM generation_jobs WHERE batchId = :batchId ORDER BY variantIndex ASC")
    fun observeByBatch(batchId: String): Flow<List<GenerationJobEntity>>

    @Query("SELECT * FROM generation_jobs WHERE id = :id")
    suspend fun getById(id: String): GenerationJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: GenerationJobEntity)

    @Update
    suspend fun update(entity: GenerationJobEntity)

    @Query("DELETE FROM generation_jobs WHERE status IN ('SUCCEEDED','FAILED','CANCELLED')")
    suspend fun clearFinished()
}

@Dao
interface UsageDao {
    @Query("SELECT * FROM usage_records ORDER BY timestampMs DESC")
    fun observeAll(): Flow<List<UsageRecordEntity>>

    @Insert
    suspend fun insert(entity: UsageRecordEntity)

    @Query("DELETE FROM usage_records")
    suspend fun clear()
}
