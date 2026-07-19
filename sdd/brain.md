Para abordar una arquitectura de esta magnitud, estructurada en **12 repositorios interconectados bajo Java 25 y Maven**, necesitamos un documento centralizado que actúe como la "fuente de la verdad" (*Single Source of Truth*).

Este plan **`brain.md`** está diseñado bajo los principios de **Spec-Driven Development (SDD)**. Su objetivo es mapear las responsabilidades exactas de cada componente del ecosistema Jettra, sus interdependencias críticas y servir de guía estricta para que cualquier IA (o tú mismo) pueda expandir, refactorizar o añadir módulos sin romper las reglas de diseño del framework.

Aquí tienes la estructura completa del archivo para tu base de conocimientos.

---

```markdown
# Brain Specification: Jettra Ecosystem (Java 25 & Maven)
## Spec-Driven Development (SDD) Guide

Este documento es la especificación maestra y cerebro operacional del ecosistema **Jettra**. Define la arquitectura híbrida, las responsabilidades de cada repositorio y las reglas de acoplamiento para guiar el desarrollo asistido por IA (Antigravity/Gemini), asegurando alto rendimiento, escalabilidad y estricto tipado estático.

---

## 1. Visión General de la Arquitectura

El ecosistema **Jettra** es un framework corporativo *full-stack* nativo para **Java 25**. Su propuesta de valor central radica en la unificación del ciclo de desarrollo: elimina la fragmentación de tecnologías (HTML/JS/CSS) traduciendo abstracciones puras orientadas a objetos en interfaces reactivas, mientras expone microservicios de alto rendimiento (REST/gRPC), motores de reglas y motores de persistencia multimodelo optimizados con las últimas características de la JVM (Virtual Threads, Pattern Matching avanzado y Stream Gatherers).

### Grafo de Dependencias Críticas
1. **`JettraAnnotation`** ➔ Consumido por prácticamente todo el ecosistema para la generación de código en tiempo de compilación.
2. **`JettraJSON`** ➔ El motor de serialización base de baja latencia usado por `JettraRest`, `JettraStoreEngine` y `JettraFlux`.
3. **`JettraFlux`** ➔ Acoplado a `JettraAppServer` (para el ciclo de vida/sesión) y demostrado en `JettraFluxExample`.

---

## 2. Especificación por Proyecto (Módulos del Ecosistema)

### Módulo 1: JettraAnnotation
*   **Repositorio:** `https://github.com/avbravo/JettraAnnotation.git`
*   **Propósito:** Procesamiento de anotaciones en tiempo de compilación (*APT - Annotation Processing Tool*) para evitar la reflexión en tiempo de ejecución, maximizando el rendimiento en Java 25.
*   **Componentes Clave:**
    *   `@FluxComponent`, `@RestEndpoint`, `@StoreEntity`, `@Inject`.
    *   Procesadores que extienden de `AbstractProcessor` generando código fuente estructurado automáticamente.
*   **Reglas de Modificación (SDD):** Toda nueva anotación debe incluir su procesador correspondiente y validar las restricciones de tipo en tiempo de compilación mediante el uso del árbol de elementos (`ElementVisitor`).

### Módulo 2: JettraAppServer
*   **Repositorio:** `https://github.com/avbravo/JettraAppServer.git`
*   **Propósito:** Servidor de aplicaciones integrado y ultra-ligero que gestiona el ciclo de vida de los microservicios, la inyección de dependencias (IoC) básica y el manejo de contextos/sesiones.
*   **Componentes Clave:**
    *   `ApplicationContext`: Registro de instancias y scopes (Singleton, Session, Request).
    *   `ServerContainer`: Orquestador de sockets y hilos virtuales (Virtual Threads) para manejar concurrencia masiva.
*   **Reglas de Modificación (SDD):** Cualquier cambio en el manejo de peticiones concurrentes debe optimizarse usando *Structured Concurrency* de Java 25.

