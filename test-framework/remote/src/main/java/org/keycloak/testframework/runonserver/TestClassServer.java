package org.keycloak.testframework.runonserver;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class TestClassServer {

    private final HttpServer server;
    private final TestClassPathHandler handler;
    public static final String[] PERMITTED_PACKAGES = new String[] {
            "/org/keycloak/testframework",
            "/org/keycloak/tests",
            "/org/junit/jupiter",
            "/org/hamcrest",
            "/org/keycloak/admin/client",
            "/org/openqa/selenium",
            "/com/webauthn4j",
            "/com/fasterxml/jackson/dataformat/cbor",
            "/org/slf4j",
            "/org/apache"
    };

    TestClassServer() {
        this.handler = new TestClassPathHandler();

        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 8500), 0);
            server.createContext("/test-classes", handler);
            server.start();
        } catch (IOException exp) {
            throw new RuntimeException(exp);
        }
    }

    public void close() {
        server.stop(0);
    }

    private static class TestClassPathHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String request = new String(httpExchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String classAsPath = request.replace(".", "/") + ".class";

            Headers respHeaders = httpExchange.getResponseHeaders();
            respHeaders.set("Content-Type", "text/plain;charset=utf-8");
            byte[] response = isPermittedPackage(classAsPath) ?
                    TestClassServer.class.getClassLoader().getResource(classAsPath).getPath().getBytes(StandardCharsets.UTF_8) :
                    null;

            httpExchange.sendResponseHeaders(200, Objects.requireNonNull(response).length);
            httpExchange.getResponseBody().write(response);
            httpExchange.close();
        }
    }

    private static boolean isPermittedPackage(String className) {
        for (String p : PERMITTED_PACKAGES) {
            if (className.startsWith(p)) {
                return true;
            }
        }
        return false;
    }
}
