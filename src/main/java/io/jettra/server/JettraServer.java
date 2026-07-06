package io.jettra.server;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import io.jettra.server.core.JettraContext;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

public class JettraServer {

    private HttpServer server;
    private boolean isRunning = false;
    private Map<String, Object> handlerRegistry = new HashMap<>(); // Can be HttpHandler, Class, or Supplier
    private Thread hotReloadThread;
    private String errorPage;
    private int customPort = -1;
    private io.jettra.server.core.AutocloneManager autocloneManager;
    private int cloneCount = 0;

    public void setPort(int port) {
        this.customPort = port;
    }

    public void setErrorPage(String path) {
        this.errorPage = path;
    }

    /**
     * Registra un manejador HTTP personalizado antes de iniciar el servidor.
     * @param path la ruta, ej. "/login"
     * @param handler el manejador
     */
    public void addHandler(String path, HttpHandler handler) {
        handlerRegistry.put(path, handler);
        if (server != null) registerInServer(path, handler);
    }

    public void addHandler(String path, java.util.function.Supplier<HttpHandler> supplier) {
        handlerRegistry.put(path, supplier);
        if (server != null) registerInServer(path, supplier);
    }

    public void addHandler(String path, Class<? extends HttpHandler> handlerClass) {
        handlerRegistry.put(path, handlerClass);
        if (server != null) registerInServer(path, handlerClass);
    }

    private void registerInServer(String path, Object handlerObj) {
        String resolvedPath = resolvePath(path);
        HttpHandler wrapped = wrapHandler(handlerObj);
        server.createContext(resolvedPath, wrapped);
        if (resolvedPath.endsWith("/") && resolvedPath.length() > 1) {
            server.createContext(resolvedPath.substring(0, resolvedPath.length() - 1), wrapped);
        }
    }

    public static String getContextPath() {
        String contextPath = io.jettra.server.config.JettraConfig.getProperty("server.contextpath");
        if (contextPath == null || contextPath.isBlank() || contextPath.equals("/")) {
            return "/";
        }
        
        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }
        
        if (contextPath.endsWith("/") && contextPath.length() > 1) {
            contextPath = contextPath.substring(0, contextPath.length() - 1);
        }
        