### Módulo 3: JettraJSON
*   **Repositorio:** `https://github.com/avbravo/JettraJSON.git`
*   **Propósito:** Motor de parsing y serialización JSON de alto rendimiento, optimizado para no generar basura (*garbage-free*) y aprovechar los *Records* de Java.
*   **Componentes Clave:**
    *   `JettraJsonParser`, `JsonWriter`.
    *   Soporte nativo para mapear esquemas directamente a tipos de datos complejos e inmutables.
*   **Reglas de Modificación (SDD):** Prohibido el uso de reflexión pesada. Se debe usar *MethodHandles* o generación de código previo vía `JettraAnnotation`.

### Módulo 4: JettraRest
*   **Repositorio:** `https://github.com/avbravo/JettraRest.git`
*   **Propósito:** Engine para la exposición y consumo de APIs RESTful que implementa de forma nativa la especificación OpenAPI.
*   **Componentes Clave:**
    *   `RestRouter`: Enrutador de peticiones basado en búsquedas de prefijos (*Radix Tree*) de alta velocidad.
    *   `OpenApiGenerator`: Escanea rutas para disponibilizar el archivo `openapi.json` dinámicamente.
*   **Reglas de Modificación (SDD):** Al añadir soporte para nuevos verbos o layouts HTTP, se debe actualizar obligatoriamente el generador de la documentación OpenAPI.

### Módulo 5: JettraJWT
*   **Repositorio:** `https://github.com/avbravo/JettraJWT.git`
*   **Propósito:** Capa de seguridad y autenticación sin estado (*stateless*) mediante JSON Web Tokens.
*   **Componentes Clave:**
    *   `JwtProvider`: Creación y firma de tokens con algoritmos modernos (RS256, EdDSA).
    *   `JwtFilter`: Interceptor compatible con `JettraRest` y `JettraAppServer` para la validación de claims y roles.
*   **Reglas de Modificación (SDD):** El almacenamiento temporal de claves criptográficas debe mitigar vulnerabilidades de memoria utilizando estructuras limpias e inmutables.

### Módulo 6: JettraFlux
*   **Repositorio:** `https://github.com/avbravo/JettraFlux.git`
*   **Propósito:** API de abstracción de interfaz de usuario. Encapsula HTML5, Tailwind CSS y JavaScript en componentes puros de Java, gestionando el estado del lado del servidor de forma reactiva.
*   **Componentes Clave:**
    *   `Component`, `Row`, `Column`, `Chip`, `DataTable`.
    *   Sistema de renderizado basado en árboles de componentes virtuales y actualización mediante eventos.
*   **Reglas de Modificación (SDD):** **No escribir código HTML/CSS/JS directo en las vistas.** Todo elemento visual debe ser una clase Java que herede de `Component`. Los eventos interactivos de visibilidad o mutación deben gestionarse con JavaScript vanilla generado por el propio componente, evitando dependencias externas obsoletas como jQuery.

### Módulo 7: JettraFluxExample
*   **Repositorio:** `https://github.com/avbravo/JettraFluxExample.git`
*   **Propósito:** Proyecto de referencia y banco de pruebas de UI que implementa casos de uso complejos (menús colapsables persistentes, cuadrículas de datos avanzadas, formularios enlazados).
*   **Componentes Clave:**
    *   `TemplatePage`: Página maestra con menús de navegación (`LeftMenu`).
    *   `MiscPage`: Demostración de componentes misceláneos como `Chip` con iconos.
*   **Reglas de Modificación (SDD):** Sirve como validador de regresión visual. Si un componente se modifica en `JettraFlux`, su impacto debe reflejarse y probarse aquí. Los menús colapsados dinámicamente deben mantener su estado en la sesión hasta que el usuario decida interactuar con ellos.

