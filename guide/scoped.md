# Ciclos de Vida y Contextos de Jettra

JettraAppServer provee un motor de inyección de dependencias robusto, soportado por anotaciones que definen el tiempo de vida (scope) de las instancias gestionadas (beans).

## Anotaciones Soportadas

*   **`@RequestScoped`**: El componente nace y muere con la petición HTTP actual. `JettraContext.destroyRequest()` limpia esta capa al finalizar la llamada.
*   **`@SessionScoped`**: El componente perdura durante la sesión activa del usuario. El ciclo de vida finaliza al expirar la sesión (gestionado por el hilo limpiador de `JettraContext` o al llamar explícitamente a `logout`).
*   **`@ApplicationScoped`**: Singleton por aplicación. Vive mientras el servidor `JettraAppServer` siga en ejecución.
*   **`@ViewScoped`**: Atado al ciclo de vida de la vista actual renderizada. Útil para mantener estado en Single Page Applications o flujos multipaso.
*   **`@ClientScoped`**: Persistente en base a identificadores de cliente, compartiendo memoria a lo largo de varias sesiones del mismo dispositivo.
*   **`@WindowScoped`**: Específico a una pestaña o ventana particular del navegador.

## Reglas de Resolución de Dependencias

El motor, `DependencyInjector`, analiza las clases mediante reflection.
Si un campo está anotado con `@Inject`, busca en el `JettraContext` la instancia correspondiente según su Scope.

**Ejemplo de Uso:**

```java
@SessionScoped
public class MiCarrito {
    // ...
}

public class CheckoutPage {
    @Inject
    private MiCarrito carrito;
}
```

Al resolver `CheckoutPage`, el inyector buscará `MiCarrito` en el mapa de sesión. Si no existe, creará una instancia nueva y la registrará automáticamente.
