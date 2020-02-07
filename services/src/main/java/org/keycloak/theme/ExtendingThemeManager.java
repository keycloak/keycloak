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

import org.apache.commons.lang.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.common.Version;
import org.keycloak.common.util.StringPropertyReplacer;
import org.keycloak.common.util.SystemEnvProperties;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ExtendingThemeManager implements ThemeProvider {

    private static final Logger log = Logger.getLogger(ExtendingThemeManager.class);

    private final KeycloakSession session;
    private final ConcurrentHashMap<ExtendingThemeManagerFactory.ThemeKey, Theme> themeCache;
    private List<ThemeProvider> providers;
    private String defaultTheme;

    public ExtendingThemeManager(KeycloakSession session, ConcurrentHashMap<ExtendingThemeManagerFactory.ThemeKey, Theme> themeCache) {
        this.session = session;
        this.themeCache = themeCache;
        this.defaultTheme = Config.scope("theme").get("default", Version.NAME.toLowerCase());
    }

    private List<ThemeProvider> getProviders() {
        if (providers == null) {
            providers = new LinkedList();

            for (ThemeProvider p : session.getAllProviders(ThemeProvider.class)) {
                if (!(p instanceof ExtendingThemeManager)) {
                    if (!p.getClass().equals(ExtendingThemeManager.class)) {
                        providers.add(p);
                    }
                }
            }

            Collections.sort(providers, new Comparator<ThemeProvider>() {
                @Override
                public int compare(ThemeProvider o1, ThemeProvider o2) {
                    return o2.getProviderPriority() - o1.getProviderPriority();
                }
            });
        }

        return providers;
    }

    @Override
    public int getProviderPriority() {
        return 0;
    }

    @Override
    public Theme getTheme(String name, Theme.Type type) throws IOException {
        if (name == null) {
            name = defaultTheme;
        }

        if (themeCache != null) {
            ExtendingThemeManagerFactory.ThemeKey key = ExtendingThemeManagerFactory.ThemeKey.get(name, type);
            Theme theme = themeCache.get(key);
            if (theme == null) {
                theme = loadTheme(name, type);
                if (theme == null) {
                    theme = loadTheme("keycloak", type);
                    if (theme == null) {
                        theme = loadTheme("base", type);
                    }
                    log.errorv("Failed to find {0} theme {1}, using built-in themes", type, name);
                } else if (themeCache.putIfAbsent(key, theme) != null) {
                    theme = themeCache.get(key);
                }
            }
            return theme;
        } else {
            return loadTheme(name, type);
        }
    }

    private Theme loadTheme(String name, Theme.Type type) throws IOException {
        Theme theme = findTheme(name, type);
        List<Theme> themes = new LinkedList<>();
        themes.add(theme);

        if (theme.getImportName() != null) {
            String[] s = theme.getImportName().split("/");
            themes.add(findTheme(s[1], Theme.Type.valueOf(s[0].toUpperCase())));
        }

        if (theme.getParentName() != null) {
            for (String parentName = theme.getParentName(); parentName != null; parentName = theme.getParentName()) {
                theme = findTheme(parentName, type);
                themes.add(theme);

                if (theme.getImportName() != null) {
                    String[] s = theme.getImportName().split("/");
                    themes.add(findTheme(s[1], Theme.Type.valueOf(s[0].toUpperCase())));
                }
            }
        }

        return new ExtendingTheme(themes, session.getAllProviders(ThemeResourceProvider.class));
    }

    @Override
    public Set<String> nameSet(Theme.Type type) {
        Set<String> themes = new HashSet<String>();
        for (ThemeProvider p : getProviders()) {
            themes.addAll(p.nameSet(type));
        }
        return themes;
    }

    @Override
    public boolean hasTheme(String name, Theme.Type type) {
        for (ThemeProvider p : getProviders()) {
            if (p.hasTheme(name, type)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void close() {
        providers = null;
    }

    private Theme findTheme(String name, Theme.Type type) {
        for (ThemeProvider p : getProviders()) {
            if (p.hasTheme(name, type)) {
                try {
                    return p.getTheme(name, type);
                } catch (IOException e) {
                    log.errorv(e, p.getClass() + " failed to load theme, type={0}, name={1}", type, name);
                }
            }
        }
        return null;
    }

    public static class ExtendingTheme implements Theme {

        private List<Theme> themes;
        private Set<ThemeResourceProvider> themeResourceProviders;

        private Properties properties;

        private ConcurrentHashMap<String, ConcurrentHashMap<Locale, Properties>> messages = new ConcurrentHashMap<>();

        public ExtendingTheme(List<Theme> themes, Set<ThemeResourceProvider> themeResourceProviders) {
            this.themes = themes;
            this.themeResourceProviders = themeResourceProviders;
        }

        @Override
        public String getName() {
            return themes.get(0).getName();
        }

        @Override
        public String getParentName() {
            return themes.get(0).getParentName();
        }

        @Override
        public String getImportName() {
            return themes.get(0).getImportName();
        }

        @Override
        public Type getType() {
            return themes.get(0).getType();
        }

        @Override
        public URL getTemplate(String name) throws IOException {
            for (Theme t : themes) {
                URL template = t.getTemplate(name);
                if (template != null) {
                    return template;
                }
            }

            for (ThemeResourceProvider t : themeResourceProviders) {
                URL template = t.getTemplate(name);
                if (template != null) {
                    return template;
                }
            }

            return null;
        }

        @Override
        public InputStream getResourceAsStream(String path) throws IOException {
            for (Theme t : themes) {
                InputStream resource = t.getResourceAsStream(path);
                if (resource != null) {
                    return resource;
                }
            }

            for (ThemeResourceProvider t : themeResourceProviders) {
                InputStream resource = t.getResourceAsStream(path);
                if (resource != null) {
                    return resource;
                }
            }

            return null;
        }

        @Override
        public Properties getMessages(Locale locale) throws IOException {
            return getMessages("messages", locale);
        }

        @Override
        public Properties getMessages(String baseBundlename, Locale locale) throws IOException {

            Properties messages = getCachedMessages(baseBundlename, locale);
            if (messages != null) {
                return messages;
            }

            messages = new Properties();

            // Add english locale as fallback
            List<Locale> locales = expandLocalesFromUnspecificToSpecificPrefixedWithFallback(locale, Locale.ENGLISH);

            /*
             * Populate the messages Properties with the translations for the expanded locales,
             * starting with fallback Locale from the least specific Locale to the most specific locale.
             */
            for (Locale currentLocale : locales) {
                for (ThemeResourceProvider t : themeResourceProviders) {
                    messages.putAll(t.getMessages(baseBundlename, currentLocale));
                }

                ListIterator<Theme> itr = themes.listIterator(themes.size());
                while (itr.hasPrevious()) {
                    Properties m = itr.previous().getMessages(baseBundlename, currentLocale);
                    if (m != null) {
                        messages.putAll(m);
                    }
                }
            }

            this.messages.computeIfAbsent(baseBundlename, ignored -> new ConcurrentHashMap<>()).putIfAbsent(locale, messages);

            return messages;
        }

        protected Properties getCachedMessages(String baseBundlename, Locale locale) {

            ConcurrentHashMap<Locale, Properties> map = messages.get(baseBundlename);
            if (map == null) {
                return null;
            }

            return map.get(locale);
        }

        /**
         * Returns a list of derived locales for a given locale by creating dedicated {@link Locale Locale's} for the language, country and variant components.
         * If a fallback {@link Locale} is provided, then it is prefixed to the {@link Locale Locale's} list.
         * If no fallback {@link Locale} is provided. {@link Locale#ENGLISH} is used as a fallback.
         * An example {@link Locale Locale's} list could look like: {@code Locale(fallback-language), Locale(Language), Locale(Language, Country), Locale(Language, Country, Variant)}
         * @param locale the reference {@link Locale}
         * @param fallback the fallback {@link Locale}
         * @return The expanded {@link Locale} {@link List}.
         */
        private List<Locale> expandLocalesFromUnspecificToSpecificPrefixedWithFallback(Locale locale, Locale fallback) {

            Locale fallbackLocale = fallback;
            if (fallbackLocale == null) {
                fallbackLocale = Locale.ENGLISH;
            }

            if (locale == null || fallbackLocale.equals(locale)) {
                return Collections.singletonList(fallbackLocale);
            }

            List<Locale> locales = new ArrayList<>();

            locales.add(fallback);

            String language = locale.getLanguage();
            locales.add(new Locale(language));

            String country = locale.getCountry();
            if (!StringUtils.isEmpty(country)) {
                locales.add(new Locale(language, country));
            }

            String variant = locale.getVariant();
            if (!StringUtils.isEmpty(variant)) {
                locales.add(new Locale(language, country, variant));
            }

            return locales;
        }

        @Override
        public Properties getProperties() throws IOException {
            if (properties == null) {
                Properties properties = new Properties();
                ListIterator<Theme> itr = themes.listIterator(themes.size());
                while (itr.hasPrevious()) {
                    Properties p = itr.previous().getProperties();
                    if (p != null) {
                        properties.putAll(p);
                    }
                }
                substituteProperties(properties);
                this.properties = properties;
                return properties;
            } else {
                return properties;
            }
        }

        /**
         * Iterate over all string properties defined in "theme.properties" then substitute the value with system property or environment variables.
         * See {@link StringPropertyReplacer#replaceProperties} for details about the different formats.
         */
        private void substituteProperties(final Properties properties) {
            for (final String propertyName : properties.stringPropertyNames()) {
                properties.setProperty(propertyName, StringPropertyReplacer.replaceProperties(properties.getProperty(propertyName), new SystemEnvProperties()));
            }
        }
    }
}
