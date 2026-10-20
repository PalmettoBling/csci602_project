package edu.citadel.weather;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/** Local HTTP fixture; tests never call the live provider. */
public class WeatherStub implements AutoCloseable {
    private final HttpServer server;
    public final AtomicInteger calls = new AtomicInteger();
    public volatile int status = 200;
    public volatile String query;
    public volatile String body = """
            {"hourly":{"time":["2026-10-01T18:00"],
            "temperature_2m":[24.5],"precipitation":[0.3],
            "weather_code":[51],"wind_speed_10m":[12.0]}}
            """;

    public WeatherStub() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        server.createContext("/v1/archive", exchange -> {
            calls.incrementAndGet();
            query = exchange.getRequestURI().getRawQuery();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        });
        server.start();
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
