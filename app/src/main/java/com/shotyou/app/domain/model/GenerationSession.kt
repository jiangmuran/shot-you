package com.shotyou.app.domain.model

/** Lifecycle stage of a "session" — one batch of photos the user kicked off. */
enum class SessionStage {
    CLASSIFYING,        // VLM classification running in the background
    READY_FOR_REVIEW,   // classification done — waiting for the user to curate/decide
    GENERATING,         // user started generation; candidate batches are running
    DONE,               // everything finished
    FAILED,             // classification failed
}

/**
 * A whole run the user started from the Library: the selected photos, the classification
 * result (groups), and which stage it's at. Lets classification happen in the background
 * (in the queue) instead of blocking the UI.
 */
data class GenerationSession(
    val id: String,
    val stage: SessionStage,
    val photoUris: List<String>,
    val groups: List<PhotoGroup> = emptyList(),
    val error: String? = null,
    val createdAtMs: Long = 0L,
    val updatedAtMs: Long = 0L,
) {
    val photoCount: Int get() = photoUris.size
    val groupCount: Int get() = groups.size
}
