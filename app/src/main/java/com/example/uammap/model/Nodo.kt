package com.example.uammap.model

data class Nodo(
    val id: String,
    val nombre: String,
    val x: Float,
    val y: Float,
    val tipo: TipoNodo = TipoNodo.EDIFICIO
)

enum class TipoNodo {
    EDIFICIO, BIBLIOTECA, CAJA, CAFETERIA
}