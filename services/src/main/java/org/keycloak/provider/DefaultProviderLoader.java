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

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import org.keycloak.theme.ClasspathThemeProviderFactory;
import org.keycloak.theme.ClasspathThemeResourceProviderFactory;
import org.keycloak.theme.ThemeResourceSpi;
import org.keycloak.theme.ThemeSpi;

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
            // Build-time-generated registry from @KeycloakProvider annotation scan, then
            // ServiceLoader for anything not yet annotated (including third-party providers
            // that ship META-INF/services descriptors). Dedup is by factory Class so a
            // provider listed in both sources is instantiated only once.
            Set<Class<?>> seen = new HashSet<>();
            Class<? extends ProviderFactory> spiFactoryClass = spi.getProviderFactoryClass();
            for (Class<? extends ProviderFactory> factoryClass : GeneratedProviderRegistry.getProviderFactoryClasses()) {
                if (!spiFactoryClass.isAssignableFrom(factoryClass)) {
                    continue;
                }
                if (seen.add(factoryClass)) {
                    list.add(instantiate(factoryClass));
                }
            }
            for (ProviderFactory f : ServiceLoader.load(spiFactoryClass, classLoader)) {
                if (seen.add(f.getClass())) {
                    list.add(f);
                }
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

    private static ProviderFactory instantiate(Class<? extends ProviderFactory> factoryClass) {
        try {
            return factoryClass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Cannot instantiate provider factory " + factoryClass.getName()
                    + " — a public no-arg constructor is required", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Provider factory " + factoryClass.getName()
                    + " threw during instantiation", e.getCause());
        }
    }

}
