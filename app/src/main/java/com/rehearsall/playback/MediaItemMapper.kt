package com.rehearsall.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.rehearsall.data.db.entity.AudioFileEntity
import com.rehearsall.data.db.entity.LoopEntity
import com.rehearsall.data.db.entity.PlaylistEntity

/**
 * Builds MediaItems for the Android Auto content tree.
 * Browsable items let the user navigate deeper; playable items start playback.
 */
object MediaItemMapper {
    fun browsableFolder(
        mediaId: String,
        title: String,
    ): MediaItem {
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build(),
            )
            .build()
    }

    fun playableFile(file: AudioFileEntity): MediaItem {
        return MediaItem.Builder()
            .setMediaId("file:${file.id}")
            .setUri(file.internalPath)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(file.displayName)
                    .setArtist(file.artist)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setExtras(
                        Bundle().apply {
                            putLong("fileId", file.id)
                            putLong("durationMs", file.durationMs)
                        },
                    )
                    .build(),
            )
            .build()
    }

    fun browsableFileWithLoops(file: AudioFileEntity): MediaItem {
        return MediaItem.Builder()
            .setMediaId("file:${file.id}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(file.displayName)
                    .setArtist(file.artist)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .setExtras(
                        Bundle().apply {
                            putLong("fileId", file.id)
                        },
                    )
                    .build(),
            )
            .build()
    }

    fun fullTrackItem(file: AudioFileEntity): MediaItem {
        return MediaItem.Builder()
            .setMediaId("file:${file.id}:full")
            .setUri(file.internalPath)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Full Track")
                    .setArtist(file.displayName)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setExtras(
                        Bundle().apply {
                            putLong("fileId", file.id)
                        },
                    )
                    .build(),
            )
            .build()
    }

    fun loopItem(
        file: AudioFileEntity,
        loop: LoopEntity,
    ): MediaItem {
        val durationSec = (loop.endMs - loop.startMs) / 1000
        return MediaItem.Builder()
            .setMediaId("file:${file.id}:loop:${loop.id}")
            .setUri(file.internalPath)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(loop.name)
                    .setArtist("${formatMs(loop.startMs)} – ${formatMs(loop.endMs)}")
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setExtras(
                        Bundle().apply {
                            putLong("fileId", file.id)
                            putLong("loopId", loop.id)
                            putLong("loopStartMs", loop.startMs)
                            putLong("loopEndMs", loop.endMs)
                        },
                    )
                    .build(),
            )
            .build()
    }

    fun playablePlaylist(playlist: PlaylistEntity): MediaItem {
        return MediaItem.Builder()
            .setMediaId("playlist:${playlist.id}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(playlist.name)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_PLAYLIST)
                    .build(),
            )
            .build()
    }

    fun playlistTrackItem(
        file: AudioFileEntity,
        playlistId: Long,
    ): MediaItem {
        return MediaItem.Builder()
            .setMediaId("playlist:$playlistId:file:${file.id}")
            .setUri(file.internalPath)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(file.displayName)
                    .setArtist(file.artist)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setExtras(
                        Bundle().apply {
                            putLong("fileId", file.id)
                        },
                    )
                    .build(),
            )
            .build()
    }

    private fun formatMs(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%d:%02d".format(min, sec)
    }
}
