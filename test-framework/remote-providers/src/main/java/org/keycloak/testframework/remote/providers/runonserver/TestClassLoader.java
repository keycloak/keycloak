package org.keycloak.testframework.remote.providers.runonserver;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

public class TestClassLoader extends URLClassLoader {

    private static TestClassLoader instance;

    public static TestClassLoader getInstance() {
        if (instance == null) {
            ClassLoader parent = TestClassLoader.class.getClassLoader();
            try {
                instance = new TestClassLoader(parent);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return instance;
    }

    private TestClassLoader(ClassLoader parent) throws IOException {
        super(new URL[] { new URL("http://localhost:8500/test-classes") }, parent);
    }
}
