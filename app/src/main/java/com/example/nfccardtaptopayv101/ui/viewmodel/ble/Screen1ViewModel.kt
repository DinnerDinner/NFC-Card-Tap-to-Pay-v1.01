package com.example.nfccardtaptopayv101.ui.viewmodel.ble

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class ModeSelectionUiState {
    object Idle : ModeSelectionUiState()
    object NavigateToRequestMoney : ModeSelectionUiState()
    object NavigateToSendMoney : ModeSelectionUiState()
}

class Screen1ViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow<ModeSelectionUiState>(ModeSelectionUiState.Idle)
    val uiState: StateFlow<ModeSelectionUiState> = _uiState

    fun onRequestMoneyClicked() {
        _uiState.value = ModeSelectionUiState.NavigateToRequestMoney
    }

    fun onSendMoneyClicked() {
        _uiState.value = ModeSelectionUiState.NavigateToSendMoney
    }

    fun clearNavigationFlag() {
        _uiState.value = ModeSelectionUiState.Idle
    }

    fun reset() {
        _uiState.value = ModeSelectionUiState.Idle
    }
}