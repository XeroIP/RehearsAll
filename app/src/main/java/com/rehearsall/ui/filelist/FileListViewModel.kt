package com.rehearsall.ui.filelist

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rehearsall.data.audio.AudioImporter
import com.rehearsall.data.repository.AudioFileRepository
import com.rehearsall.data.repository.PlaylistRepository
import com.rehearsall.domain.model.QueueItem
import com.rehearsall.playback.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FileListViewModel
    @Inject
    constructor(
        private val repository: AudioFileRepository,
        private val playlistRepository: PlaylistRepository,
        private val importer: AudioImporter,
        private val playbackManager: PlaybackManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<FileListUiState>(FileListUiState.Loading)
        val uiState: StateFlow<FileListUiState> = _uiState.asStateFlow()

        private val _events = MutableSharedFlow<FileListEvent>()
        val events: SharedFlow<FileListEvent> = _events.asSharedFlow()

        private val _isImporting = MutableStateFlow(false)
        val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

        init {
            viewModelScope.launch {
                combine(
                    repository.getAllFiles(),
                    playlistRepository.getAllPlaylists(),
                ) { files, playlists ->
                    FileListUiState.Loaded(files = files, playlists = playlists) as FileListUiState
                }
                    .catch { e ->
                        Timber.e(e, "Error loading files")
                        emit(FileListUiState.Error("Failed to load files"))
                    }
                    .collect { _uiState.value = it }
            }
        }

        fun importFile(uri: Uri) {
            viewModelScope.launch {
                _isImporting.value = true
                val result = importer.import(uri)
                _isImporting.value = false

                result.fold(
                    onSuccess = { audioFile ->
                        _events.emit(FileListEvent.ImportSuccess(audioFile.displayName))
                    },
                    onFailure = { error ->
                        val message = error.message ?: "Import failed"
                        _events.emit(FileListEvent.ImportError(message))
                    },
                )
            }
        }

        fun importFiles(uris: List<Uri>) {
            if (uris.isEmpty()) return
            if (uris.size == 1) {
                importFile(uris.first())
                return
            }
            viewModelScope.launch {
                _isImporting.value = true
                val semaphore = Semaphore(3)
                val results = uris.map { uri ->
                    async {
                        semaphore.withPermit {
                            importer.import(uri)
                        }
                    }
                }.awaitAll()

                var successCount = 0
                var errorCount = 0
                results.forEach { result ->
                    result.fold(
                        onSuccess = { successCount++ },
                        onFailure = { error ->
                            errorCount++
                            Timber.w("Batch import failed: %s", error.message)
                        },
                    )
                }
                _isImporting.value = false
                if (successCount > 0) {
                    _events.emit(FileListEvent.ImportBatchComplete(successCount))
                }
                if (errorCount > 0) {
                    _events.emit(FileListEvent.ImportError("$errorCount file(s) failed to import"))
                }
            }
        }

        fun toggleFileSelection(id: Long) {
            val state = _uiState.value as? FileListUiState.Loaded ?: return
            val newSelected =
                if (id in state.selectedFileIds) {
                    state.selectedFileIds - id
                } else {
                    state.selectedFileIds + id
                }
            _uiState.value = state.copy(selectedFileIds = newSelected)
        }

        fun clearSelection() {
            val state = _uiState.value as? FileListUiState.Loaded ?: return
            _uiState.value = state.copy(selectedFileIds = emptySet())
        }

        fun addSelectedToPlaylist(playlistId: Long) {
            val state = _uiState.value as? FileListUiState.Loaded ?: return
            val selectedIds = state.selectedFileIds.toList()
            if (selectedIds.isEmpty()) return
            viewModelScope.launch {
                val playlist = playlistRepository.getById(playlistId)
                for (fileId in selectedIds) {
                    playlistRepository.addFileToPlaylist(playlistId, fileId)
                }
                clearSelection()
                _events.emit(
                    FileListEvent.AddedBatchToPlaylist(
                        count = selectedIds.size,
                        playlistName = playlist?.name ?: "Playlist",
                    ),
                )
            }
        }

        fun deleteFile(id: Long) {
            viewModelScope.launch {
                val file = repository.getById(id) ?: return@launch

                try {
                    File(file.internalPath).delete()
                } catch (e: Exception) {
                    Timber.w(e, "Failed to delete audio file: %s", file.internalPath)
                }

                try {
                    val waveformDir = File(file.internalPath).parentFile?.parentFile
                    waveformDir?.let {
                        File(it, "waveforms/${file.id}.waveform").delete()
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to delete waveform cache for id=%d", file.id)
                }

                repository.delete(id)
                _events.emit(FileListEvent.DeleteSuccess(file.displayName))
            }
        }

        fun renameFile(
            id: Long,
            newName: String,
        ) {
            viewModelScope.launch {
                repository.updateDisplayName(id, newName.trim())
                _events.emit(FileListEvent.RenameSuccess(newName.trim()))
            }
        }

        fun createPlaylist(name: String) {
            viewModelScope.launch {
                val id = playlistRepository.createPlaylist(name.trim())
                _events.emit(FileListEvent.PlaylistCreated(name.trim(), id))
            }
        }

        fun addFileToPlaylist(
            audioFileId: Long,
            playlistId: Long,
        ) {
            viewModelScope.launch {
                val file = repository.getById(audioFileId)
                val playlist = playlistRepository.getById(playlistId)
                playlistRepository.addFileToPlaylist(playlistId, audioFileId)
                _events.emit(
                    FileListEvent.AddedToPlaylist(
                        fileName = file?.displayName ?: "File",
                        playlistName = playlist?.name ?: "Playlist",
                    ),
                )
            }
        }

        fun playAll() {
            val state = _uiState.value as? FileListUiState.Loaded ?: return
            if (state.files.isEmpty()) return
            val queueItems =
                state.files.map { file ->
                    QueueItem(
                        fileId = file.id,
                        displayName = file.displayName,
                        artist = file.artist,
                        durationMs = file.durationMs,
                        path = file.internalPath,
                    )
                }
            playbackManager.setQueue(queueItems)
        }
    }