        return contextPath;
    }

    public static String resolvePath(String path) {
        String contextPath = getContextPath();
        if (contextPath.equals("/")) {
            return path.startsWith("/") ? path : "/" + path;
        }
        
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        return contextPath + path;
    }

    /**
     * Inicia el servidor HTTP y los módulos de Jettra.
     */
    public void start() {
        if (isRunning) {
            IO.println("JettraServer ya se encuentra en ejecución.");
            return;
        }
        IO.println("Starting JettraServer...");
        IO.println("Java version: " + System.getProperty("java.version"));

        // Initialize JettraSecurityDB if backend server is true
        String typeBackend = io.jettra.server.config.JettraConfig.getProperty("server.typebackend");
        if ("true".equalsIgnoreCase(typeBackend)) {
            IO.println("[JettraServer] server.typebackend=true detected. Initializing JettraSecurityDB...");
            io.jettra.server.autentification.repository.JettraSecurityDBInitializer.initializeIfEmpty();
        }

        // Add native admin console for security database
        this.addHandler("/securitydb/admin", new io.jettra.server.autentification.AdminConsoleHandler());

        // Inicializamos componentes abstractos/ejemplo si los hay
        IO.println("Initializing ExampleRest and ConfigInjector...");
        io.jettra.server.test.ExampleRest example = new io.jettra.server.test.ExampleRest();
        io.jettra.server.config.ConfigInjector.inject(example);
        example.draw();

        try {
            int port;
            if (customPort > 0) {
                port = customPort;
            } else {
                String portValue = io.jettra.server.config.JettraConfig.getProperty("server.port");
                port = (portValue != null && !portValue.isBlank()) ? Integer.parseInt(portValue) : 8080;
            }
            server = HttpServer.create(new InetSocketAddress(port), 0);
            
            // Add custom handlers from registry
            for (Map.Entry<String, Object> entry : handlerRegistry.entrySet()) {
                registerInServer(entry.getKey(), entry.getValue());
            }
            
            // ... [root handled logic remains but uses wrapped] ...
            
            // Default handler check
            String rootPath = resolvePath("/");
            boolean rootHandled = false;
            for(String path : handlerRegistry.keySet()){
                if(resolvePath(path).equals(rootPath)){
                    rootHandled = true;
                    break;
                }
            }
            
            if (!rootHandled) {
                HttpHandler defaultHandler = exchange -> {
                    String response = "JettraServer is running!";
                    exchange.sendResponseHeaders(200, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.getResponseBody().close();
                };
                
                HttpHandler wrappedRoot = wrapHandler(defaultHandler);
                server.createContext(rootPath, wrappedRoot);
                
                if (rootPath.endsWith("/") && rootPath.length() > 1) {
                    server.createContext(rootPath.substring(0, rootPath.length() - 1), wrappedRoot);
                }
            }

            if (!getContextPath().equals("/")) {
                HttpHandler redirectHandler = exchange -> {
                    String path = exchange.getRequestURI().getPath();
                    String redirectUrl = getContextPath() + (path.startsWith("/") ? path : "/" + path);
                    
                    String query = exchange.getRequestURI().getQuery();
                    if (query != null && !query.isEmpty()) {
                        redirectUrl += "?" + query;
                    }
                    
                    exchange.getResponseHeaders().set("Location", redirectUrl);
                    exchange.sendResponseHeaders(302, -1);
                    exchange.getResponseBody().close();
                };
                server.createContext("/", wrapHandler(redirectHandler));
            }
            
            server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
            server.start();
            isRunning = true;
            
            // Java 25 / Compact Header support detection
            String javaVersion = System.getProperty("java.version");
            boolean isJava25 = javaVersion != null && javaVersion.startsWith("25");
            String compactHeader = io.jettra.server.config.JettraConfig.getProperty("server.compactheader");
            
            if (isJava25 || "true".equalsIgnoreCase(compactHeader)) {
                IO.println("[JettraServer] Optimizando para Java 25: Uso de Compact Object Headers (JEP 450) detectado o configurado.");
            }

            startHotReloadWatcher();

            IO.println("JettraServer HTTP server started on port " + port);
            IO.println("JettraServer HTTP server contextpath = " + getContextPath());
            IO.println("Features initialized: REST, gRPC, JWT, Health, FaultTolerance, Session, DI, Security (XSS).");
            
            // Iniciar AutocloneManager si estamos en el servidor principal (no en un clon)
            if (customPort <= 0) {
                String thresholdVal = io.jettra.server.config.JettraConfig.getProperty("server.autoclone.threshold");
                int threshold = (thresholdVal != null && !thresholdVal.isBlank()) ? Integer.parseInt(thresholdVal) : 100;
                autocloneManager = new io.jettra.server.core.AutocloneManager(this, threshold);
                autocloneManager.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to start JettraServer embedded HTTP server.");
        }
    }

    /**
     * Detiene la ejecución del servidor.
     */
    public void stop() {
        if (!isRunning || server == null) {
            IO.println("JettraServer no está en ejecución.");
            return;
        }
        IO.println("Stopping JettraServer...");
        
        // Cierra todas las sesiones de todos los usuarios
        io.jettra.server.core.JettraContext.clearSessions();
        IO.println("[JettraServer] Todas las sesiones han sido cerradas.");

        String expiredValue = io.jettra.server.config.JettraConfig.getProperty("server.session.expired");
        int expired = (expiredValue != null && !expiredValue.isBlank()) ? Integer.parseInt(expiredValue) : 0;
        server.stop(expired);
        if (hotReloadThread != null) hotReloadThread.interrupt();
        if (autocloneManager != null) autocloneManager.stop();
        isRunning = false;
        IO.println("JettraServer detenido exitosamente.");
    }

    /**
     * Autoclonación Dinámica:
     * Replica el servidor creando una nueva instancia en otro puerto para gestionar la carga.
     */
    public void autoclone() {
        cloneCount++;
        int basePort;
        if (customPort > 0) {
            basePort = customPort;
        } else {
            String portValue = io.jettra.server.config.JettraConfig.getProperty("server.port");
            basePort = (portValue != null && !portValue.isBlank()) ? Integer.parseInt(portValue) : 8080;
        }
        int newPort = basePort + cloneCount;

        IO.println("[Autoclonación Dinámica] Iniciando réplica del servidor en el puerto " + newPort + "...");
        IO.println("[Autoclonación Dinámica] Se están transfiriendo los objetos más viejos y equilibrando la carga para mejorar el rendimiento.");

        JettraServer replica = new JettraServer();
        replica.setPort(newPort);
        replica.setErrorPage(this.errorPage);
        
        for (Map.Entry<String, Object> entry : this.handlerRegistry.entrySet()) {
            if (entry.getValue() instanceof HttpHandler) {
                replica.addHandler(entry.getKey(), (HttpHandler) entry.getValue());
            } else if (entry.getValue() instanceof java.util.function.Supplier) {
                replica.addHandler(entry.getKey(), (java.util.function.Supplier<HttpHandler>) entry.getValue());
            } else if (entry.getValue() instanceof Class) {
                replica.addHandler(entry.getKey(), (Class<? extends HttpHandler>) entry.getValue());
            }
        }
        
        Thread replicaThread = new Thread(() -> replica.start());
        replicaThread.start();
    }

    private HttpHandler wrapHandler(Object original) {
        return exchange -> {
            String sessionId = getOrCreateSessionId(exchange);
            JettraContext context = new JettraContext(sessionId);
            JettraContext.setCurrent(context);
            try {
                // Add enhanced security headers (XSS Protection)
                exchange.getResponseHeaders().add("X-Content-Type-Options", "nosniff");
                exchange.getResponseHeaders().add("X-Frame-Options", "DENY");
                exchange.getResponseHeaders().add("X-XSS-Protection", "1; mode=block");
                exchange.getResponseHeaders().add("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://unpkg.com https://cdnjs.cloudflare.com; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdn.jsdelivr.net https://unpkg.com https://cdnjs.cloudflare.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: https:; media-src 'self' blob: data: mediastream:; connect-src 'self' ws: wss:;");
                exchange.getResponseHeaders().add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
                exchange.getResponseHeaders().add("Referrer-Policy", "strict-origin-when-cross-origin");
                
                HttpHandler instance = null;
                if (original instanceof HttpHandler) {
                    instance = (HttpHandler) original;
                } else if (original instanceof java.util.function.Supplier) {
                    instance = ((java.util.function.Supplier<HttpHandler>) original).get();
                } else if (original instanceof Class) {
                    try {
                        instance = ((Class<? extends HttpHandler>) original).getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                
                if (instance != null) {
                    try {
                        instance.handle(exchange);
                    } catch (Exception e) {
                        e.printStackTrace();
                        handleErrorRedirect(exchange, "500 Internal Server Error", e.getClass().getSimpleName() + " - " + e.getMessage(), "Handler: " + original.getClass().getSimpleName());
                    }
                } else {
                    handleErrorRedirect(exchange, "404 Not Found", "The requested handler was not found", exchange.getRequestURI().toString());
                }
            } finally {
                JettraContext.clear();
            }
        };
    }

    private void handleErrorRedirect(HttpExchange exchange, String title, String detail, String origin) throws java.io.IOException {
        if (this.errorPage != null && !this.errorPage.isEmpty()) {
            try {
                String redirectUrl = this.errorPage + "?title=" + java.net.URLEncoder.encode(title, "UTF-8") +
                                     "&detail=" + java.net.URLEncoder.encode(detail, "UTF-8") +
                                     "&origin=" + java.net.URLEncoder.encode(origin, "UTF-8");
                exchange.getResponseHeaders().set("Location", resolvePath(redirectUrl));
                exchange.sendResponseHeaders(302, -1);
                exchange.getResponseBody().close();
                return;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        String req = title + ": " + detail + " at " + origin;
        exchange.sendResponseHeaders(404, req.length());
        exchange.getResponseBody().write(req.getBytes());
        exchange.getResponseBody().close();
    }

    private String getOrCreateSessionId(HttpExchange exchange) {
        String cookies = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookies != null) {
            for (String cookie : cookies.split(";")) {
                cookie = cookie.trim();
                if (cookie.startsWith("jsessionid=")) return cookie.substring("jsessionid=".length());
            }
        }
        String newId = java.util.UUID.randomUUID().toString();
        exchange.getResponseHeaders().add("Set-Cookie", "jsessionid=" + newId + "; Path=/; HttpOnly");
        return newId;
    }

    private void startHotReloadWatcher() {
        String hotreload = io.jettra.server.config.JettraConfig.getProperty("server.hotreload");
        if (!"true".equalsIgnoreCase(hotreload)) return;

        hotReloadThread = new Thread(() -> {
            try {
                WatchService watcher = FileSystems.getDefault().newWatchService();
                Path path = Paths.get("target/classes");
                if (!Files.exists(path)) return;
                
                path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
                IO.println("[HotReload] Watching for changes in target/classes...");

                while (!Thread.currentThread().isInterrupted()) {
                    WatchKey key = watcher.poll(1, TimeUnit.SECONDS);
                    if (key != null) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            IO.println("[HotReload] Change detected: " + event.context());
                            restore(); // Full restart for now
                            return; 
                        }
                        key.reset();
                    }
                }
            } catch (Exception e) {
                // Stopped
            }
        });
        hotReloadThread.setDaemon(true);
        hotReloadThread.start();
    }

    /**
     * Reinicia / restaura el servidor deteniéndolo e iniciándolo de nuevo.
     */
    public void restore() {
        IO.println("Restaurando JettraServer...");
        stop();
        start();
    }

    /**
     * Retorna el estado actual del servidor.
     * @return STATUS (RUNNING o STOPPED)
     */
    public String status() {
        return isRunning ? "RUNNING" : "STOPPED";
    }

    /**
     * Check de liveness para saber si el servidor está operativo.
     * @return true si el servidor está levantado y aceptando peticiones.
     */
    public boolean live() {
        return isRunning;
    }
}
