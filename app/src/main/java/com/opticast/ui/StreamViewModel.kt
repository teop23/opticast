package com.opticast.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opticast.data.ConnectionRepository
import com.opticast.model.Connection
import com.opticast.model.StreamState
import com.opticast.model.StreamStats
import com.opticast.stream.Broadcaster
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiState(
    val streamState: StreamState = StreamState.Idle,
    val stats: StreamStats = StreamStats(),
    val selected: Connection? = null,
    val muted: Boolean = false,
    val torch: Boolean = false,
    val previewEnabled: Boolean = false,   // off by default to save battery/CPU
    val connections: List<Connection> = emptyList()
)

class StreamViewModel(
    private val broadcaster: Broadcaster,
    private val repository: ConnectionRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch { broadcaster.state.collect { s -> _ui.update { it.copy(streamState = s) } } }
        viewModelScope.launch { broadcaster.stats.collect { s -> _ui.update { it.copy(stats = s) } } }
        viewModelScope.launch {
            repository.connections().collect { list -> _ui.update { it.copy(connections = list) } }
        }
    }

    /** The underlying engine (unwrapped from any coordinator) so the Live screen can attach preview. */
    fun broadcasterForPreview(): Broadcaster =
        (broadcaster as? com.opticast.stream.StreamCoordinator)?.engine() ?: broadcaster

    fun selectConnection(c: Connection) { _ui.update { it.copy(selected = c) } }

    fun goLive() {
        val c = _ui.value.selected
        if (c == null) {
            _ui.update { it.copy(streamState = StreamState.Error("No connection selected")) }
            return
        }
        broadcaster.start(c)
    }

    fun stop() = broadcaster.stop()
    fun switchCamera() = broadcaster.switchCamera()

    fun toggleMute() {
        val next = !_ui.value.muted
        broadcaster.setMuted(next)
        _ui.update { it.copy(muted = next) }
    }

    fun toggleTorch() {
        val next = !_ui.value.torch
        broadcaster.setTorch(next)
        _ui.update { it.copy(torch = next) }
    }

    fun togglePreview() = _ui.update { it.copy(previewEnabled = !it.previewEnabled) }

    suspend fun save(c: Connection) = repository.save(c)
    suspend fun delete(id: String) = repository.delete(id)
}
