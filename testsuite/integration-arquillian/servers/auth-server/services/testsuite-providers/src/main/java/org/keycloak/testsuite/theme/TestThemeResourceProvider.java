package org.keycloak.testsuite.theme;

import org.keycloak.Config;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.theme.ClasspathThemeResourceProviderFactory;

import io.quarkus.runtime.Application;

public class TestThemeResourceProvider extends ClasspathThemeResourceProviderFactory implements EnvironmentDependentProviderFactory {

    public TestThemeResourceProvider() {
        super("test-resources", TestThemeResourceProvider.class.getClassLoader());
    }

    /**
     * Quarkus detects theme resources automatically, so this provider should only be enabled on Undertow
     *
     * @return true if platform is Undertow
     */
    @Override
    public boolean isSupported(Config.Scope config) {
        return Application.currentApplication() == null;
    }
}
