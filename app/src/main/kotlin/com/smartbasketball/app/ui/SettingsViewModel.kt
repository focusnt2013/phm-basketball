package com.smartbasketball.app.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun updateCountdownTime(seconds: Int) {
        _uiState.update { it.copy(countdownTime = seconds) }
    }

    fun updateFixedBallCount(count: Int) {
        _uiState.update { it.copy(fixedBallCount = count) }
    }

    fun updateVolume(volume: Float) {
        _uiState.update { it.copy(volume = volume) }
    }
}

data class SettingsUiState(
    val countdownTime: Int = 60,
    val fixedBallCount: Int = 20,
    val volume: Float = 1.0f,
    val isLoading: Boolean = false
)
