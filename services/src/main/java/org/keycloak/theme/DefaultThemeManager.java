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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.keycloak.common.util.StringPropertyReplacer;
import org.keycloak.common.util.SystemEnvProperties;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.ThemeManager;
import org.keycloak.services.util.LocaleUtil;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultThemeManager implements ThemeManager {

    private static final Logger log = Logger.getLogger(DefaultThemeManager.class);

    private final DefaultThemeManagerFactory factory;
    private final KeycloakSession session;
    private List<ThemeProvider> providers;

    public DefaultThemeManager(DefaultThemeManagerFactory factory, KeycloakSession session) {
        this.factory = factory;
        this.session = session;
    }

    @Override
    public Theme getTheme(Theme.Type type) {
        String name = session.getProvider(ThemeSelectorProvider.class).getThemeName(type);
        return getTheme(name, type);
    }

    @Override
    public Theme getTheme(String name, Theme.Type type) {
        Theme theme = factory.getCachedTheme(name, type);
        if (theme == null) {
            theme = loadTheme(name, type);
            if (theme == null) {
                String defaultThemeName = session.getProvider(ThemeSelectorProvider.class).getDefaultThemeName(type);
                theme = loadTheme(defaultThemeName, type);
                log.errorv("Failed to find {0} theme {1}, using built-in themes", type, name);
            } else {
                theme = factory.addCachedTheme(name, type, theme);
            }
        }

        return theme;
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
    public boolean isCacheEnabled() {
        return factory.isCacheEnabled();
    }

    @Override
    public void clearCache() {
        factory.clearCache();
    }

    @Override
    public void close() {
    }

    private Theme loadTheme(String name, Theme.Type type) {
        Theme theme = findTheme(name, type);
        if (theme == null) {
            return null;
        }

        List<Theme> themes = new LinkedList<>();
        themes.add(theme);

        if (!processImportedTheme(themes, theme, name, type)) return null;

        if (theme.getParentName() != null) {
            for (String parentName = theme.getParentName(); parentName != null; parentName = theme.getParentName()) {
                String currentThemeName = theme.getName();
                theme = findTheme(parentName, type);
                if (theme == null) {
                    log.warnf("Not found parent theme '%s' of theme '%s'. Unable to load %s theme '%s' due to this.", parentName, currentThemeName, type, name);
                    return null;
                }
                themes.add(theme);

                if (!processImportedTheme(themes, theme, name, type)) return null;
            }
        }

        return new ExtendingTheme(themes, session.getAllProviders(ThemeResourceProvider.class));
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

    private boolean processImportedTheme(List<Theme> themes, Theme theme, String origThemeName, Theme.Type type) {
        if (theme.getImportName() != null) {
            String[] s = theme.getImportName().split("/");
            Theme importedTheme = findTheme(s[1], Theme.Type.valueOf(s[0].toUpperCase()));
            if (importedTheme == null) {
                log.warnf("Not found theme '%s' referenced as import of theme '%s'. Unable to load %s theme '%s' due to this.", theme.getImportName(), theme.getName(), type, origThemeName);
                return false;
            }
            themes.add(importedTheme);
        }
        return true;
    }

    private static class ExtendingTheme implements Theme {

        private final List<Theme> themes;
        private final Set<ThemeResourceProvider> themeResourceProviders;

        private Properties properties;

        private final ConcurrentHashMap<String, ConcurrentHashMap<Locale, Map<Locale, Properties>>> messages =
                new ConcurrentHashMap<>();

        private Pattern compiledContentHashPattern;

        public ExtendingTheme(List<Theme> themes, Set<ThemeResourceProvider> themeResourceProviders) {
            this.themes = themes;
            this.themeResourceProviders = themeResourceProviders;
            try {
                Object contentHashPattern = getProperties().get(CONTENT_HASH_PATTERN);
                if (contentHashPattern != null) {
                    compiledContentHashPattern = Pattern.compile(contentHashPattern.toString());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
            Map<Locale, Properties> messagesByLocale = getMessagesByLocale(baseBundlename, locale);
            return LocaleUtil.mergeGroupedMessages(locale, messagesByLocale);
        }
        
        @Override
        public Properties getEnhancedMessages(RealmModel realm, Locale locale) throws IOException {
            Map<Locale, Properties> messagesByLocale = getMessagesByLocale("messages", locale);
            return LocaleUtil.enhancePropertiesWithRealmLocalizationTexts(realm, locale, messagesByLocale);
        }

        @Override
        public boolean hasContentHash(String path) throws IOException {
            return compiledContentHashPattern != null && compiledContentHashPattern.matcher(path).matches();
        }

        private Map<Locale, Properties> getMessagesByLocale(String baseBundlename, Locale locale) throws IOException {
            if (messages.get(baseBundlename) == null || messages.get(baseBundlename).get(locale) == null) {
                Locale parent = getParent(locale);

                Map<Locale, Properties> parentMessages =
                        parent == null ? Collections.emptyMap() : getMessagesByLocale(baseBundlename, parent);

                Properties currentMessages = new Properties();
                Map<Locale, Properties> groupedMessages = new HashMap<>(parentMessages);
                groupedMessages.put(locale, currentMessages);

                for (ThemeResourceProvider t : themeResourceProviders) {
                    currentMessages.putAll(t.getMessages(baseBundlename, locale));
                }

                ListIterator<Theme> itr = themes.listIterator(themes.size());
                while (itr.hasPrevious()) {
                    Properties m = itr.previous().getMessages(baseBundlename, locale);
                    if (m != null) {
                        currentMessages.putAll(m);
                    }
                }

                addlocaleTranslations(locale, currentMessages);

                this.messages.putIfAbsent(baseBundlename, new ConcurrentHashMap<>());
                this.messages.get(baseBundlename).putIfAbsent(locale, groupedMessages);

                return groupedMessages;
            } else {
                return messages.get(baseBundlename).get(locale);
            }
        }

        protected void addlocaleTranslations(Locale locale, Properties m) throws IOException {
            for (String l : getProperties().getProperty("locales", "").split(",")) {
                l = l.trim();
                String key = "locale_" + l;
                String label = m.getProperty(key);
                if (label != null) {
                    continue;
                }
                String rl = l;
                // This is mapping old locale codes to the new locale codes for Simplified and Traditional Chinese.
                // Once the existing locales have been moved, this code can be removed.
                if (l.equals("zh-CN")) {
                    rl = "zh-Hans";
                } else if (l.equals("zh-TW")) {
                    rl = "zh-Hant";
                }
                Locale loc = Locale.forLanguageTag(rl);
                label = capitalize(loc.getDisplayName(locale), locale);
                if (!Objects.equals(loc, locale)) {
                    label += " (" + capitalize(loc.getDisplayName(loc), loc) + ")";
                }
                m.put(key, label);
            }
        }

        private static final Pattern LATIN_CHARACTERS;

        static {
            Pattern p;
            try {
                p = Pattern.compile("(\\p{L1}|\\p{InLATIN_EXTENDED_A}|\\p{InLATIN_EXTENDED_B}|\\p{InLATIN_EXTENDED_C}|\\p{InLATIN_EXTENDED_D}|\\p{InLATIN_EXTENDED_E}).*");
            } catch (PatternSyntaxException ex) {
                log.warn("unable to create regex for latin characters", ex);
                // just in case the JVM doesn't recognize the language patterns used above
                p = Pattern.compile("[a-zA-Z]");
            }
            LATIN_CHARACTERS = p;
        }

        private String capitalize(String name, Locale locale) {
            if (LATIN_CHARACTERS.matcher(name).matches()) {
                return name.substring(0, 1).toUpperCase(locale) + name.substring(1);
            } else {
                return name;
            }
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
                properties.setProperty(propertyName, StringPropertyReplacer.replaceProperties(properties.getProperty(propertyName), SystemEnvProperties.UNFILTERED::getProperty));
            }
        }
    }

    private static Locale getParent(Locale locale) {
        return LocaleUtil.getParentLocale(locale);
    }

    private List<ThemeProvider> getProviders() {
        if (providers == null) {
            providers = new LinkedList(session.getAllProviders(ThemeProvider.class));
            Collections.sort(providers, (o1, o2) -> o2.getProviderPriority() - o1.getProviderPriority());
        }

        return providers;
    }

}
