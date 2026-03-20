package com.rehearsall.ui.filelist

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rehearsall.data.audio.AudioImporter
import com.rehearsall.data.repository.AudioFileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FileListViewModel @Inject constructor(
    private val repository: AudioFileRepository,
    private val importer: AudioImporter,
) : ViewModel() {

    private val _uiState = MutableStateFlow<FileListUiState>(FileListUiState.Loading)
    val uiState: StateFlow<FileListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<FileListEvent>()
    val events: SharedFlow<FileListEvent> = _events.asSharedFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllFiles()
                .map<_, FileListUiState> { files -> FileListUiState.Loaded(files) }
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

    fun deleteFile(id: Long) {
        viewModelScope.launch {
            val file = repository.getById(id) ?: return@launch

            // Delete internal audio file
            try {
                File(file.internalPath).delete()
            } catch (e: Exception) {
                Timber.w(e, "Failed to delete audio file: %s", file.internalPath)
            }

            // Delete waveform cache if it exists
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

    fun renameFile(id: Long, newName: String) {
        viewModelScope.launch {
            repository.updateDisplayName(id, newName.trim())
            _events.emit(FileListEvent.RenameSuccess(newName.trim()))
        }
    }
}
