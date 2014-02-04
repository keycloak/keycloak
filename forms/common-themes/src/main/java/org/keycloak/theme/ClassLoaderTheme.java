package org.keycloak.theme;

import org.keycloak.freemarker.Theme;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClassLoaderTheme implements Theme {

    private final String name;

    private final String parentName;

    private final Type type;

    private final String templateRoot;

    private final String resourceRoot;

    private final String messages;

    private final Properties properties;

    public ClassLoaderTheme(String name, Type type) throws IOException {
        this.name = name;
        this.type = type;

        String themeRoot = "theme/" + type.toString().toLowerCase() + "/" + name + "/";

        this.templateRoot = themeRoot;
        this.resourceRoot = themeRoot + "resources/";
        this.messages = themeRoot + "messages/messages.properties";
        this.properties = new Properties();

        URL p = getClass().getClassLoader().getResource(themeRoot + "theme.properties");
        if (p != null) {
            properties.load(p.openStream());
            this.parentName = properties.getProperty("parent");
        } else {
            this.parentName = null;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getParentName() {
        return parentName;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public URL getTemplate(String name) {
        return getClass().getClassLoader().getResource(templateRoot + name);
    }

    @Override
    public InputStream getTemplateAsStream(String name) {
        return getClass().getClassLoader().getResourceAsStream(templateRoot + name);
    }

    @Override
    public URL getResource(String path) {
        return getClass().getClassLoader().getResource(resourceRoot + path);
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        return getClass().getClassLoader().getResourceAsStream(resourceRoot + path);
    }

    @Override
    public Properties getMessages() throws IOException {
        Properties m = new Properties();
        URL url = getClass().getClassLoader().getResource(this.messages);
        if (url != null) {
            m.load(url.openStream());
        }
        return m;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

}
