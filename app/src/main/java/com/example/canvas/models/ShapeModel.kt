package com.example.canvas.models

import kotlinx.serialization.Serializable

@Serializable
data class ShapeModel(
    val id: String,
    val type: ShapeType,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val color: Long,
    val strokeWidth: Float,
    val points: List<PointData> = emptyList() // For polygon vertices
)

@Serializable
enum class ShapeType { RECTANGLE, CIRCLE, LINE, POLYGON }
