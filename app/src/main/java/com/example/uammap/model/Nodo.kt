package com.example.uammap.model

//Esta clase representa un punto de conexión en el grafo. Contiene el identificador, nombre, coordenadas en dp y tipo de edificio. 

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