package org.keycloak.theme;

import org.keycloak.freemarker.Theme;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.Properties;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FolderTheme implements Theme {

    private String parentName;
    private String importName;
    private File themeDir;
    private File resourcesDir;
    private String name;
    private Type type;
    private final Properties properties;

    public FolderTheme(File themeDir, String name, Type type) throws IOException {
        this.themeDir = themeDir;
        this.name = name;
        this.type = type;
        this.properties = new Properties();

        File propertiesFile = new File(themeDir, "theme.properties");
        if (propertiesFile .isFile()) {
            properties.load(new FileInputStream(propertiesFile));
            parentName = properties.getProperty("parent");
            importName = properties.getProperty("import");
        }

        resourcesDir = new File(themeDir, "resources");
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
    public String getImportName() {
        return importName;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public URL getTemplate(String name) throws IOException {
        File file = new File(themeDir, name);
        return file.isFile() ? file.toURI().toURL() : null;
    }

    @Override
    public InputStream getTemplateAsStream(String name) throws IOException {
        URL url = getTemplate(name);
        return url != null ? url.openStream() : null;
    }

    @Override
    public URL getResource(String path) throws IOException {
        if (File.separatorChar != '/') {
            path = path.replace('/', File.separatorChar);
        }

        File file = new File(resourcesDir, path);
        if (!file.isFile() || !file.getCanonicalPath().startsWith(resourcesDir.getCanonicalPath())) {
            return null;
        } else {
            return file.toURI().toURL();
        }
    }

    @Override
    public InputStream getResourceAsStream(String path) throws IOException {
        URL url = getResource(path);
        return url != null ? url.openStream() : null;
    }

    @Override
    public Properties getMessages(Locale locale) throws IOException {
        if(locale == null){
            return null;
        }

        Properties m = new Properties();

        File file = new File(themeDir, "messages" + File.separator + "messages_" + locale.toString() + ".properties");
        if (file.isFile()) {
            m.load(new FileInputStream(file));
        }
        return m;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }
}
