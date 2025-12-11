package org.keycloak.testframework.remote.providers.runonserver;

import java.net.MalformedURLException;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class RunOnServerRealmResourceProviderFactory implements RealmResourceProviderFactory {

    private static final String ID = "testing-run-on-server";

    private ClassLoader testClassLoader;

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new RunOnServerRealmResourceProvider(session, testClassLoader);
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

    @Override
    public void init(org.keycloak.Config.Scope config) {
        try {
            testClassLoader = new TestClassLoader();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

}
