package com.DMHelper.api;

import org.springframework.context.ConfigurableApplicationContext;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;

final class ApiTestServer implements AutoCloseable {
    private final ConfigurableApplicationContext application = ApiServer.start(0);

    HttpRequest.Builder request(String path) {
        int port = application.getEnvironment().getRequiredProperty("local.server.port", Integer.class);
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(15))
                .header(DesktopClientFilter.TOKEN_HEADER, ApiServer.getClientToken());
    }

    @Override
    public void close() {
        application.close();
    }
}
