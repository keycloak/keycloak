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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.keycloak.provider.KeycloakProvider;
import org.keycloak.provider.ProviderFactory;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

/**
 * Scans the Quarkus Jandex index for classes annotated with {@link KeycloakProvider},
 * validates them at build time, and publishes them as a {@link ProviderRegistryBuildItem}
 * for {@link KeycloakProcessor#configureKeycloakSessionFactory} to feed into provider
 * discovery.
 *
 * Build-time validation:
 * <ul>
 *   <li>the annotation target must be a class (the annotation is already {@code @Target(TYPE)},
 *       so a non-class target indicates an illegal state),</li>
 *   <li>the class must be a public, non-abstract class (no interface, enum or annotation),</li>
 *   <li>the class must declare a public no-arg constructor (Jandex check).</li>
 * </ul>
 * The {@link ProviderFactory} assignability check happens inside
 * {@link #loadFactoryClasses(Set)} via {@link Class#asSubclass(Class)} so we do not need
 * to pull {@code keycloak-server-spi} into the Quarkus Jandex index just to walk the
 * interface chain.
 */
class ProviderRegistryProcessor {

    private static final Logger logger = Logger.getLogger(ProviderRegistryProcessor.class);

    private static final DotName KEYCLOAK_PROVIDER = DotName.createSimple(KeycloakProvider.class);

    @BuildStep
    ProviderRegistryBuildItem scanKeycloakProviders(CombinedIndexBuildItem indexBuildItem) {
        IndexView index = indexBuildItem.getIndex();
        TreeSet<String> classNames = new TreeSet<>();

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
            classNames.add(classInfo.name().toString());
        }

        logger.debugf("Discovered %d @KeycloakProvider-annotated provider factories", classNames.size());
        return new ProviderRegistryBuildItem(loadFactoryClasses(classNames));
    }

    @SuppressWarnings("rawtypes")
    private static Set<Class<? extends ProviderFactory>> loadFactoryClasses(Set<String> classNames) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Set<Class<? extends ProviderFactory>> classes = new LinkedHashSet<>(classNames.size());
        for (String className : classNames) {
            try {
                classes.add(Class.forName(className, false, classLoader).asSubclass(ProviderFactory.class));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("@" + KeycloakProvider.class.getSimpleName()
                        + " class " + className + " is in the Jandex index but not on the deployment classpath", e);
            } catch (ClassCastException e) {
                throw new IllegalStateException("@" + KeycloakProvider.class.getSimpleName()
                        + " class " + className + " does not implement " + ProviderFactory.class.getName(), e);
            }
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
