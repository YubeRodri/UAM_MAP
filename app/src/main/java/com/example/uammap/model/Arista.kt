package com.example.uammap.model

//Esta clase define un camino entre dos nodos, incluyendo su distancia.

data class Arista(val origen: String, val destino: String, val peso: Double)