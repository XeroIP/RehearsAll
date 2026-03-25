package com.rehearsall.playback

import androidx.media3.common.MediaItem
import com.rehearsall.data.db.dao.AudioFileDao
import com.rehearsall.data.db.dao.LoopDao
import com.rehearsall.data.db.dao.PlaylistDao
import com.rehearsall.data.db.dao.PlaylistItemDao

/**
 * Builds the content tree for Android Auto media browsing.
 *
 * Tree structure:
 *   root -> Recent, All Files, Playlists
 *   recent -> last 20 played files (playable)
 *   all_files -> files without loops (playable), files with loops (browsable)
 *   file:{id} -> "Full Track" (playable) + saved loops (playable)
 *   playlists -> all playlists (browsable)
 *   playlist:{id} -> files in playlist (playable)
 */
class ContentTreeBuilder(
    private val audioFileDao: AudioFileDao,
    private val playlistDao: PlaylistDao,
    private val playlistItemDao: PlaylistItemDao,
    private val loopDao: LoopDao,
) {
    companion object {
        const val ROOT_ID = "root"
        const val RECENT_ID = "recent"
        const val ALL_FILES_ID = "all_files"
        const val PLAYLISTS_ID = "playlists"
    }

    suspend fun getRoot(): List<MediaItem> {
        return listOf(
            MediaItemMapper.browsableFolder(RECENT_ID, "Recent"),
            MediaItemMapper.browsableFolder(ALL_FILES_ID, "All Files"),
            MediaItemMapper.browsableFolder(PLAYLISTS_ID, "Playlists"),
        )
    }

    suspend fun getChildren(parentId: String): List<MediaItem> {
        return when (parentId) {
            ROOT_ID -> getRoot()
            RECENT_ID -> getRecentFiles()
            ALL_FILES_ID -> getAllFiles()
            PLAYLISTS_ID -> getPlaylists()
            else -> {
                // file:{id} or playlist:{id}
                when {
                    parentId.startsWith("playlist:") -> {
                        val playlistId = parentId.removePrefix("playlist:").toLongOrNull()
                        if (playlistId != null) getPlaylistTracks(playlistId) else emptyList()
                    }
                    parentId.startsWith("file:") -> {
                        val fileId = parentId.removePrefix("file:").toLongOrNull()
                        if (fileId != null) getFileWithLoops(fileId) else emptyList()
                    }
                    else -> emptyList()
                }
            }
        }
    }

    suspend fun getItem(mediaId: String): MediaItem? {
        return when {
            mediaId.startsWith("file:") && mediaId.contains(":loop:") -> {
                // file:{fileId}:loop:{loopId}
                val parts = mediaId.split(":")
                val fileId = parts.getOrNull(1)?.toLongOrNull() ?: return null
                val loopId = parts.getOrNull(3)?.toLongOrNull() ?: return null
                val file = audioFileDao.getById(fileId) ?: return null
                val loop = loopDao.getById(loopId) ?: return null
                MediaItemMapper.loopItem(file, loop)
            }
            mediaId.startsWith("file:") && mediaId.endsWith(":full") -> {
                val fileId = mediaId.removePrefix("file:").removeSuffix(":full").toLongOrNull() ?: return null
                val file = audioFileDao.getById(fileId) ?: return null
                MediaItemMapper.fullTrackItem(file)
            }
            mediaId.startsWith("file:") -> {
                val fileId = mediaId.removePrefix("file:").toLongOrNull() ?: return null
                val file = audioFileDao.getById(fileId) ?: return null
                MediaItemMapper.playableFile(file)
            }
            else -> null
        }
    }

    suspend fun search(query: String): List<MediaItem> {
        val lowerQuery = query.lowercase()
        val results = mutableListOf<MediaItem>()

        // Search files
        val files = audioFileDao.getAllList()
        files.filter {
            it.displayName.lowercase().contains(lowerQuery) ||
                it.artist?.lowercase()?.contains(lowerQuery) == true
        }.take(10).forEach { file ->
            results.add(MediaItemMapper.playableFile(file))
        }

        // Search playlists
        val playlists = playlistDao.getAllList()
        playlists.filter {
            it.name.lowercase().contains(lowerQuery)
        }.take(5).forEach { playlist ->
            results.add(MediaItemMapper.playablePlaylist(playlist))
        }

        return results
    }

    private suspend fun getRecentFiles(): List<MediaItem> {
        return audioFileDao.getRecentList(20).map { file ->
            MediaItemMapper.playableFile(file)
        }
    }

    private suspend fun getAllFiles(): List<MediaItem> {
        val files = audioFileDao.getAllList()
        return files.map { file ->
            val loops = loopDao.getAllForFileList(file.id)
            if (loops.isNotEmpty()) {
                MediaItemMapper.browsableFileWithLoops(file)
            } else {
                MediaItemMapper.playableFile(file)
            }
        }
    }

    private suspend fun getFileWithLoops(fileId: Long): List<MediaItem> {
        val file = audioFileDao.getById(fileId) ?: return emptyList()
        val loops = loopDao.getAllForFileList(fileId)
        val items = mutableListOf<MediaItem>()
        items.add(MediaItemMapper.fullTrackItem(file))
        loops.forEach { loop ->
            items.add(MediaItemMapper.loopItem(file, loop))
        }
        return items
    }

    private suspend fun getPlaylists(): List<MediaItem> {
        return playlistDao.getAllList().map { playlist ->
            MediaItemMapper.playablePlaylist(playlist)
        }
    }

    private suspend fun getPlaylistTracks(playlistId: Long): List<MediaItem> {
        val items = playlistItemDao.getItemsForPlaylist(playlistId)
        return items.mapNotNull { playlistItem ->
            val file = audioFileDao.getById(playlistItem.audioFileId)
            file?.let { MediaItemMapper.playlistTrackItem(it, playlistId) }
        }
    }
}
