# Guía de Uso: Anotación @DiscoveredLoad

La anotación `@DiscoveredLoad` se utiliza exclusivamente a nivel de la clase principal (usualmente `Main.java`) de tu proyecto. Actúa como el interruptor maestro que habilita la carga automática de todas las clases del proyecto y sus dependencias que hayan sido marcadas con `@Discovered(automatic = true)`.

## ¿Por qué es necesaria?

En aplicaciones grandes, podrías incluir librerías que contienen cientos de controladores o servicios marcados con `@Discovered`. Si el servidor los cargara todos sin tu consentimiento explícito, podrías tener rutas no deseadas o problemas de seguridad. Al colocar `@DiscoveredLoad` en tu clase `Main`, le indicas explícitamente a Jettra que apruebas el auto-descubrimiento en este proyecto.

## Ejemplo de Uso

A continuación se muestra cómo limpiar un método `main` tradicional que registraba múltiples clases manualmente:

```java
package com.jettra.main;

import io.jettra.server.JettraServer;
import io.jettra.rest.server.JettraRestServer;
import io.jettra.server.discoverer.DiscoveredLoad;
import io.jettra.server.discoverer.DiscoveredRegistry;

import java.util.ArrayList;
import java.util.List;

// 1. Activar la carga de clases descubiertas
@DiscoveredLoad
public class Main {

    public static void main(String[] args) {
        JettraServer server = new JettraServer();

        // 2. Obtener la lista de controladores descubiertos automáticamente (útil para OpenAPI/Swagger)
        List<Class<?>> controllers = new ArrayList<>(DiscoveredRegistry.getDiscoveredClasses(Main.class));
        
        // 3. Puedes agregar controladores manuales a la lista (aquellos sin @Discovered o con automatic=false)
        controllers.add(MiControladorManual.class);

        // ... Configuración de OpenAPI usando la lista 'controllers' ...

        // 4. Registrar automáticamente en el servidor todos los controladores descubiertos
        JettraRestServer.registerDiscovered(server, Main.class);

        // 5. Registrar manualmente las excepciones (clases sin @Discovered o automatic=false)
        JettraRestServer.register(server, MiControladorManual.class);

        server.start();
    }
}
```

## Flujo de Trabajo

1. El compilador detecta `@Discovered` en tus controladores y crea un archivo de índice.
2. Tu clase `Main` arranca el servidor.
3. Llamas a `DiscoveredRegistry.getDiscoveredClasses(Main.class)` o `JettraRestServer.registerDiscovered(...)`.
4. Jettra verifica que `Main.class` tenga `@DiscoveredLoad`.
5. Si lo tiene, Jettra lee rápidamente los archivos de índice generados y carga/registra los controladores. Las clases que tienen `@Discovered(automatic = false)` se filtran y son ignoradas en este proceso, requiriendo registro manual (Paso 5 en el ejemplo).
