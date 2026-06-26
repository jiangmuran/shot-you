package com.shotyou.app.data.repository

import com.shotyou.app.data.local.UsageDao
import com.shotyou.app.data.local.toDomain
import com.shotyou.app.data.local.toEntity
import com.shotyou.app.domain.model.UsageRecord
import com.shotyou.app.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepositoryImpl @Inject constructor(
    private val usageDao: UsageDao,
) : UsageRepository {

    override fun observeRecords(): Flow<List<UsageRecord>> =
        usageDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun record(record: UsageRecord) {
        usageDao.insert(record.toEntity())
    }

    override suspend fun clear() {
        usageDao.clear()
    }
}
