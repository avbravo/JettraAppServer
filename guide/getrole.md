# Obtener el Rol de Usuario en la Sesión

Para simplificar la obtención del rol del usuario en la sesión actual en el sistema Jettra, se ha añadido un método utilitario en el enumerador `Scope` de `JettraContext`.

## Uso

Anteriormente, para obtener el rol, se requería extraer el contexto manualmente y acceder a las variables de la sesión de la siguiente manera:

```java
io.jettra.server.core.JettraContext ctx = io.jettra.server.core.JettraContext.getCurrent();
String role = ctx != null ? (String) ctx.get(io.jettra.server.core.JettraContext.Scope.SESSION, "role") : "";
```

A partir de esta actualización, puedes obtener el rol del usuario logueado en la sesión activa de forma simplificada:

```java
String role = io.jettra.server.core.JettraContext.Scope.SESSION.getRole();
```

Esto devolverá un `String` con el rol del usuario (por ejemplo, `"ADMIN"`, `"MANAGER"`, etc.) o una cadena vacía en caso de que no exista un contexto de sesión activo o no haya un rol definido.
