package com.tks.videophotobookv3.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tks.videophotobookv3.model.ArKeyPair
import com.tks.videophotobookv3.repository.KeyPairRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainScreenViewModel(private val repository: KeyPairRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<List<ArKeyPair>>(emptyList())
    val uiState: StateFlow<List<ArKeyPair>> = _uiState.asStateFlow()

    init {
        loadPairs()
    }

    fun loadPairs() {
        viewModelScope.launch {
            _uiState.value = repository.getPairs()
        }
    }

    fun addPair(markerUri: String, videoUri: String, physicalWidth: Float, scaleFactor: Float) {
        viewModelScope.launch {
            val newPair = ArKeyPair(
                markerUri = markerUri,
                videoUri = videoUri,
                physicalWidth = physicalWidth,
                scaleFactor = scaleFactor
            )
            repository.addPair(newPair)
            loadPairs()
        }
    }

    fun updatePair(id: String, markerUri: String, videoUri: String, physicalWidth: Float, scaleFactor: Float) {
        viewModelScope.launch {
            val updatedPair = ArKeyPair(
                id = id,
                markerUri = markerUri,
                videoUri = videoUri,
                physicalWidth = physicalWidth,
                scaleFactor = scaleFactor
            )
            repository.updatePair(updatedPair)
            loadPairs()
        }
    }

    fun deletePair(id: String) {
        viewModelScope.launch {
            repository.deletePair(id)
            loadPairs()
        }
    }
}
