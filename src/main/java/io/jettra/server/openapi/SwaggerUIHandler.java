package io.jettra.server.openapi;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class SwaggerUIHandler implements HttpHandler {

    private final String openApiUrl;

    public SwaggerUIHandler(String openApiUrl) {
        this.openApiUrl = io.jettra.server.JettraServer.resolvePath(openApiUrl);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String html = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"utf-8\" />\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n" +
                "  <meta name=\"description\" content=\"SwaggerUI\" />\n" +
                "  <title>SwaggerUI</title>\n" +
                "  <link rel=\"stylesheet\" href=\"https://unpkg.com/swagger-ui-dist@5.9.0/swagger-ui.css\" />\n" +
                "</head>\n" +
                "<body>\n" +
                "<div id=\"swagger-ui\"></div>\n" +
                "<script src=\"https://unpkg.com/swagger-ui-dist@5.9.0/swagger-ui-bundle.js\" crossorigin></script>\n" +
                "<script>\n" +
                "  window.onload = () => {\n" +
                "    window.ui = SwaggerUIBundle({\n" +
                "      url: '" + openApiUrl + "',\n" +
                "      dom_id: '#swagger-ui',\n" +
                "    });\n" +
                "  };\n" +
                "</script>\n" +
                "</body>\n" +
                "</html>";

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        byte[] response = html.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, response.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
}
