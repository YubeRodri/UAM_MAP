package com.example.uammap.utils

import com.example.uammap.model.*

//GrafoCampus contiene los 16 nodos que representan las distintas ubicaciones dentro del campus de la UAM.
//Contiene aristas conectores, y la lista de puntos de interés.

object GrafoCampus {
    val nodos: List<Nodo> = listOf(
        Nodo("A", "Caja", 50f, 400f, TipoNodo.CAJA),
        Nodo("B", "Edificio B", 200f, 150f, TipoNodo.EDIFICIO),
        Nodo("C", "Edificio C", 130f, 320f, TipoNodo.EDIFICIO),
        Nodo("D", "Edificio D", 300f, 450f, TipoNodo.EDIFICIO),
        Nodo("E", "Edificio E", 350f, 80f, TipoNodo.EDIFICIO),
        Nodo("F", "Edificio F", 260f, 250f, TipoNodo.EDIFICIO),
        Nodo("G", "Edificio G", 400f, 350f, TipoNodo.EDIFICIO),
        Nodo("H", "Edificio H", 150f, 500f, TipoNodo.EDIFICIO),
        Nodo("I", "Biblioteca", 320f, 180f, TipoNodo.BIBLIOTECA),
        Nodo("J", "Edificio J", 450f, 120f, TipoNodo.EDIFICIO),
        Nodo("K", "Cafetería", 480f, 400f, TipoNodo.CAFETERIA),
        Nodo("L", "Edificio L", 380f, 520f, TipoNodo.EDIFICIO),
        Nodo("M", "Edificio M", 80f, 250f, TipoNodo.EDIFICIO),
        Nodo("N", "Edificio N", 520f, 250f, TipoNodo.EDIFICIO),
        Nodo("O", "Edificio O", 550f, 500f, TipoNodo.EDIFICIO),
        Nodo("P", "Edificio P", 250f, 380f, TipoNodo.EDIFICIO)
    )

    val aristas: List<Arista> = listOf(
        Arista("A", "H", 120.0),
        Arista("H", "L", 130.0),
        Arista("L", "O", 150.0),
        Arista("A", "C", 100.0),
        Arista("C", "M", 90.0),
        Arista("M", "B", 110.0),
        Arista("B", "E", 130.0),
        Arista("E", "J", 120.0),
        Arista("J", "N", 100.0),
        Arista("N", "O", 140.0),
        Arista("C", "F", 100.0),
        Arista("F", "I", 90.0),
        Arista("I", "J", 110.0),
        Arista("F", "G", 110.0),
        Arista("G", "K", 100.0),
        Arista("K", "O", 130.0),
        Arista("D", "H", 110.0),
        Arista("D", "G", 120.0),
        Arista("D", "L", 100.0),
        Arista("F", "P", 90.0),
        Arista("P", "D", 100.0),
        Arista("P", "H", 110.0)
    )

    val puntosInteres: List<PuntoDeInteres> = listOf(
        PuntoDeInteres("poi_caja", "Caja", "Pagos y trámites", "A", CategoriaPOI.CAJA),
        PuntoDeInteres("poi_biblio", "Biblioteca", "Consulta de libros", "I", CategoriaPOI.BIBLIOTECA),
        PuntoDeInteres("poi_cafe", "Cafetería Central", "Comida y café", "K", CategoriaPOI.CAFETERIA)
    )
}