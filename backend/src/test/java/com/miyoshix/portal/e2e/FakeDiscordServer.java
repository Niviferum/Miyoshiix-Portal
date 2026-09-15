package com.miyoshix.portal.e2e;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Faux serveur OAuth2 Discord pour les tests E2E. Expose l'endpoint de jeton et
 * l'endpoint de profil, et enregistre les requêtes reçues.
 */
final class FakeDiscordServer {

    static final String ACCESS_TOKEN = "jeton-de-test";

    /** Requête reçue par le faux serveur. */
    record ReceivedRequest(String method, String path, String authorization, String body) {
    }

    private final HttpServer server;
    private final List<ReceivedRequest> requests = new CopyOnWriteArrayList<>();
    private volatile String userJson = "{}";

    FakeDiscordServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/oauth2/token", this::handleToken);
        server.createContext("/api/v10/users/@me", this::handleUser);
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /** Définit le profil renvoyé par {@code /users/@me}. */
    void respondWithUser(String json) {
        userJson = json;
    }

    List<ReceivedRequest> requests() {
        return List.copyOf(requests);
    }

    void reset() {
        requests.clear();
        userJson = "{}";
    }

    void stop() {
        server.stop(0);
    }

    private void handleToken(HttpExchange exchange) throws IOException {
        record(exchange);
        send(exchange, 200, """
                {"access_token":"%s","token_type":"Bearer","expires_in":3600,"scope":"identify"}
                """.formatted(ACCESS_TOKEN));
    }

    private void handleUser(HttpExchange exchange) throws IOException {
        ReceivedRequest request = record(exchange);
        if (!("Bearer " + ACCESS_TOKEN).equals(request.authorization())) {
            send(exchange, 401, "{\"message\":\"401: Unauthorized\"}");
            return;
        }
        send(exchange, 200, userJson);
    }

    private ReceivedRequest record(HttpExchange exchange) throws IOException {
        String body;
        try (InputStream in = exchange.getRequestBody()) {
            body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        ReceivedRequest request = new ReceivedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getPath(),
                exchange.getRequestHeaders().getFirst("Authorization"),
                body);
        requests.add(request);
        return request;
    }

    private static void send(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
