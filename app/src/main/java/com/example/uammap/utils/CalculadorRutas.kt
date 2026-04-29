package com.example.uammap.utils

//CalculadorRutas recibe el conjunto de la lista de nodos, aristas, origen y destino.
//A base de la información recibida, crea un objeto Ruta, que representa el camino más corto existente entre el origen y destino seleccionados.
//Para lograr esto, utiliza un algoritmo estilo Dijkstra, un metodo utilizado para encontrar la ruta más corta desde un nodo inicial en un grafo, hasta los demás nodos.

import com.example.uammap.model.*
import java.util.PriorityQueue

object CalculadorRutas {
    fun calcularRuta(
        nodos: List<Nodo>,
        aristas: List<Arista>,
        origenId: String,
        destinoId: String
    ): Ruta? {
        val dist = mutableMapOf<String, Double>()
        val prev = mutableMapOf<String, String?>()
        val pq = PriorityQueue<Pair<String, Double>>(compareBy { it.second })

        nodos.forEach {
            dist[it.id] = Double.MAX_VALUE
            prev[it.id] = null
        }
        dist[origenId] = 0.0
        pq.add(origenId to 0.0)

        while (pq.isNotEmpty()) {
            val (u, d) = pq.poll() ?: continue
            if (u == destinoId) break
            if (d > dist[u]!!) continue

            val vecinos = aristas.filter { it.origen == u }.map { it.destino to it.peso } +
                    aristas.filter { it.destino == u }.map { it.origen to it.peso }
            for ((v, peso) in vecinos) {
                val alt = dist[u]!! + peso
                if (alt < dist[v]!!) {
                    dist[v] = alt
                    prev[v] = u
                    pq.add(v to alt)
                }
            }
        }

        if (prev[destinoId] == null && origenId != destinoId) return null

        val camino = mutableListOf<String>()
        var actual: String? = destinoId
        while (actual != null) {
            camino.add(0, actual)
            actual = prev[actual]
        }
        return Ruta(camino, dist[destinoId]!!)
    }
}