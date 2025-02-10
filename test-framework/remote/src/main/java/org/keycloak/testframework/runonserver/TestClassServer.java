package org.keycloak.testframework.runonserver;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

public class TestClassServer {

    public static final String CONTEXT_PATH = "/test-classes/";
    private final HttpServer server;
    private final TestClassPathHandler handler;

    public static final Set<String> DEFAULT_PERMITTED_PACKAGES = Set.of(
        "org.keycloak.testframework",
        "org.junit",
        "org.opentest4j"
    );

    private final Set<String> permittedPackages;

    TestClassServer() {
        permittedPackages = new HashSet<>(DEFAULT_PERMITTED_PACKAGES);

        this.handler = new TestClassPathHandler();

        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 8500), 0);
            server.createContext(CONTEXT_PATH, handler);
            server.start();
        } catch (IOException exp) {
            throw new RuntimeException(exp);
        }
    }

    public void addPermittedPackages(Set<String> permittedPackages) {
        this.permittedPackages.addAll(permittedPackages);
    }

    public void close() {
        server.stop(0);
    }

    private class TestClassPathHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String resource = httpExchange.getRequestURI().getPath().substring(CONTEXT_PATH.length() - 1);

            Headers respHeaders = httpExchange.getResponseHeaders();
            respHeaders.set("Content-Type", "application/x-java-applet;charset=utf-8");

            if (!isPermittedPackage(resource) || !resource.endsWith(".class")) {
                httpExchange.sendResponseHeaders(403, 0);
            } else {
                try (InputStream resourceStream = TestClassServer.class.getResourceAsStream(resource)) {
                    if (resourceStream != null) {
                        byte[] bytes = resourceStream.readAllBytes();
                        httpExchange.sendResponseHeaders(200, bytes.length);
                        httpExchange.getResponseBody().write(bytes);
                    } else {
                        httpExchange.sendResponseHeaders(404, 0);
                    }
                }
            }
            httpExchange.close();
        }

        private boolean isPermittedPackage(String className) {
            String c = className.substring(1).replace('/', '.');
            for (String p : permittedPackages) {
                if (c.startsWith(p)) {
                    return true;
                }
            }
            return false;
        }
    }

}
