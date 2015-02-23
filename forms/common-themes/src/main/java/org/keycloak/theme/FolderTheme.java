package org.keycloak.theme;

import org.jboss.logging.Logger;
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

    private static final Logger LOGGER = Logger.getLogger(FolderTheme.class);
    private String parentName;
    private String importName;
    private File themeDir;
    private Type type;
    private final Properties properties;

    public FolderTheme(File themeDir, Type type) throws IOException {
        this.themeDir = themeDir;
        this.type = type;
        this.properties = new Properties();

        File propertiesFile = new File(themeDir, "theme.properties");
        if (propertiesFile .isFile()) {
            properties.load(new FileInputStream(propertiesFile));
            parentName = properties.getProperty("parent");
            importName = properties.getProperty("import");
        }
    }

    @Override
    public String getName() {
        return themeDir.getName();
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
        File file = new File(themeDir, "/resources/" + path);
        return file.isFile() ? file.toURI().toURL() : null;
    }

    @Override
    public InputStream getResourceAsStream(String path) throws IOException {
        URL url = getResource(path);
        return url != null ? url.openStream() : null;
    }

    @Override
    public Properties getMessages(Locale locale) throws IOException {
        Properties m = new Properties();

        File file = null;
        if(locale != null) {
            File messageFile = new File(themeDir, "messages" + File.separator + "messages_" + locale.toString() + ".properties");
            if (messageFile.isFile()) {
                file = messageFile;
            } else {
                LOGGER.warnf("Can not find message file %s", messageFile);
            }
        }

        if(file == null){
            file = new File(themeDir, "messages" + File.separator + "messages.properties");
        }

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
