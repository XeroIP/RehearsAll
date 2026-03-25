package com.rehearsall.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rehearsall.data.preferences.UserPreferencesRepository
import com.rehearsall.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val skipIncrementMs: Long = 5000L,
    val loopCrossfade: Boolean = true,
)

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val prefsRepo: UserPreferencesRepository,
    ) : ViewModel() {
        val uiState: StateFlow<SettingsUiState> =
            combine(
                prefsRepo.themeMode,
                prefsRepo.skipIncrementMs,
                prefsRepo.loopCrossfade,
            ) { theme, skip, crossfade ->
                SettingsUiState(
                    themeMode = theme,
                    skipIncrementMs = skip,
                    loopCrossfade = crossfade,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SettingsUiState(),
            )

        fun setThemeMode(mode: ThemeMode) {
            viewModelScope.launch { prefsRepo.setThemeMode(mode) }
        }

        fun setSkipIncrement(ms: Long) {
            viewModelScope.launch { prefsRepo.setSkipIncrementMs(ms) }
        }

        fun setLoopCrossfade(enabled: Boolean) {
            viewModelScope.launch { prefsRepo.setLoopCrossfade(enabled) }
        }
    }
