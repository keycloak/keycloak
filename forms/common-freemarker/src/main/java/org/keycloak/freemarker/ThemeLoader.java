package org.keycloak.freemarker;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.util.ProviderLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ThemeLoader {

    private static final Logger logger = Logger.getLogger(ThemeLoader.class);
    public static final String BASE = "base";
    public static String DEFAULT = BASE;

    public static Theme createTheme(String name, Theme.Type type) throws FreeMarkerException {
        if (name == null) {
            name = DEFAULT;
        }

        Iterable<ThemeProvider> providers = ProviderLoader.load(ThemeProvider.class);

        Theme theme = findTheme(providers, name, type);
        if (theme.getParentName() != null) {
            List<Theme> themes = new LinkedList<Theme>();
            themes.add(theme);

            for (String parentName = theme.getParentName(); parentName != null; parentName = theme.getParentName()) {
                theme = findTheme(providers, parentName, type);
                themes.add(theme);
            }

            return new ExtendingTheme(themes);
        } else {
            return theme;
        }
    }

    private static Theme findTheme(Iterable<ThemeProvider> providers, String name, Theme.Type type) throws FreeMarkerException {
        for (ThemeProvider p : providers) {
            if (p.hasTheme(name, type)) {
                try {
                    return p.createTheme(name, type);
                } catch (IOException e) {
                    if (name.equals(BASE)) {
                        throw new FreeMarkerException("Failed to create " + type.toString().toLowerCase() + " theme", e);
                    } else {
                        logger.error("Failed to create " + type.toString().toLowerCase() + " theme", e);
                        return findTheme(providers, BASE, type);
                    }
                }
            }
        }

        if (name.equals(BASE)) {
            throw new FreeMarkerException(type.toString().toLowerCase() + " theme '" + name + "' not found");
        } else {
            logger.error(type.toString().toLowerCase() + " theme '" + name + "' not found");
            return findTheme(providers, BASE, type);
        }
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
