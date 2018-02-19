package org.keycloak.testsuite.theme;

import org.keycloak.theme.ClasspathThemeResourceProviderFactory;

public class TestThemeResourceProvider extends ClasspathThemeResourceProviderFactory {

    public TestThemeResourceProvider() {
        super("test-resources", TestThemeResourceProvider.class.getClassLoader());
    }

}
