package com.example.uammap.model

data class PuntoDeInteres(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val nodoAsociado: String,
    val categoria: CategoriaPOI
)

enum class CategoriaPOI {
    BIBLIOTECA, CAJA, CAFETERIA, AUDITORIO, OTRO
}