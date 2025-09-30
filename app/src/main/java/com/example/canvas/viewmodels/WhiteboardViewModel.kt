package com.example.canvas.viewmodels

import androidx.lifecycle.ViewModel
import com.example.canvas.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WhiteboardViewModel : ViewModel() {

    private val _state = MutableStateFlow(WhiteboardState())
    val state: StateFlow<WhiteboardState> = _state

    // Full history stacks
    private val undoStack: ArrayDeque<WhiteboardState> = ArrayDeque()
    private val redoStack: ArrayDeque<WhiteboardState> = ArrayDeque()

    // limit (tweakable, e.g. 50–100 states)
    private val HISTORY_LIMIT = 50

    // === Core ===
    private fun pushState(newState: WhiteboardState) {
        // Save current before change
        undoStack.addLast(_state.value.copy())

        // Enforce limit
        if (undoStack.size > HISTORY_LIMIT) {
            undoStack.removeFirst()
        }

        // Once a new action happens, redo is invalid
        redoStack.clear()

        _state.value = newState
    }

    // User actions
    fun addStroke(stroke: Stroke) {
        pushState(
            _state.value.copy(strokes = _state.value.strokes + stroke)
        )
    }

    fun addShape(shape: ShapeModel) {
        pushState(
            _state.value.copy(shapes = _state.value.shapes + shape)
        )
    }

    fun addText(text: TextModel) {
        pushState(
            _state.value.copy(texts = _state.value.texts + text)
        )
    }

    fun deleteText(id: String) {
        pushState(
            _state.value.copy(texts = _state.value.texts.filterNot { it.id == id })
        )
    }

    fun clearCanvas() {
        pushState(WhiteboardState())
    }

    fun enableEraser(enabled: Boolean) {
        pushState(_state.value.copy(isEraserOn = enabled))
    }

    // Undo / Redo
    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeLast()
            redoStack.addLast(_state.value.copy())

            if (redoStack.size > HISTORY_LIMIT) {
                redoStack.removeFirst()
            }

            _state.value = prev
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeLast()
            undoStack.addLast(_state.value.copy())

            if (undoStack.size > HISTORY_LIMIT) {
                undoStack.removeFirst()
            }

            _state.value = next
        }
    }
}

