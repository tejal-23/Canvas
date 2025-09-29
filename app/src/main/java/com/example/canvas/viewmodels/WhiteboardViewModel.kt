package com.example.canvas.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.canvas.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WhiteboardViewModel : ViewModel() {

    private val _state = MutableStateFlow(WhiteboardState())
    val state: StateFlow<WhiteboardState> = _state

    // User actions
    fun addStroke(stroke: Stroke) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                strokes = _state.value.strokes + stroke
            )
        }
    }

    fun addShape(shape: ShapeModel) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                shapes = _state.value.shapes + shape
            )
        }
    }

    fun addText(text: TextModel) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                texts = _state.value.texts + text
            )
        }
    }

    fun deleteText(id: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                texts = _state.value.texts.filterNot { it.id == id }
            )
        }
    }

    fun clearCanvas() {
        viewModelScope.launch {
            _state.value = WhiteboardState()
        }
    }

    fun undo() {
        viewModelScope.launch {
            val current = _state.value
            when {
                current.strokes.isNotEmpty() -> _state.value =
                    current.copy(strokes = current.strokes.dropLast(1))
                current.shapes.isNotEmpty() -> _state.value =
                    current.copy(shapes = current.shapes.dropLast(1))
                current.texts.isNotEmpty() -> _state.value =
                    current.copy(texts = current.texts.dropLast(1))
            }
        }
    }

    fun enableEraser(enabled: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isEraserOn = enabled)
        }
    }
}
