# Creador de Plugins Jettra (PluginCLI)

Esta guía explica cómo utilizar el generador automático de plugins provisto por el componente `JettraAppServer`. 

El comando `PluginCLI` facilita la creación, instalación y eliminación de plugins JettraFlux a través de la terminal mediante una generación 100% autónoma y plantillas nativas, garantizando un entorno aislado.

---

## El script `mvn-jettra`

Para facilitar la ejecución de estas tareas y omitir verbosidad en la consola, se utiliza el script bash `mvn-jettra`.
**Nota importante**: A partir de las últimas versiones, el motor `JettraServer` auto-generará de forma dinámica este script en el directorio raíz de la ejecución si el archivo no existe, inyectándole automáticamente los permisos requeridos (`chmod +x`). Simplemente arranca tu servidor Jettra local para tenerlo disponible.

```bash
#!/bin/bash
if [ "$1" = "-jettra" ]; then
    shift
fi

# Execute the CLI tool using the local pom.xml
mvn -q exec:java -Dexec.mainClass="io.jettra.server.cli.PluginCLI" -Dexec.args="$*"
```

---

## Comandos y Sintaxis

### 1. Generar un Plugin
 Generar un plugin a partir de un proyecto        
Este se ejecuta desde consola como por ejemplo

-path: Ruta del directorio donde se creara el plugin
-name: Nombre del plugin 
exclude-package: Excluye de la generación todos los documentos dentro de los paquetes especificados.
exclude-class: Excluye clases especificas que no se desean migrar
incluye-test: Indica que se pasarán también los test al plugin. Valores son yes|no



Existen múltiples maneras de generar una estructura de proyecto para tu nuevo plugin. La recomendación es usar siempre los parámetros explícitos `-path` y `-name` para mayor seguridad.

**Sintaxis Recomendada (Uso de Flags):**
```bash
./mvn-jettra generate-plugin -path <directorio_destino> -name <NombrePlugin> [opciones]
```
Ejemplo con exclusión de paquetes, clases e inclusión de tests:
```bash
./mvn-jettra generate-plugin -path /home/avbravo/Descargas -name MiNuevoPlugin exclude-plugin plugin1,plugin2 exclude-package com.avbravo.general, com.avbravo.prueba exclude-class Clase1.java, Clase2.java incluye-test yes
```

**Parámetros Opcionales de Generación:**
- `exclude-plugin`: Excluye plugins específicos de la generación.
- `exclude-package`: Excluye de la migración todos los documentos dentro de los paquetes especificados (ej: `com.avbravo.general, com.avbravo.prueba`).
- `exclude-class`: Excluye clases específicas que no se desean migrar (ej: `Clase1.java, Clase2.java`).
- `incluye-test`: Indica si se copiarán también las clases y recursos de pruebas (`src/test/java` y `src/test/resources`) al nuevo plugin. Valores permitidos: `yes` | `no`.

**Sintaxis Simplificada (Directorio Actual):**
Si omites `-path`, el generador construirá el plugin en el directorio donde te encuentres ubicado:
```bash
./mvn-jettra generate-plugin ReportesPlugin exclude-plugin VentasPlugin,InventarioPlugin exclude-package com.avbravo.general exclude-class Clase1.java incluye-test yes
```

#### **¿Qué hace `generate-plugin` bajo el capó?**
- **Generación Autónoma**: El generador no requiere clonar ningún proyecto existente; genera internamente y desde cero todos los directorios.
- **Auto-Resolución de Versiones**: Automáticamente lee el `pom.xml` del directorio desde donde estás ejecutando el comando para extraer las versiones correctas de Jettra (`<jettra.*.version>`), y luego las inyecta en el nuevo pom del plugin para asegurar compatibilidad exacta.
- **Generación del POM**: Crea un `pom.xml` para el plugin asignando el `groupId` a `io.jettraflux.<nombre-plugin-minuscula>`, y agrega dependencias limpias.
- **Multilingüismo Localizado**: Escribe los archivos de propiedades (ej: `messages-ReportesPlugin_es.properties`) utilizando el nombre único del plugin.
- **Página de Entrada**: Escribe la clase `Main<NombrePlugin>Page.java` con las dependencias heredadas y empleando la etiqueta `@InjectProperties(name = "messages-<nombre-plugin>")` vinculada a sus propios resources.
- **Descriptor de Restricciones**: Genera el archivo `plugin-descriptor.md` en la raíz del plugin para que incluyas las definiciones de WidgetLets y layouts.

### 2. Instalar un Plugin en tu Proyecto

Para instalar un plugin generado (o externo) dentro del proyecto actual en el que estás trabajando, debes pasarle la ruta donde se encuentra el código fuente del plugin:
```bash
./mvn-jettra install-plugin /ruta/absoluta/a/ReportesPlugin
```
*(También puedes usar una ruta relativa al directorio donde ejecutas el comando)*

#### **¿Qué hace `install-plugin` bajo el capó?**
1. **Compilación y Empaquetado**: Ejecuta `mvn clean install` dentro del directorio del plugin para compilarlo e instalarlo en tu repositorio Maven local (`~/.m2`).
2. **Inyección de Dependencia**: Lee el `pom.xml` del plugin extraído y automáticamente agrega la etiqueta `<dependency>` correspondiente en el `pom.xml` de tu proyecto actual.
3. **Inyección de Menús**: Analiza el archivo `plugin-descriptor.md` dentro de la carpeta del plugin. Si encuentra bloques de código definiendo menús (variables `WidgetLet`), buscará tu archivo `TemplatePage.java` local e inyectará ese código automáticamente. Finalmente, agregará dichas variables directamente en la invocación de la barra lateral `Left.of(...)` para que los menús aparezcan inmediatamente tras reiniciar tu servidor.

### 3. Remover un Plugin

Comando para gestionar la eliminación lógica del entorno (no borra archivos físicamente para prevenir pérdidas):
```bash
./mvn-jettra remove-plugin ReportesPlugin
```

---

## Ejemplo de Configuración en `.m2/settings.xml`

Para un correcto funcionamiento e integración de repositorios y descargas, Maven emplea su archivo de configuración global `settings.xml`. Si llegaras a enfrentar un error de la consola, como advertencias de "Unrecognised tag", asegúrate de que el archivo `~/.m2/settings.xml` contenga una sintaxis válida. 

A continuación, un ejemplo de estructura correcta para dicho archivo:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">

    <localRepository>${user.home}/.m2/repository</localRepository>

    <pluginGroups>
        <!-- pluginGroup
         | Specifying a pluginGroup forces Maven to search the group's repository for plugins
         | if no groupId is supplied.
        -->
        <pluginGroup>org.apache.maven.plugins</pluginGroup>
    </pluginGroups>

    <profiles>
        <profile>
            <id>jettra-profile</id>
            <properties>
                <maven.compiler.source>25</maven.compiler.source>
                <maven.compiler.target>25</maven.compiler.target>
            </properties>
            <repositories>
                <!-- Define repositorios si cuentas con fuentes privadas -->
            </repositories>
        </profile>
    </profiles>

    <activeProfiles>
        <!-- Asegura la activación del perfil de tu entorno -->
        <activeProfile>jettra-profile</activeProfile>
    </activeProfiles>

</settings>
```

> [!TIP]
> **Silenciando Salidas (Quiet Mode):**
> Al usar el wrapper `mvn-jettra`, todas las salidas intrusivas o advertencias nativas de Maven (como un `settings.xml` mal formado) son omitidas por diseño gracias al flag `-q`. Sin embargo, es buena práctica mantener tus settings pulcros para otros usos.
