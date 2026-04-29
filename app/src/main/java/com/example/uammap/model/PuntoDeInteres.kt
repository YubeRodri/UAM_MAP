package com.example.uammap.model

//La clase representa un lugar de alta demanda dentro del campus de la UAM, que se asocia a alguno de los nodos existentes.

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