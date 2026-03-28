package com.rehearsall.playback

import androidx.media3.common.MediaItem
import com.rehearsall.data.repository.LoopRepository
import timber.log.Timber

/**
 * Handles the custom "Toggle Loop" action on the Android Auto Now Playing screen.
 * When the current file has saved loops, toggling activates/deactivates the first loop.
 */
class LoopActionHandler(
    private val loopRepository: LoopRepository,
) {
    companion object {
        const val ACTION_TOGGLE_LOOP = "com.rehearsall.ACTION_TOGGLE_LOOP"
    }

    /**
     * Handles a toggle loop command.
     * Returns the new LoopRegion if loop was activated, null if cleared.
     */
    suspend fun handleToggle(
        currentFileId: Long?,
        currentLoopRegion: LoopRegion?,
    ): LoopRegion? {
        if (currentFileId == null) return null

        return if (currentLoopRegion != null) {
            // Loop is active — clear it
            Timber.d("Auto: clearing loop for file %d", currentFileId)
            null
        } else {
            // Loop is inactive — activate first saved loop for this file
            val loops = loopRepository.getLoopsForFileList(currentFileId)
            val firstLoop = loops.firstOrNull()
            if (firstLoop != null) {
                Timber.d("Auto: activating loop '%s' for file %d", firstLoop.name, currentFileId)
                LoopRegion(firstLoop.startMs, firstLoop.endMs)
            } else {
                Timber.d("Auto: no saved loops for file %d", currentFileId)
                null
            }
        }
    }

    /**
     * Parses loop info from a MediaItem's extras when a loop item is selected from Auto.
     * Returns the LoopRegion if the item represents a loop, null otherwise.
     */
    fun parseLoopFromMediaItem(mediaItem: MediaItem): LoopRegion? {
        val mediaId = mediaItem.mediaId
        if (!mediaId.contains(":loop:")) return null

        val extras = mediaItem.mediaMetadata.extras ?: return null
        val startMs = extras.getLong("loopStartMs", -1)
        val endMs = extras.getLong("loopEndMs", -1)

        return if (startMs >= 0 && endMs > startMs) {
            LoopRegion(startMs, endMs)
        } else {
            null
        }
    }
}
