package com.example.canvas.models

import kotlinx.serialization.Serializable

@Serializable
data class PointData(val x: Float, val y: Float)

@Serializable
data class Stroke(
    val id: String,
    val points: List<PointData>,
    val color: Long,         // ARGB as Long
    val strokeWidth: Float,
    val isEraser: Boolean = false
)
