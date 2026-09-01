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

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.keycloak.provider.KeycloakProvider;
import org.keycloak.provider.ProviderFactory;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

/**
 * Scans the Quarkus Jandex index for classes annotated with {@link KeycloakProvider},
 * validates them at build time, and publishes them as a {@link ProviderRegistryBuildItem}
 * for {@link KeycloakProcessor#configureKeycloakSessionFactory} to feed into provider
 * discovery. Each annotated class is registered for the provider factory interface named
 * by {@link KeycloakProvider#value()}, mirroring a {@code META-INF/services/<value>} entry.
 *
 * Build-time validation:
 * <ul>
 *   <li>the annotation target must be a class (the annotation is already {@code @Target(TYPE)},
 *       so a non-class target indicates an illegal state),</li>
 *   <li>the class must be a public, non-abstract class (no interface, enum or annotation),</li>
 *   <li>the class must declare a public no-arg constructor (Jandex check),</li>
 *   <li>the annotation value must be a {@link ProviderFactory} type and the class must implement it.</li>
 * </ul>
 * The assignability checks happen inside {@link #loadFactoryClasses(SortedMap)} via
 * {@link Class#asSubclass(Class)} / {@link Class#isAssignableFrom(Class)} so we do not need to
 * pull {@code keycloak-server-spi} into the Quarkus Jandex index just to walk the interface chain.
 */
class ProviderRegistryProcessor {

    private static final Logger logger = Logger.getLogger(ProviderRegistryProcessor.class);

    private static final DotName KEYCLOAK_PROVIDER = DotName.createSimple(KeycloakProvider.class);

    @BuildStep
    ProviderRegistryBuildItem scanKeycloakProviders(CombinedIndexBuildItem indexBuildItem) {
        IndexView index = indexBuildItem.getIndex();
        // factory interface name -> annotated class names, both sorted for a deterministic build
        SortedMap<String, SortedSet<String>> classNames = new TreeMap<>();

        Collection<AnnotationInstance> annotations = index.getAnnotations(KEYCLOAK_PROVIDER);
        for (AnnotationInstance annotation : annotations) {
            AnnotationTarget target = annotation.target();
            if (target.kind() != AnnotationTarget.Kind.CLASS) {
                throw new IllegalStateException("@" + KeycloakProvider.class.getSimpleName()
                        + " must be placed on a class but was found on " + target);
            }

            ClassInfo classInfo = target.asClass();
            if (!isPublicConcreteClass(classInfo)) {
                throw new IllegalStateException("@" + KeycloakProvider.class.getSimpleName()
                        + " class " + classInfo.name() + " must be a public, non-abstract class");
            }
            if (!hasPublicNoArgConstructor(classInfo)) {
                throw new IllegalStateException("@" + KeycloakProvider.class.getSimpleName()
                        + " class " + classInfo.name() + " must have a public no-arg constructor");
            }

            AnnotationValue value = annotation.value();
            if (value == null) {
                throw new IllegalStateException("@" + KeycloakProvider.class.getSimpleName()
                        + " on class " + classInfo.name() + " must specify the provider factory interface it is registered for");
            }
            String factoryInterfaceName = value.asClass().name().toString();
            classNames.computeIfAbsent(factoryInterfaceName, key -> new TreeSet<>()).add(classInfo.name().toString());
        }

        logger.debugf("Discovered %d @KeycloakProvider-annotated provider factories", annotations.size());
        return new ProviderRegistryBuildItem(loadFactoryClasses(classNames));
    }

    private static Map<Class<? extends ProviderFactory>, Set<Class<? extends ProviderFactory>>> loadFactoryClasses(
            SortedMap<String, SortedSet<String>> classNames) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Map<Class<? extends ProviderFactory>, Set<Class<? extends ProviderFactory>>> classes = new LinkedHashMap<>();
        for (Map.Entry<String, SortedSet<String>> entry : classNames.entrySet()) {
            String factoryInterfaceName = entry.getKey();
            Class<? extends ProviderFactory> factoryInterface;
            try {
                factoryInterface = Class.forName(factoryInterfaceName, false, classLoader).asSubclass(ProviderFactory.class);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("@" + KeycloakProvider.class.getSimpleName()
                        + " value " + factoryInterfaceName + " is not on the deployment classpath", e);
            } catch (ClassCastException e) {
                throw new IllegalStateException("@" + KeycloakProvider.class.getSimpleName()
                        + " value " + factoryInterfaceName + " is not a " + ProviderFactory.class.getName() + " type", e);
            }

            Set<Class<? extends ProviderFactory>> implementations = new LinkedHashSet<>();
            for (String className : entry.getValue()) {
                Class<?> implementation;
                try {
                    implementation = Class.forName(className, false, classLoader);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("@" + KeycloakProvider.class.getSimpleName()
                            + " class " + className + " is in the Jandex index but not on the deployment classpath", e);
                }
                if (!factoryInterface.isAssignableFrom(implementation)) {
                    throw new IllegalStateException("@" + KeycloakProvider.class.getSimpleName()
                            + " class " + className + " does not implement " + factoryInterfaceName);
                }
                implementations.add(implementation.asSubclass(factoryInterface));
            }
            classes.put(factoryInterface, implementations);
        }
        return classes;
    }

    private static boolean isPublicConcreteClass(ClassInfo classInfo) {
        short flags = classInfo.flags();
        return !classInfo.isInterface() && !classInfo.isEnum() && !classInfo.isAnnotation()
                && Modifier.isPublic(flags) && !Modifier.isAbstract(flags);
    }

    private static boolean hasPublicNoArgConstructor(ClassInfo classInfo) {
        for (MethodInfo constructor : classInfo.constructors()) {
            if (constructor.parametersCount() == 0 && Modifier.isPublic(constructor.flags())) {
                return true;
            }
        }
        return false;
    }
}
