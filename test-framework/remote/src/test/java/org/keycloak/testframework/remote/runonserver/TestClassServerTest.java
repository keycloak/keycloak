package org.keycloak.testframework.remote.runonserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

import org.keycloak.representations.idm.RealmRepresentation;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestClassServerTest {

    private static HttpServer SERVER;

    @BeforeAll
    public static void setupServer() throws IOException {
        SERVER = HttpServer.create(new InetSocketAddress("127.0.0.1", 8500), 10);
        SERVER.start();
    }

    @AfterAll
    public static void closeServer() {
        SERVER.stop(0);
    }

    @Test
    public void testPermittedPackage() throws ClassNotFoundException, MalformedURLException {
        TestClassServer server = new TestClassServer(SERVER);
        server.addPermittedPackages(Set.of("org.keycloak.representations.idm"));

        URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { new URL("http://localhost:8500/test-classes/") }, null);
        Assertions.assertNotNull(urlClassLoader.loadClass(RealmRepresentation.class.getName()));

        server.close();
    }

    @Test
    public void testInvalidPackage() throws MalformedURLException {
        TestClassServer server = new TestClassServer(SERVER);

        URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { new URL("http://localhost:8500/test-classes/") }, null);
        Assertions.assertThrows(ClassNotFoundException.class, () -> urlClassLoader.loadClass(RealmRepresentation.class.getName()));

        server.close();
    }

}
