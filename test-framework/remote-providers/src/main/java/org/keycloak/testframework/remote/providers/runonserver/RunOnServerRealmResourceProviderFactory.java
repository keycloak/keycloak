package org.keycloak.testframework.remote.providers.runonserver;

import java.net.MalformedURLException;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class RunOnServerRealmResourceProviderFactory implements RealmResourceProviderFactory {

    private static final String ID = "testing-run-on-server";

    private String testClassUrl = TestClassLoader.DEFAULT_TEST_CLASS_URL;
    private String executionId;
    private ClassLoader testClassLoader;

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new RunOnServerRealmResourceProvider(session, this);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }

    public ClassLoader getTestClassLoader(String executionId) {
        if (!executionId.equals(this.executionId)) {
            try {
                testClassLoader = new TestClassLoader(testClassUrl);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            this.executionId = executionId;
        }
        return testClassLoader;
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {
        // Servers running in a container cannot reach the test class server on localhost
        testClassUrl = config.get("test-class-url", TestClassLoader.DEFAULT_TEST_CLASS_URL);
        try {
            testClassLoader = new TestClassLoader(testClassUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}
