package org.keycloak.testframework.runonserver;

import org.junit.jupiter.api.Test;
import org.keycloak.testframework.remote.providers.runonserver.TestClassLoader;

public class TestClassServerTest {

    @Test
    public void test() throws ClassNotFoundException {
        TestClassServer server = new TestClassServer();

        TestClassLoader testClassLoader = new TestClassLoader(null);
        testClassLoader.loadClass("org.keycloak.testframework.runonserver.TestClassServerTest");

        server.close();
    }

}
