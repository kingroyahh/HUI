package com.hui.mapsystem.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hui.mapsystem.model.SquareCoordinate;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * 启动本地 HTTP 服务，以地图可视化为游戏入口。
 * 提供游戏初始页面和 REST 风格的游戏指令接口：
 * <ul>
 *   <li>GET  {@code /}          — 游戏 HTML 主页</li>
 *   <li>GET  {@code /api/state} — 当前游戏状态 JSON</li>
 *   <li>POST {@code /api/move}  — 移动实体，请求体 {@code {"entityId","toHexX","toHexY"}}</li>
 *   <li>GET  {@code /api/path}  — 路径预览，参数 {@code entityId, toHexX, toHexY}</li>
 * </ul>
 */
public final class MapPreviewServer {

    private static final int DEFAULT_PORT = 8765;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private MapPreviewServer() {
    }

    /**
     * 启动游戏服务。
     *
     * @param args 第一个参数可选，表示模板 id；第二个参数可选，表示端口。
     * @throws IOException 当端口绑定失败时抛出。
     */
    public static void main(String[] args) throws IOException {
        String templateId = args.length > 0 ? args[0] : "starter-map";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        GameSession session = new GameSession(templateId);
        String initialHtml = MapHtmlRenderer.renderGame(session.getGameMap());

        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/", new GameHandler(session, initialHtml));
        // HTTP 请求线程池独立于 LogicThread，避免阻塞游戏逻辑
        httpServer.setExecutor(Executors.newFixedThreadPool(4));
        httpServer.start();

        System.out.println("Game server started: http://127.0.0.1:" + port + "/");
        System.out.println("Template: " + templateId);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            httpServer.stop(0);
            session.shutdown();
        }));
    }

    private static final class GameHandler implements HttpHandler {

        private final GameSession session;
        private final String initialHtml;

        private GameHandler(GameSession session, String initialHtml) {
            this.session = session;
            this.initialHtml = initialHtml;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            try {
                switch (path) {
                    case "/", "/index.html" -> serveHtml(exchange, initialHtml);
                    case "/api/state" -> serveJson(exchange, MAPPER.writeValueAsString(session.snapshot()));
                    case "/api/move" -> handleMove(exchange);
                    case "/api/path" -> handlePath(exchange);
                    default -> send(exchange, 404, "text/plain", "Not Found");
                }
            } catch (Exception exception) {
                String body = "{\"error\":\"" + exception.getMessage() + "\"}";
                send(exchange, 500, "application/json", body);
            }
        }

        private void handleMove(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, "text/plain", "Method Not Allowed");
                return;
            }
            JsonNode body = MAPPER.readTree(exchange.getRequestBody());
            String entityId = body.get("entityId").asText();
            int toHexX = body.get("toHexX").asInt();
            int toHexY = body.get("toHexY").asInt();

            List<SquareCoordinate> path;
            try {
                // 投递到 LogicThread 执行，HTTP 线程等待结果
                path = session.submitMove(entityId, new SquareCoordinate(toHexX, toHexY)).get();
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                send(exchange, 500, "application/json", "{\"error\":\"interrupted\"}");
                return;
            } catch (ExecutionException executionException) {
                send(exchange, 500, "application/json", "{\"error\":\"move failed\"}");
                return;
            }

            boolean ok = !path.isEmpty();
            List<List<Integer>> pathJson = path.stream()
                .map(c -> List.of(c.hexX(), c.hexY()))
                .toList();
            String json = MAPPER.writeValueAsString(new MoveResponse(ok, pathJson, session.snapshot()));
            serveJson(exchange, json);
        }

        private void handlePath(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (query == null) {
                send(exchange, 400, "text/plain", "Missing query parameters");
                return;
            }
            String entityId = null;
            int toHexX = -1;
            int toHexY = -1;
            for (String param : query.split("&")) {
                String[] kv = param.split("=", 2);
                if (kv.length == 2) {
                    switch (kv[0]) {
                        case "entityId" -> entityId = kv[1];
                        case "toHexX" -> toHexX = Integer.parseInt(kv[1]);
                        case "toHexY" -> toHexY = Integer.parseInt(kv[1]);
                    }
                }
            }
            if (entityId == null || toHexX < 0 || toHexY < 0) {
                send(exchange, 400, "text/plain", "Missing entityId, toHexX or toHexY");
                return;
            }
            List<SquareCoordinate> path = session.previewPath(entityId, new SquareCoordinate(toHexX, toHexY));
            List<List<Integer>> pathJson = path.stream()
                .map(c -> List.of(c.hexX(), c.hexY()))
                .toList();
            serveJson(exchange, MAPPER.writeValueAsString(new PathResponse(pathJson)));
        }

        private void serveHtml(HttpExchange exchange, String html) throws IOException {
            send(exchange, 200, "text/html; charset=UTF-8", html);
        }

        private void serveJson(HttpExchange exchange, String json) throws IOException {
            send(exchange, 200, "application/json; charset=UTF-8", json);
        }

        private void send(HttpExchange exchange, int status, String contentType, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        }
    }

    private record MoveResponse(boolean ok, List<List<Integer>> path, GameSession.GameStateSnapshot state) {}

    private record PathResponse(List<List<Integer>> path) {}
}
