package org.keycloak.freemarker;

import org.keycloak.Config;
import org.keycloak.provider.ProviderSession;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ExtendingThemeManager implements ThemeProvider {

    private List<ThemeProvider> providers;
    private String defaultTheme;
    private int staticMaxAge;

    public ExtendingThemeManager(ProviderSession providerSession) {
        providers = new LinkedList();

        for (ThemeProvider p : providerSession.getAllProviders(ThemeProvider.class)) {
            if (!p.getClass().equals(ExtendingThemeManager.class)) {
                providers.add(p);
            }
        }

        Collections.sort(providers, new Comparator<ThemeProvider>() {
            @Override
            public int compare(ThemeProvider o1, ThemeProvider o2) {
                return o2.getProviderPriority() - o1.getProviderPriority();
            }
        });

        this.defaultTheme = Config.scope("theme").get("default");
        this.staticMaxAge = Config.scope("theme").getInt("staticMaxAge");
    }

    public int getStaticMaxAge() {
        return staticMaxAge;
    }

    @Override
    public int getProviderPriority() {
        return 0;
    }

    @Override
    public Theme createTheme(String name, Theme.Type type) throws IOException {
        if (name == null) {
            name = defaultTheme;
        }

        Theme theme = findTheme(name, type);
        if (theme.getParentName() != null) {
            List<Theme> themes = new LinkedList<Theme>();
            themes.add(theme);

            if (theme.getImportName() != null) {
                String[] s = theme.getImportName().split("/");
                themes.add(findTheme(s[1], Theme.Type.valueOf(s[0].toUpperCase())));
            }

            for (String parentName = theme.getParentName(); parentName != null; parentName = theme.getParentName()) {
                theme = findTheme(parentName, type);
                themes.add(theme);

                if (theme.getImportName() != null) {
                    String[] s = theme.getImportName().split("/");
                    themes.add(findTheme(s[1], Theme.Type.valueOf(s[0].toUpperCase())));
                }
            }

            return new ExtendingTheme(themes);
        } else {
            return theme;
        }
    }

    @Override
    public Set<String> nameSet(Theme.Type type) {
        Set<String> themes = new HashSet<String>();
        for (ThemeProvider p : providers) {
            themes.addAll(p.nameSet(type));
        }
        return themes;
    }

    @Override
    public boolean hasTheme(String name, Theme.Type type) {
        for (ThemeProvider p : providers) {
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
        for (ThemeProvider p : providers) {
            if (p.hasTheme(name, type)) {
                try {
                    return p.createTheme(name, type);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create " + type.toString().toLowerCase() + " theme", e);
                }
            }
        }
        throw new RuntimeException(type.toString().toLowerCase() + " theme '" + name + "' not found");
    }

    public static class ExtendingTheme implements Theme {

        private List<Theme> themes;

        public ExtendingTheme(List<Theme> themes) {
            this.themes = themes;
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
            return null;
        }

        @Override
        public InputStream getTemplateAsStream(String name) throws IOException {
            for (Theme t : themes) {
                InputStream template = t.getTemplateAsStream(name);
                if (template != null) {
                    return template;
                }
            }
            return null;
        }


        @Override
        public URL getResource(String path) throws IOException {
            for (Theme t : themes) {
                URL resource = t.getResource(path);
                if (resource != null) {
                    return resource;
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
            return null;
        }

        @Override
        public Properties getMessages() throws IOException {
            Properties messages = new Properties();
            ListIterator<Theme> itr = themes.listIterator(themes.size());
            while (itr.hasPrevious()) {
                Properties m = itr.previous().getMessages();
                if (m != null) {
                    messages.putAll(m);
                }
            }
            return messages;
        }

        @Override
        public Properties getProperties() throws IOException {
            Properties properties = new Properties();
            ListIterator<Theme> itr = themes.listIterator(themes.size());
            while (itr.hasPrevious()) {
                Properties p = itr.previous().getProperties();
                if (p != null) {
                    properties.putAll(p);
                }
            }
            return properties;
        }

    }

}
