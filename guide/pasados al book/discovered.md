# Guía de Uso: Anotación @Discovered

La anotación `@Discovered` permite marcar clases (como controladores REST, servicios, etc.) para que el framework Jettra las detecte y registre automáticamente al iniciar el servidor, sin necesidad de declararlas manualmente en la clase principal (`Main`).

## ¿Cómo funciona?

Jettra utiliza un **Procesador de Anotaciones** en tiempo de compilación. Cuando compilas tu proyecto, el procesador busca todas las clases anotadas con `@Discovered` y crea un índice ligero (`META-INF/jettra/discovered.classes`). En tiempo de ejecución, el servidor lee este índice casi instantáneamente, evitando los lentos escaneos de classpath que penalizan el arranque de la aplicación.

## Propiedades

* `automatic` (boolean): Determina si la clase debe ser cargada y registrada de forma automática por el servidor. El valor por defecto es `true`.
  * Si es `true`, la clase se auto-registrará.
  * Si es `false`, el procesador la indexa pero el cargador automático la ignorará. Será necesario registrarla manualmente si se desea utilizar.

## Ejemplo de Uso

```java
package com.miempresa.controladores;

import io.jettra.rest.annotations.Path;
import io.jettra.server.discoverer.Discovered;

// Este controlador se registrará automáticamente en el servidor
@Discovered
@Path("/api/usuarios")
public class UsuarioController {
    // ... métodos GET, POST, etc.
}
```

```java
package com.miempresa.controladores;

import io.jettra.rest.annotations.Path;
import io.jettra.server.discoverer.Discovered;

// Este controlador NO se registrará automáticamente. 
// Debe registrarse de forma manual en el Main si se desea habilitar.
@Discovered(automatic = false)
@Path("/api/admin")
public class AdminController {
    // ...
}
```

## Requisitos para el Auto-Registro

Para que las clases con `@Discovered(automatic = true)` sean inyectadas en tu servidor:
1. La clase principal de tu proyecto (`Main`) debe poseer la anotación `@DiscoveredLoad`.
2. Debes invocar `JettraRestServer.registerDiscovered(server, Main.class)`.

Para más detalles sobre cómo cargar estas clases, consulta la [guía de @DiscoveredLoad](discoveredload.md).
