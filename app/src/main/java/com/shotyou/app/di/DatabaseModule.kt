package com.shotyou.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shotyou.app.data.local.GenerationJobDao
import com.shotyou.app.data.local.SessionDao
import com.shotyou.app.data.local.ShotYouDatabase
import com.shotyou.app.data.local.TemplateDao
import com.shotyou.app.data.local.UsageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE generation_jobs ADD COLUMN batchId TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE generation_jobs ADD COLUMN variantIndex INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE generation_jobs ADD COLUMN variantLabel TEXT")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS sessions (" +
                    "id TEXT NOT NULL PRIMARY KEY, " +
                    "stage TEXT NOT NULL, " +
                    "photoUris TEXT NOT NULL, " +
                    "groupsJson TEXT NOT NULL, " +
                    "error TEXT, " +
                    "createdAtMs INTEGER NOT NULL, " +
                    "updatedAtMs INTEGER NOT NULL)",
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ShotYouDatabase =
        Room.databaseBuilder(context, ShotYouDatabase::class.java, ShotYouDatabase.NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration() // safety net for any other version jump
            .build()

    @Provides
    fun provideTemplateDao(db: ShotYouDatabase): TemplateDao = db.templateDao()

    @Provides
    fun provideGenerationJobDao(db: ShotYouDatabase): GenerationJobDao = db.generationJobDao()

    @Provides
    fun provideUsageDao(db: ShotYouDatabase): UsageDao = db.usageDao()

    @Provides
    fun provideSessionDao(db: ShotYouDatabase): SessionDao = db.sessionDao()
}
