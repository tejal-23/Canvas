package com.example.canvas.models

import kotlinx.serialization.Serializable

@Serializable
data class WhiteboardState(
    val strokes: List<Stroke> = emptyList(),
    val shapes: List<ShapeModel> = emptyList(),
    val texts: List<TextModel> = emptyList(),
    val width: Int = 0,
    val height: Int = 0,
    val isEraserOn: Boolean = false

)
