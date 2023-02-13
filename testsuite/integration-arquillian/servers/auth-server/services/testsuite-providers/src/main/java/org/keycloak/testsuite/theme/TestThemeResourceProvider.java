package org.keycloak.testsuite.theme;

import org.keycloak.platform.Platform;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.theme.ClasspathThemeResourceProviderFactory;

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
    public boolean isSupported() {
        return Platform.getPlatform().name().equals("Undertow");
    }
}
