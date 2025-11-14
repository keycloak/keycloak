package org.keycloak.quarkus.runtime.themes;

import java.io.File;
import java.util.Optional;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.theme.FolderThemeProvider;
import org.keycloak.theme.ThemeProvider;
import org.keycloak.theme.ThemeProviderFactory;

public class QuarkusFolderThemeProviderFactory implements ThemeProviderFactory {

    private static final String CONFIG_DIR_KEY = "dir";
    private FolderThemeProvider themeProvider;

    @Override
    public ThemeProvider create(KeycloakSession sessions) {
        return themeProvider;
    }

    @Override
    public void init(Config.Scope config) {
        String configDir = config.get(CONFIG_DIR_KEY);
        File rootDir = getThemeRootDirWithFallback(configDir);
        themeProvider = new FolderThemeProvider(rootDir);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "folder";
    }

    /**
     * Determines if the theme root directory we get
     * from {@link Config} exists.
     * If not, uses the default theme directory as a fallback.
     *
     * @param rootDirFromConfig string value from {@link Config}
     * @return Directory to use as theme root directory in {@link File} format, either from config or from default. Null if none is available.
     * @throws RuntimeException when filesystem path is not accessible
     */
    private File getThemeRootDirWithFallback(String rootDirFromConfig) {
        return Optional.ofNullable(rootDirFromConfig).or(Environment::getDefaultThemeRootDir).map(File::new)
                .filter(File::exists).orElse(null);
    }
}
