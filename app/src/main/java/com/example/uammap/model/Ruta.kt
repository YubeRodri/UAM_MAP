package com.example.uammap.model

//La clase Ruta es la encargada de almacenar el resultado del calculo de ruta efectuado cuando el usuario le da a la opción "Buscar Ruta" dentro de la aplicación.
//Almacena la secuencia de nodos que conforman el camino formado.

data class Ruta(
    val nodos: List<String>,
    val distanciaTotal: Double
)