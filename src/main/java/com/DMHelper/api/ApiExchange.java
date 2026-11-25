package com.DMHelper.api;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

final class ApiExchange {
    private static final int MAX_BODY_BYTES = 10_000_000;
    private static final MediaType TEXT_UTF8 = new MediaType("text", "plain", StandardCharsets.UTF_8);

    private final ServerRequest request;
    private final String body;
    private int status = 200;
    private MediaType contentType = TEXT_UTF8;
    private String result;

    private ApiExchange(ServerRequest request, byte[] body) {
        this.request = request;
        this.body = new String(body, StandardCharsets.UTF_8);
    }

    static HandlerFunction<ServerResponse> handle(Consumer<ApiExchange> handler) {
        return request -> {
            byte[] body = request.servletRequest().getInputStream().readNBytes(MAX_BODY_BYTES + 1);
            if (body.length > MAX_BODY_BYTES) {
                return ServerResponse.status(413).contentType(TEXT_UTF8).body("Request body too large");
            }
            ApiExchange exchange = new ApiExchange(request, body);
            handler.accept(exchange);
            return exchange.response();
        };
    }

    String body() {
        return body;
    }

    String pathParam(String name) {
        return request.pathVariable(name);
    }

    String queryParam(String name) {
        return request.param(name).orElse(null);
    }

    ApiExchange status(int code) {
        status = code;
        return this;
    }

    ApiExchange contentType(String value) {
        contentType = MediaType.parseMediaType(value);
        return this;
    }

    ApiExchange result(String value) {
        result = value;
        return this;
    }

    private ServerResponse response() {
        var response = ServerResponse.status(status);
        if (result == null || status == 204) return response.build();
        return response.contentType(contentType).body(result);
    }
}
