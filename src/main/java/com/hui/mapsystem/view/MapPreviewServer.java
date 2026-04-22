package com.hui.mapsystem.view;

import com.hui.mapsystem.model.GameMap;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

/**
 * 启动本地 HTTP 服务并展示地图预览。
 */
public final class MapPreviewServer {
    private static final int DEFAULT_PORT = 8765;

    private MapPreviewServer() {
    }

    /**
     * 启动地图预览服务。
     *
     * @param args 第一个参数可选，表示模板 id；第二个参数可选，表示端口。
     * @throws IOException 当端口绑定或响应输出失败时抛出。
     */
    public static void main(String[] args) throws IOException {
        String templateId = args.length > 0 ? args[0] : "starter-map";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;
        Path previewPath = exportPreview(templateId);

        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/", new PreviewHandler(templateId, previewPath));
        httpServer.setExecutor(Executors.newSingleThreadExecutor());
        httpServer.start();

        System.out.println("Map preview server started: http://127.0.0.1:" + port + "/");
        System.out.println("Serving template: " + templateId);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> httpServer.stop(0)));
    }

    private static Path exportPreview(String templateId) {
        GameMap gameMap = GameMap.fromTemplate(templateId);
        Path outputPath = Path.of("target", "map-preview", templateId + ".html");
        return MapHtmlRenderer.export(gameMap, outputPath);
    }

    private static final class PreviewHandler implements HttpHandler {
        private final String templateId;
        private final Path previewPath;

        private PreviewHandler(String templateId, Path previewPath) {
            this.templateId = templateId;
            this.previewPath = previewPath;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            if ("/".equals(requestPath) || ("/" + templateId + ".html").equals(requestPath)) {
                byte[] responseBody = Files.readAllBytes(previewPath);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, responseBody.length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(responseBody);
                }
                return;
            }

            byte[] responseBody = "Not Found".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, responseBody.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(responseBody);
            }
        }
    }
}