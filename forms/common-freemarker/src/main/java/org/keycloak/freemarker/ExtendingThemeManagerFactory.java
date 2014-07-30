package org.keycloak.freemarker;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ExtendingThemeManagerFactory implements ThemeProviderFactory {

    private ConcurrentHashMap<ThemeKey, Theme> themeCache = new ConcurrentHashMap<ThemeKey, Theme>();

    private ExtendingThemeManager themeManager;

    @Override
    public ThemeProvider create(KeycloakSession session) {
        return new ExtendingThemeManager(session, themeCache);
    }

    @Override
    public void init(Config.Scope config) {
        if(Config.scope("theme").getBoolean("cacheThemes", true)) {
            themeCache = new ConcurrentHashMap<ThemeKey, Theme>();
        }
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "extending";
    }

    public static class ThemeKey {

        private String name;
        private Theme.Type type;

        public static ThemeKey get(String name, Theme.Type type) {
            return new ThemeKey(name, type);
        }

        private ThemeKey(String name, Theme.Type type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Theme.Type getType() {
            return type;
        }

        public void setType(Theme.Type type) {
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ThemeKey themeKey = (ThemeKey) o;

            if (name != null ? !name.equals(themeKey.name) : themeKey.name != null) return false;
            if (type != themeKey.type) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (type != null ? type.hashCode() : 0);
            return result;
        }

    }

}
