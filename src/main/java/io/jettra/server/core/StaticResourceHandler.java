package io.jettra.server.core;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StaticResourceHandler implements HttpHandler {

    private final String prefix;

    public StaticResourceHandler(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        
        String contextPath = io.jettra.server.JettraServer.getContextPath();
        if (!contextPath.equals("/") && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        
        // Serve from classpath (e.g. /static/...)
        // Remove leading slash for classpath lookup if it starts with /
        String resourcePath = path.startsWith("/") ? path.substring(1) : path;
        
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        }

        if (is == null) {
            String response = "404 Not Found: " + path;
            exchange.sendResponseHeaders(404, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
            return;
        }

        String mimeType = getMimeType(path);
        if (mimeType != null) {
            exchange.getResponseHeaders().set("Content-Type", mimeType);
        }
        
        // Cache control for static assets
        exchange.getResponseHeaders().set("Cache-Control", "public, max-age=31536000");

        exchange.sendResponseHeaders(200, 0); // chunked response
        try (OutputStream os = exchange.getResponseBody()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = is.read(buffer)) != -1) {
                os.write(buffer, 0, count);
            }
        } finally {
            is.close();
        }
    }

    private String getMimeType(String path) {
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".woff")) return "font/woff";
        if (path.endsWith(".woff2")) return "font/woff2";
        if (path.endsWith(".ttf")) return "font/ttf";
        if (path.endsWith(".eot")) return "application/vnd.ms-fontobject";
        return "application/octet-stream";
    }
}
