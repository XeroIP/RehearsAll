package com.rehearsall.ui.recents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rehearsall.data.repository.AudioFileRepository
import com.rehearsall.domain.model.AudioFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface RecentsUiState {
    data object Loading : RecentsUiState
    data class Loaded(val files: List<AudioFile>) : RecentsUiState
    data class Error(val message: String) : RecentsUiState
}

@HiltViewModel
class RecentsViewModel
    @Inject
    constructor(
        private val repository: AudioFileRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<RecentsUiState>(RecentsUiState.Loading)
        val uiState: StateFlow<RecentsUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                repository.getRecentFiles()
                    .catch { e ->
                        Timber.e(e, "Error loading recent files")
                        emit(emptyList())
                    }
                    .collect { files ->
                        _uiState.value = RecentsUiState.Loaded(files = files)
                    }
            }
        }
    }