### Módulo 8: JettraGRPC
*   **Repositorio:** `https://github.com/avbravo/JettraGRPC.git`
*   **Propósito:** Módulo de comunicación inter-servicio de ultra-baja latencia basado en HTTP/2 y Protocol Buffers.
*   **Componentes Clave:**
    *   `JettraGrpcServer`, `GrpcClientStub`.
    *   Mapeadores directos entre entidades de dominio Java y mensajes proto.
*   **Reglas de Modificación (SDD):** Debe integrarse directamente con el pool de Virtual Threads de `JettraAppServer` para evitar el bloqueo de hilos del sistema operativo en llamadas RPC extensas.

### Módulo 9: JettraStoreEngine
*   **Repositorio:** `https://github.com/avbravo/JettraStoreEngine.git`
*   **Propósito:** Motor de base de datos multimodelo embebido/distribuido (Key-Value, Documental y Relacional básico) diseñado desde cero.
*   **Componentes Clave:**
    *   `StoreEngine`: Gestor de almacenamiento en disco/memoria.
    *   Capa de consenso distribuido (estructuras estilo Raft) y estrategias de sharding de datos.
*   **Reglas de Modificación (SDD):** Las operaciones de I/O en disco deben aprovechar las optimizaciones de canales de memoria e inmutabilidad de Java 25.

### Módulo 10: JettraRules
*   **Repositorio:** `https://github.com/avbravo/JettraRules.git`
*   **Propósito:** Motor de reglas de negocio ejecutable en tiempo real mediante evaluación secuencial o de grafos directos.
*   **Componentes Clave:**
    *   `RuleEngine`, `FactContext`, `RuleBuilder`.
*   **Reglas de Modificación (SDD):** La evaluación de condiciones debe optimizarse utilizando expresiones condicionales avanzadas y el *Pattern Matching* nativo de la versión de Java activa.

### Módulo 11: JettraReport
*   **Repositorio:** `https://github.com/avbravo/JettraReport.git`
*   **Propósito:** Motor de generación de reportes estructurados (PDF, Excel, JSON estructural) sin bloqueos de memoria.
*   **Componentes Clave:**
    *   `ReportGenerator`, `ExcelExporter` (con soporte para librerías eficientes de streaming de celdas).
*   **Reglas de Modificación (SDD):** El procesamiento de grandes volúmenes de datos para exportación debe utilizar *Stream Gatherers* para fragmentar y paginar el consumo de memoria.

### Módulo 12: JettraTest
*   **Repositorio:** `https://github.com/avbravo/JettraTest.git`
*   **Propósito:** Framework de pruebas especializado para validar el comportamiento del ecosistema (Mocks de peticiones REST, validación de estructuras gRPC y aserciones de renderizado en JettraFlux).
*   **Componentes Clave:**
    *   `JettraTestRunner`, `FluentAssertions`.
*   **Reglas de Modificación (SDD):** Cada nueva funcionalidad crítica en el resto de los módulos debe emparejarse obligatoriamente con una suite de pruebas automatizada en este proyecto.

---

## 3. Directrices de Mejora y Escalabilidad

Al dar instrucciones de desarrollo a la IA basadas en este plan, se deben auditar los siguientes puntos críticos:

1.  **Tipado en Checkboxes/Formularios:** Al mapear componentes de formularios reactivos (`JettraFlux`) hacia modelos de datos, evitar tipos primitivos de control ambiguo en la serialización si el comportamiento del framework tiende a invertir la lógica del estado seleccionado. Utilizar enlaces basados en cadenas estables de texto o envolturas (*wrappers*) controladas.
2.  **Cero dependencias visuales:** Mantener el aislamiento absoluto de `JettraFlux`. Si un elemento requiere interactividad (como colapsar/expandir menús o lanzar alertas), inyectar código JavaScript vanilla directo y estructurado. Prohibir el uso de jQuery.
3.  **Rendimiento E/S:** Asegurar que `JettraStoreEngine` y `JettraAppServer` utilicen exclusividad de hilos virtuales para las operaciones bloqueantes de red o almacenamiento, liberando los hilos portadores (*carrier threads*) de la CPU.

```

---
