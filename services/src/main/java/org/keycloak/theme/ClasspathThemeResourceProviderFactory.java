package org.keycloak.theme;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.Properties;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class ClasspathThemeResourceProviderFactory implements ThemeResourceProviderFactory, ThemeResourceProvider {

    public static final String THEME_RESOURCES = "theme-resources";
    public static final String THEME_RESOURCES_TEMPLATES = THEME_RESOURCES + "/templates/";
    public static final String THEME_RESOURCES_RESOURCES = THEME_RESOURCES + "/resources/";
    public static final String THEME_RESOURCES_MESSAGES = THEME_RESOURCES + "/messages/";

    private final String id;
    protected final ClassLoader classLoader;

    public ClasspathThemeResourceProviderFactory() {
        this("classpath", Thread.currentThread().getContextClassLoader());
    }

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
        return ResourceLoader.getResourceAsStream(THEME_RESOURCES_RESOURCES, path);
    }

    @Override
    public Properties getMessages(String baseBundlename, Locale locale) throws IOException {
        Properties messages = new Properties();
        URL resource = classLoader.getResource(THEME_RESOURCES_MESSAGES + baseBundlename + "_" + locale.toString() + ".properties");
        loadMessages(messages, resource);
        return messages;
    }

    protected void loadMessages(Properties messages, URL resource) throws IOException {
        if (resource != null) {
            try (InputStream stream = resource.openStream()) {
                PropertiesUtil.readCharsetAware(messages, stream);
            }
        }
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
