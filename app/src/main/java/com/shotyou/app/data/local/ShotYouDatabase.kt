package com.shotyou.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TemplateEntity::class,
        GenerationJobEntity::class,
        UsageRecordEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class ShotYouDatabase : RoomDatabase() {
    abstract fun templateDao(): TemplateDao
    abstract fun generationJobDao(): GenerationJobDao
    abstract fun usageDao(): UsageDao

    companion object {
        const val NAME = "shot_you.db"
    }
}
