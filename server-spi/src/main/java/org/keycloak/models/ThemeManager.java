package org.keycloak.models;

import org.keycloak.theme.Theme;

import java.io.IOException;
import java.util.Set;

public interface ThemeManager {

    /**
     * Returns the theme for the specified type. The theme is determined by the theme selector.
     *
     * @param type
     * @return
     * @throws IOException
     */
    Theme getTheme(Theme.Type type) throws IOException;

    /**
     * Returns the specified theme for the specified type.
     *
     * @param name
     * @param type
     * @return
     * @throws IOException
     */
    Theme getTheme(String name, Theme.Type type) throws IOException;

    /**
     * Returns a set of all theme names for the specified type.
     *
     * @param type
     * @return
     */
    Set<String> nameSet(Theme.Type type);

    void clearCache();

}
