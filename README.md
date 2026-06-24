UAM-MAP - MAPA DE NAVEGACIÓN INTERNA DE LA UNIVERSIDAD AMERICANA

Prototipo funcional de un sistema de navegación geo-referencial para el campus de la Universidad Americana (UAM).
La aplicación permite visualizar los 16 edificios (A–P) con puntos de interés como la Caja (A), la Biblioteca (I) y la Cafetería (K), calcular rutas óptimas entre ellos y simular la navegación en tiempo real al estilo Waze.

DESCRIPCIÓN

Cada año, entre 1200 y 1800 estudiantes de nuevo ingreso se incorporan al campus y sufren desorientación de ubicación institucional. Esta app resuelve esa problemática mediante:

- Mapa interactivo del campus con edificios distribuidos geográficamente.
- Búsqueda de destinos por nombre de edificio o lugar.
- Cálculo automático de la ruta más corta usando el algoritmo de Dijkstra.
- Simulación de navegación paso a paso (Waze‑style) que avanza por los nodos de la ruta.
- Interfaz intuitiva con desplazamiento táctil (arrastrar el mapa) y toque directo sobre un edificio para obtener la ruta desde la Caja.

CARACTERÍSTICAS PRINCIPALES

- 16 edificios (A a P) colocados de forma realista en el mapa.
- Dijkstra para calcular la ruta más corta sobre el grafo del campus.
- Mapa desplazable con el dedo (arrastre) y toque en los círculos para calcular ruta.
- Búsqueda por texto de puntos de interés y edificios.
- Pantalla de ruta con distancia y recorrido detallado.
- Navegación animada que mueve un punto verde a lo largo de la ruta.
- Navegación entre pantallas con retroceso en cada paso.
- Interfaz moderna con Material 3 y fondo claro.

TECNOLOGÍAS USADAS

CATEGORÍA	TECNOLOGÍA
Lenguaje	Kotlin
UI	Jetpack Compose + Material 3
Navegación	Jetpack Navigation Compose
Algoritmo de ruta	Dijkstra
Gráficos	Canvas de Compose para círculos y texto
Gestos táctiles	detectDragGestures, detectTapGestures
IDE	Android Studio
Control de versión	Local, Git + Github

INSTRUCCIONES

Requisitos previos
- Android Studio instalado.
- JDK 17 o superior.
- Emulador con API 24+ o dispositivo físico con depuración USB activada.
- Conexión a Internet para la primera sincronización de Gradle.

Abre el proyecto en Android Studio.
Selecciona "File" y después "Open" y elige la carpeta clonada o donde tengas el proyecto.
Ejecuta la aplicación.
Una vez en el IDE, haz clic en "Run" o Shift + F10
Usa la app, verás el mapa con los edificios A-P, arrastra el mapa para explorar, toca un círculo para obtener la ruta desde la Caja o presiona "Buscar destino" para escribir el nombre y seleccionar de la lista.
