package org.keycloak.testsuite.rest;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class TestClassLoader extends URLClassLoader {

    private static TestClassLoader instance;

    public static TestClassLoader getInstance() {
        if (instance == null) {
            ClassLoader parent = TestClassLoader.class.getClassLoader();
            try {
                instance = new TestClassLoader(parent);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        return instance;
    }

    private TestClassLoader(ClassLoader parent) throws MalformedURLException {
        super(new URL[] { new URL("http://localhost:8500/") }, parent);
    }

}
