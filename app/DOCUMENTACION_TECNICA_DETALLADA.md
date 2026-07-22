# Documentación Técnica Exhaustiva: Proyecto UAM MAP

Este documento proporciona una explicación detallada, casi línea por línea, de los componentes fundamentales del proyecto UAM MAP, cubriendo tanto la aplicación Android como el servidor Backend.

---

## 1. MÓDULO ANDROID (Frontend)

### A. `LocationManager.kt` (Gestión de Ubicación)
Es el "corazón" de los sensores. Su función es obtener la ubicación del GPS y transmitirla a la UI.

*   **`MutableStateFlow<Location?>(null)`**: Utilizamos Programación Reactiva. Esta variable "emite" la ubicación. Cuando el GPS detecta un cambio, esta variable notifica a toda la aplicación.
*   **`locationRequest` (100ms)**: Configuramos una frecuencia de actualización ultra alta. El sistema le pide al GPS coordenadas cada 0.1 segundos para que la sincronización sea instantánea.
*   **`Priority.PRIORITY_HIGH_ACCURACY`**: Obliga al teléfono a usar el GPS satelital en lugar de solo Wi-Fi, para obtener precisión métrica.
*   **`onLocationResult`**: Aquí validamos los datos. Si el GPS envía una precisión pobre (>25m), descartamos el punto para evitar que el cursor "salte" a lugares aleatorios.
*   **`scope.launch { apiService.updateLocation(...) }`**: Cada vez que la ubicación es válida, se envía asíncronamente al servidor Spring Boot para mantener el historial en el backend.

### B. `MapDataLoader.kt` (Cargador de Datos)
Encargado de procesar el archivo GeoJSON y transformarlo en objetos de dibujo.

*   **`load(context)`**: Implementa la técnica de **Carga Dual**. Primero intenta conectar con `http://10.0.27.190:8080`. Si el servidor no responde, captura el error y abre el archivo local desde la carpeta `assets`.
*   **`parseGeoJson(root)`**: Es un motor de parsing manual. Recorre miles de líneas de JSON buscando "Polygons" (edificios) y "Points" (puntos de interés).
*   **`project(lon, lat)`**: Esta es la función matemática de **Proyección Cartográfica**. Convierte coordenadas globales (longitud/latitud) a coordenadas de pantalla (X, Y) para que el mapa se dibuje correctamente en el celular.
*   **`extractName(props)`**: Limpia los nombres de los edificios, quitando prefijos como "Edificio" o "Ed." para que el mapa se vea limpio.

### C. `HomeScreen.kt` (Interfaz y Visualización)
Es donde ocurre toda la magia visual y el renderizado a 60 FPS.

*   **`animateFloatAsState` (Interpolación)**: Es la técnica más avanzada de la UI. Toma la coordenada A y la coordenada B, y calcula todos los puntos intermedios. Esto crea el efecto de que el punto azul se desliza suavemente "tipo Google Maps" en lugar de aparecer y desaparecer.
*   **`Canvas`**: No usamos botones o imágenes estándar para el mapa. Dibujamos directamente en el "lienzo" de la GPU. Esto permite que el zoom y el movimiento sean extremadamente fluidos sin importar cuántos edificios haya.
*   **`detectTransformGestures`**: Maneja el Zoom (pellizco) y el Paneo (arrastrar). Calcula cuánto debe escalarse el mapa basado en tus dedos.
*   **`currentRoute = listOf(smoothedUserPos, destinationPoint)`**: Esta línea crea la **Ruta Directa**. Toma tu posición visual animada y traza una línea recta impecable hacia donde tocaste, sin curvas ni retrasos.

### D. `ApiService.kt` (Comunicación de Red)
*   **`Retrofit.Builder()`**: Configura la conexión HTTP.
*   **`BASE_URL`**: La dirección IP de tu computadora. Es el puente entre el celular y el servidor.
*   **`GsonConverterFactory`**: Convierte automáticamente el texto que viene del servidor en objetos de programación que Kotlin entiende.

---

## 2. MÓDULO BACKEND (Spring Boot)

### A. `CampusController.java`
*   **`@RestController`**: Marca la clase como un punto de acceso a la API.
*   **`@GetMapping("/campus.geojson")`**: Define la dirección web que el celular debe llamar.
*   **`Files.readString(...)`**: Lee el archivo del mapa desde el disco duro de tu PC y lo envía por internet al celular.

### B. `pom.xml` (Gestión de Dependencias)
*   **`spring-boot-starter-web`**: El motor que permite que tu PC actúe como un servidor.
*   **`springdoc-openapi` (Swagger)**: Crea la interfaz web `http://localhost:8080/swagger-ui/index.html` para que puedas probar la API sin usar el celular.
*   **`Lombok`**: Técnica de metaprogramación que genera automáticamente código (Getters/Setters) para mantener el proyecto limpio.

---

## 3. SEGURIDAD Y CONFIGURACIÓN

### `network_security_config.xml`
Android bloquea conexiones no seguras (HTTP). Este archivo es una **excepción de seguridad**. Le dice al sistema operativo: "Confío en mi servidor local 10.0.27.190, déjalo pasar". Sin esto, la app daría error de `CLEARTEXT_NOT_PERMITTED`.

### `AndroidManifest.xml`
*   **`ACCESS_FINE_LOCATION`**: Permiso para usar el GPS de alta precisión.
*   **`INTERNET`**: Permiso para que la app salga a la red a buscar el mapa.

---

## 4. CONCLUSIÓN
El sistema utiliza una combinación de **Matemática de Proyección**, **Animación por Interpolación** y **Arquitectura REST**. La clave de la fluidez radica en separar el procesamiento pesado (red y parsing) en hilos secundarios (Corrutinas) y usar renderizado de bajo nivel (Canvas) para la visualización.
