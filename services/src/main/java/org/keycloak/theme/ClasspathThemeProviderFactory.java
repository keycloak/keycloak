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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClasspathThemeProviderFactory implements ThemeProviderFactory {

    public static final String KEYCLOAK_THEMES_JSON = "META-INF/keycloak-themes.json";
    protected static Map<Theme.Type, Map<String, ClassLoaderTheme>> themes = new HashMap<>();

    private String id;

    public ClasspathThemeProviderFactory(String id) {
        this.id = id;
    }

    public ClasspathThemeProviderFactory(String id, ClassLoader classLoader) {
        this.id = id;
        loadThemes(classLoader, classLoader.getResourceAsStream(KEYCLOAK_THEMES_JSON));
    }

    public static class ThemeRepresentation {
        private String name;
        private String[] types;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String[] getTypes() {
            return types;
        }

        public void setTypes(String[] types) {
            this.types = types;
        }
    }

    public static class ThemesRepresentation {
        private ThemeRepresentation[] themes;

        public ThemeRepresentation[] getThemes() {
            return themes;
        }

        public void setThemes(ThemeRepresentation[] themes) {
            this.themes = themes;
        }
    }

    @Override
    public ThemeProvider create(KeycloakSession session) {
        return new ClasspathThemeProvider(themes);
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

    @Override
    public String getId() {
        return id;
    }

    protected void loadThemes(ClassLoader classLoader, InputStream themesInputStream) {
        try {
            loadThemes(classLoader, JsonSerialization.readValue(themesInputStream, ThemesRepresentation.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load themes", e);
        }
    }

    protected void loadThemes(ClassLoader classLoader, ThemesRepresentation themesRep) {
        try {
            for (ThemeRepresentation themeRep : themesRep.getThemes()) {
                for (String t : themeRep.getTypes()) {
                    Theme.Type type = Theme.Type.valueOf(t.toUpperCase());
                    if (!themes.containsKey(type)) {
                        themes.put(type, new HashMap<>());
                    }
                    themes.get(type).put(themeRep.getName(), new ClassLoaderTheme(themeRep.getName(), type, classLoader));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load themes", e);
        }
    }

}
