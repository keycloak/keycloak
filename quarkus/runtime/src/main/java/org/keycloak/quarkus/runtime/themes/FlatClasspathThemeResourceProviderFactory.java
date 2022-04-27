/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.themes;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import org.keycloak.theme.ClasspathThemeResourceProviderFactory;

public class FlatClasspathThemeResourceProviderFactory extends ClasspathThemeResourceProviderFactory {

    public static final String ID = "flat-classpath";

    @Override
    public InputStream getResourceAsStream(String path) throws IOException {
        Enumeration<URL> resources = classLoader.getResources(THEME_RESOURCES_RESOURCES);

        while (resources.hasMoreElements()) {
            InputStream is = getResourceAsStream(path, resources.nextElement());

            if (is != null) {
                return is;
            }
        }

        return null;
    }

    @Override
    public Properties getMessages(String baseBundlename, Locale locale) throws IOException {
        Properties messages = new Properties();
        Enumeration<URL> resources = classLoader.getResources(THEME_RESOURCES_MESSAGES + baseBundlename + "_" + locale.toString() + ".properties");

        while (resources.hasMoreElements()) {
            loadMessages(messages, resources.nextElement());
        }

        return messages;
    }

    @Override
    public String getId() {
        return ID;
    }
}
