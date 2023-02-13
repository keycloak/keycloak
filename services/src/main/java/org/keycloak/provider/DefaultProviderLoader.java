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

package org.keycloak.provider;

import org.keycloak.theme.ClasspathThemeProviderFactory;
import org.keycloak.theme.ClasspathThemeResourceProviderFactory;
import org.keycloak.theme.ThemeResourceSpi;
import org.keycloak.theme.ThemeSpi;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultProviderLoader implements ProviderLoader {

    private KeycloakDeploymentInfo info;
    private ClassLoader classLoader;

    public DefaultProviderLoader(KeycloakDeploymentInfo info, ClassLoader classLoader) {
        this.info = info;
        this.classLoader = classLoader;
    }

    @Override
    public List<Spi> loadSpis() {
        if (info.hasServices()) {
            LinkedList<Spi> list = new LinkedList<>();
            for (Spi spi : ServiceLoader.load(Spi.class, classLoader)) {
                list.add(spi);
            }
            return list;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<ProviderFactory> load(Spi spi) {
        List<ProviderFactory> list = new LinkedList<>();
        if (info.hasServices()) {
            for (ProviderFactory f : ServiceLoader.load(spi.getProviderFactoryClass(), classLoader)) {
                list.add(f);
            }
        }

        if (spi.getClass().equals(ThemeResourceSpi.class) && info.hasThemeResources()) {
            ClasspathThemeResourceProviderFactory resourceProviderFactory = new ClasspathThemeResourceProviderFactory(info.getName(), classLoader);
            list.add(resourceProviderFactory);
        }

        if (spi.getClass().equals(ThemeSpi.class) && info.hasThemes()) {
            ClasspathThemeProviderFactory themeProviderFactory = new ClasspathThemeProviderFactory(info.getName(), classLoader);
            list.add(themeProviderFactory);
        }

        return list;
    }

}
