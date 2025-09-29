package com.example.canvas.models

import kotlinx.serialization.Serializable

@Serializable
data class TextModel(
    val id: String,
    val x: Float,
    val y: Float,
    val text: String,
    val color: Long,
    val fontSizeSp: Float
)
