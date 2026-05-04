package com.example.uammap.model

import androidx.compose.ui.geometry.Offset

data class Edificio(
    val name: String,
    val color: Int,
    val points: List<Offset>,
    val centroid: Offset
)