package com.instakill.notification.infrastructure;

import com.instakill.notification.application.NotificationProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class NtfyNotificationSenderTest {

    private record Received(String method, String path, String title, String body) {}

    private HttpServer server;
    private final BlockingQueue<Received> requests = new LinkedBlockingQueue<>();
    private int status = 200;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requests.add(new Received(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("Title"), body));
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private NtfyNotificationSender sender(boolean enabled) {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
        return new NtfyNotificationSender(new NotificationProperties(enabled, baseUrl, "my-topic", 2000));
    }

    @Test
    void postsMessageAsPlainTextToTheTopic() throws InterruptedException {
        sender(true).send("New Instakill post", "User: alice");

        Received received = requests.poll(2, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.method()).isEqualTo("POST");
        assertThat(received.path()).isEqualTo("/my-topic");
        assertThat(received.title()).isEqualTo("New Instakill post");
        assertThat(received.body()).isEqualTo("User: alice");
    }

    @Test
    void doesNothingWhenDisabled() throws InterruptedException {
        sender(false).send("t", "m");

        assertThat(requests.poll(300, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void serverErrorsDoNotPropagate() {
        status = 500;

        assertThatCode(() -> sender(true).send("t", "m")).doesNotThrowAnyException();
    }

    @Test
    void unreachableServerDoesNotPropagate() {
        server.stop(0);

        assertThatCode(() -> sender(true).send("t", "m")).doesNotThrowAnyException();
    }
}
