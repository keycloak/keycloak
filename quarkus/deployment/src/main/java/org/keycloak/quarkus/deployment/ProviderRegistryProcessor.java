/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.deployment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.keycloak.provider.GeneratedProviderRegistry;
import org.keycloak.provider.KeycloakProvider;
import org.keycloak.provider.ProviderFactory;

import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

/**
 * Scans the Quarkus Jandex index for classes annotated with {@link KeycloakProvider}
 * and installs the discovered classes into {@link GeneratedProviderRegistry} so that
 * {@link org.keycloak.provider.DefaultProviderLoader} can include them alongside
 * {@link java.util.ServiceLoader}-discovered factories.
 *
 * Validates each annotated class at build time:
 * <ul>
 *   <li>must implement {@link ProviderFactory},</li>
 *   <li>must have a public no-arg constructor <em>or</em> be a CDI bean.</li>
 * </ul>
 */
class ProviderRegistryProcessor {

    private static final Logger logger = Logger.getLogger(ProviderRegistryProcessor.class);

    private static final DotName KEYCLOAK_PROVIDER = DotName.createSimple(KeycloakProvider.class.getName());

    @BuildStep
    ProviderRegistryBuildItem scanKeycloakProviders(CombinedIndexBuildItem indexBuildItem,
                                                   BeanDiscoveryFinishedBuildItem beanDiscovery) {
        IndexView index = indexBuildItem.getIndex();
        TreeSet<String> classNames = new TreeSet<>();
        Set<Class<? extends ProviderFactory>> factoryClasses = new LinkedHashSet<>();

        Collection<AnnotationInstance> annotations = index.getAnnotations(KEYCLOAK_PROVIDER);
        for (AnnotationInstance annotation : annotations) {
            AnnotationTarget target = annotation.target();
            if (target.kind() != AnnotationTarget.Kind.CLASS) {
                throw new IllegalStateException("@" + KeycloakProvider.class.getSimpleName()
                        + " is declared @Target(TYPE) but was found on " + target
                        + " (kind=" + target.kind() + ")");
            }

            String className = target.asClass().name().toString();
            Class<? extends ProviderFactory> factoryClass = loadAndValidate(className, beanDiscovery);

            classNames.add(className);
            factoryClasses.add(factoryClass);
        }

        GeneratedProviderRegistry.install(factoryClasses);
        logger.debugf("Installed %d @KeycloakProvider-annotated provider factories into the registry", classNames.size());

        return new ProviderRegistryBuildItem(classNames);
    }

    private static Class<? extends ProviderFactory> loadAndValidate(String className, BeanDiscoveryFinishedBuildItem beanDiscovery) {
        Class<?> clazz;
        try {
            clazz = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("@" + KeycloakProvider.class.getSimpleName()
                    + " class " + className + " is in the Jandex index but not on the deployment classpath", e);
        }

        if (!ProviderFactory.class.isAssignableFrom(clazz)) {
            throw new IllegalStateException("@" + KeycloakProvider.class.getSimpleName()
                    + " class " + className + " does not implement " + ProviderFactory.class.getName());
        }

        Class<? extends ProviderFactory> factoryClass = clazz.asSubclass(ProviderFactory.class);

        if (!hasPublicNoArgConstructor(factoryClass) && !isCdiBean(className, beanDiscovery)) {
            throw new IllegalStateException("@" + KeycloakProvider.class.getSimpleName()
                    + " class " + className
                    + " must have a public no-arg constructor or be a CDI bean");
        }

        return factoryClass;
    }

    private static boolean hasPublicNoArgConstructor(Class<?> clazz) {
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == 0 && Modifier.isPublic(constructor.getModifiers())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCdiBean(String className, BeanDiscoveryFinishedBuildItem beanDiscovery) {
        DotName name = DotName.createSimple(className);
        return beanDiscovery.beanStream().withBeanClass(name).iterator().hasNext();
    }
}
