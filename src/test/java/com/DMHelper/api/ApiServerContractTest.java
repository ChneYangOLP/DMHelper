package com.DMHelper.api;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiServerContractTest {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static ConfigurableApplicationContext app;

    @BeforeAll
    static void startServer() {
        app = ApiServer.start(0);
    }

    @AfterAll
    static void stopServer() {
        if (app != null) app.close();
    }

    @Test
    void electronCanCheckReadinessWithItsToken() throws Exception {
        var response = send("/api/ping", "GET", null, "null", ApiServer.getClientToken());
        assertEquals(200, response.statusCode());
        assertEquals("pong", response.body());
        assertEquals("null", response.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
        assertEquals("Origin", response.headers().firstValue("Vary").orElseThrow());
    }

    @Test
    void preflightAndTokenChecksRetainTheLocalClientPolicy() throws Exception {
        var preflight = send("/api/combat/sessions", "OPTIONS", null, "http://localhost:5173", null);
        assertEquals(204, preflight.statusCode());
        assertEquals("http://localhost:5173", preflight.headers().firstValue("Access-Control-Allow-Origin").orElseThrow());
        assertTrue(preflight.headers().firstValue("Access-Control-Allow-Headers").orElseThrow().contains("X-DMHelper-Token"));
        assertEquals(403, send("/api/ping", "GET", null, "https://example.invalid", ApiServer.getClientToken()).statusCode());
        assertEquals(401, send("/api/ping", "GET", null, null, null).statusCode());
    }

    @Test
    void oversizedJsonIsRejectedBeforeTheControllerRuns() throws Exception {
        var response = send("/api/characters", "POST", " ".repeat(10_000_001), null, ApiServer.getClientToken());
        assertEquals(413, response.statusCode());
    }

    private static HttpResponse<String> send(String path, String method, String body, String origin, String token) throws Exception {
        int port = app.getEnvironment().getRequiredProperty("local.server.port", Integer.class);
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (origin != null) request.header("Origin", origin);
        if (token != null) request.header("X-DMHelper-Token", token);
        return CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
