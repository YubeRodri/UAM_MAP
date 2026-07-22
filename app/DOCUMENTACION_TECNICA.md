# Documentación Técnica: Proyecto UAM MAP

## 1. Introducción
El proyecto **UAM MAP** es una solución integral de navegación y visualización geoespacial para el campus de la Universidad Americana. Se basa en una arquitectura Cliente-Servidor que utiliza un Backend en Spring Boot para la distribución de datos y una aplicación Android nativa para la experiencia del usuario final.

---

## 2. Arquitectura del Sistema
El sistema emplea una arquitectura de **microservicios simplificada** y **Clean Architecture** en el cliente:

### A. Backend (Spring Boot)
- **Tecnología:** Java 17, Spring Boot 3.3.0.
- **Función:** Servidor de datos RESTful.
- **Endpoints Principales:**
  - `GET /api/campus.geojson`: Entrega el mapa completo en formato GeoJSON.
  - `POST /api/location`: Recibe actualizaciones de ubicación de los usuarios.
- **Swagger/OpenAPI:** Implementado para documentación y pruebas interactivas de la API.

### B. Frontend (Android)
- **Lenguaje:** Kotlin.
- **UI:** Jetpack Compose (Declarativa).
- **Consumo de API:** Retrofit + OkHttp.
- **Procesamiento de Datos:** Gson / JsonParser.

---

## 3. Técnicas de Programación Empleadas

### 1. Programación Reactiva (StateFlow & Jetpack Compose)
Se utiliza `MutableStateFlow` en el `LocationManager` para emitir cambios de ubicación. La interfaz de usuario en `HomeScreen` "observa" estos estados y se repinta automáticamente.
*   **Por qué:** Garantiza que la UI siempre esté sincronizada con los sensores sin necesidad de llamadas manuales a funciones de actualización.

### 2. Animación de Estados (Interpolación)
Se emplea `animateFloatAsState` con `Spring Spec`.
*   **Por qué:** El GPS entrega coordenadas discretas (saltos). Para lograr una fluidez "tipo Google Maps", el código interpola los valores entre el punto A y el punto B, creando un movimiento continuo de 60 FPS.

### 3. Concurrencia con Corrutinas (Kotlin Coroutines)
Todas las operaciones de red y lectura de archivos se ejecutan en `Dispatchers.IO`.
*   **Por qué:** Evita que la interfaz de usuario se congele (ANR) mientras se descarga el mapa o se procesan miles de coordenadas.

### 4. Patrón Singleton
`LocationManager` y `MapDataLoader` son `object` (Singletons).
*   **Por qué:** Asegura que solo exista una instancia gestionando los sensores y los datos del mapa, ahorrando memoria y evitando conflictos de datos.

---

## 4. Funciones Críticas del Código

### `MapDataLoader.load(context)`
Lógica de **Carga Dual (Fallback)**:
1.  Intenta obtener el mapa desde la API REST.
2.  Si falla (offline), carga automáticamente el archivo desde `assets`.
*   **Técnica:** Garantía de disponibilidad.

### `LocationManager.startLocationUpdates()`
Configuración de **Alta Frecuencia**:
- Intervalo: 100ms.
- Prioridad: `PRIORITY_HIGH_ACCURACY`.
*   **Técnica:** Sincronización instantánea solicitada para tiempo real.

### `HomeScreen.Canvas`
Renderizado de **Bajo Nivel**:
- En lugar de usar componentes pesados, se dibuja directamente en un `Canvas`.
*   **Técnica:** Optimización brutal de rendimiento. Permite manejar cientos de polígonos (edificios) y líneas (calles) con zoom y paneo fluido.

---

## 5. Seguridad y Conectividad
- **Network Security Config:** Se implementó un archivo XML de seguridad para permitir tráfico `HTTP` (cleartext) hacia la IP específica del servidor local, superando las restricciones por defecto de Android 9+.
- **Filtro de Precisión:** El código valida la `accuracy` (precisión) del GPS antes de mover el cursor para evitar saltos erráticos.

---

## 6. Conclusión
UAM MAP no es solo un visor de mapas; es una aplicación optimizada que utiliza técnicas modernas de **interpolación visual**, **programación asíncrona** y **arquitectura robusta de red** para ofrecer una experiencia de usuario profesional y reactiva.
