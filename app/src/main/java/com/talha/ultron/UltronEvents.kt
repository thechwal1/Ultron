package com.talha.ultron

import com.talha.ultron.ui.ChatMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

enum class UltronState { IDLE, LISTENING, THINKING, SPEAKING }

/**
 * In-process event bus so the foreground service can push live updates
 * (new chat messages, state changes) straight to the UI and the floating
 * overlay while they're visible — no polling.
 */
object UltronEvents {
    private val _messages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 16)
    val messages = _messages.asSharedFlow()

    private val _state = MutableStateFlow(UltronState.IDLE)
    val state = _state.asStateFlow()

    suspend fun emitMessage(message: ChatMessage) {
        _messages.emit(message)
    }

    fun setState(newState: UltronState) {
        _state.value = newState
    }
}
