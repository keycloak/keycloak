/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.theme;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ThemeManager;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultThemeManagerFactory {

    private static final Logger log = Logger.getLogger(DefaultThemeManagerFactory.class);

    private ConcurrentHashMap<ThemeKey, Theme> themeCache;

    public DefaultThemeManagerFactory() {
        if(Config.scope("theme").getBoolean("cacheThemes", true)) {
            themeCache = new ConcurrentHashMap<>();
        }
    }

    public ThemeManager create(KeycloakSession session) {
        return new DefaultThemeManager(this, session);
    }

    public Theme getCachedTheme(String name, Theme.Type type) {
        if (themeCache != null) {
            DefaultThemeManagerFactory.ThemeKey key = DefaultThemeManagerFactory.ThemeKey.get(name, type);
            return themeCache.get(key);
        } else {
            return null;
        }
    }

    public Theme addCachedTheme(String name, Theme.Type type, Theme theme) {
        if (theme == null) {
            return null;
        }

        if (themeCache == null) {
            return theme;
        }

        DefaultThemeManagerFactory.ThemeKey key = DefaultThemeManagerFactory.ThemeKey.get(name, type);
        if (themeCache.putIfAbsent(key, theme) != null) {
            theme = themeCache.get(key);
        }

        return theme;
    }

    public boolean isCacheEnabled() {
        return themeCache != null;
    }

    public void clearCache() {
        if (themeCache != null) {
            themeCache.clear();
            log.info("Cleared theme cache");
        }
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
