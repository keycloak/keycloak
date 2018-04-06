package org.keycloak.theme;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class ClasspathThemeResourceProviderFactory implements ThemeResourceProviderFactory, ThemeResourceProvider {

    public static final String THEME_RESOURCES_TEMPLATES = "theme-resources/templates/";
    public static final String THEME_RESOURCES_RESOURCES = "theme-resources/resources/";
    private final String id;
    private final ClassLoader classLoader;

    public ClasspathThemeResourceProviderFactory(String id, ClassLoader classLoader) {
        this.id = id;
        this.classLoader = classLoader;
    }

    @Override
    public ThemeResourceProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public URL getTemplate(String name) throws IOException {
        return classLoader.getResource(THEME_RESOURCES_TEMPLATES + name);
    }

    @Override
    public InputStream getResourceAsStream(String path) throws IOException {
        return classLoader.getResourceAsStream(THEME_RESOURCES_RESOURCES + path);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

}
