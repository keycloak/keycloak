package org.keycloak.theme;

import org.keycloak.Config;
import org.keycloak.freemarker.ThemeProvider;
import org.keycloak.freemarker.ThemeProviderFactory;
import org.keycloak.models.KeycloakSession;

import java.io.File;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FolderThemeProviderFactory implements ThemeProviderFactory {

    private FolderThemeProvider themeProvider;

    @Override
    public ThemeProvider create(KeycloakSession sessions) {
        return themeProvider;
    }

    @Override
    public void init(Config.Scope config) {
        String d = config.get("dir");
        File rootDir = null;
        if (d != null) {
            rootDir = new File(d);
        }
        themeProvider = new FolderThemeProvider(rootDir);
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "folder";
    }
}
