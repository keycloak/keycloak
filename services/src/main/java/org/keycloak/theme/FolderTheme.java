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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
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
            Charset encoding = PropertiesUtil.detectEncoding(new FileInputStream(propertiesFile));
            try (Reader reader = new InputStreamReader(new FileInputStream(propertiesFile), encoding)) {
                properties.load(reader);
            }
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
    public InputStream getResourceAsStream(String path) throws IOException {
        if (File.separatorChar != '/') {
            path = path.replace('/', File.separatorChar);
        }

        File file = new File(resourcesDir, path);
        if (!file.isFile() || !file.getCanonicalPath().startsWith(resourcesDir.getCanonicalPath() + File.separator)) {
            return null;
        } else {
            return file.toURI().toURL().openStream();
        }
    }

    @Override
    public Properties getMessages(Locale locale) throws IOException {
        return getMessages("messages", locale);
    }

    @Override
    public Properties getMessages(String baseBundlename, Locale locale) throws IOException {
        if(locale == null){
            return null;
        }

        Properties m = new Properties();

        File file = new File(themeDir, "messages" + File.separator + baseBundlename + "_" + locale.toString() + ".properties");
        if (file.isFile()) {
            Charset encoding = PropertiesUtil.detectEncoding(new FileInputStream(file));
            try (Reader reader = new InputStreamReader(new FileInputStream(file), encoding)) {
                m.load(reader);
            }
        }
        return m;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }
}
