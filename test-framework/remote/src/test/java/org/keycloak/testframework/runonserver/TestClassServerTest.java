package org.keycloak.testframework.runonserver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RealmRepresentation;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

public class TestClassServerTest {

    @Test
    public void testPermittedPackage() throws ClassNotFoundException, MalformedURLException {
        TestClassServer server = new TestClassServer();
        server.addPermittedPackages(Set.of("org.keycloak.representations.idm"));

        URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { new URL("http://localhost:8500/test-classes/") }, null);
        Assertions.assertNotNull(urlClassLoader.loadClass(RealmRepresentation.class.getName()));

        server.close();
    }

    @Test
    public void testInvalidPackage() throws MalformedURLException {
        TestClassServer server = new TestClassServer();

        URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { new URL("http://localhost:8500/test-classes/") }, null);
        Assertions.assertThrows(ClassNotFoundException.class, () -> urlClassLoader.loadClass(RealmRepresentation.class.getName()));

        server.close();
    }

}
