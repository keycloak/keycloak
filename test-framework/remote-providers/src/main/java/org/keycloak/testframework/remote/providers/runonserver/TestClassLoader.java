package org.keycloak.testframework.remote.providers.runonserver;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class TestClassLoader extends URLClassLoader {

    private static TestClassLoader instance;

    public static TestClassLoader getInstance() {
        if (instance == null) {
            instance = new TestClassLoader(TestClassLoader.class.getClassLoader());
        }
        return instance;
    }

    public TestClassLoader(ClassLoader parent) {
        super(createUrls(), parent);
    }

    private static URL[] createUrls() {
        try {
            return new URL[] { new URL("http://localhost:8500/test-classes/") };
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}
