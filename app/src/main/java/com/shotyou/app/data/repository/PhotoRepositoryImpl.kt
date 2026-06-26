package com.shotyou.app.data.repository

import android.content.Context
import com.shotyou.app.domain.ai.AiImage
import com.shotyou.app.domain.model.Photo
import com.shotyou.app.domain.repository.PhotoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads photos from MediaStore and decodes bytes for AI calls.
 *
 * P1 skeleton — implemented by the Photo/Library agent in P2. Constructor deps may be
 * added freely (Hilt-injected); the @Binds in RepositoryModule stays valid.
 */
@Singleton
class PhotoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : PhotoRepository {

    override suspend fun queryImages(limit: Int): List<Photo> = emptyList()

    override suspend fun loadAiImage(uri: String, maxEdge: Int): AiImage =
        TODO("Implemented in P2 by the Photo agent")

    override suspend fun saveGeneratedImage(
        bytes: ByteArray,
        mimeType: String,
        displayName: String,
    ): String = TODO("Implemented in P2 by the Photo agent")
}
