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

import org.keycloak.provider.GeneratedProviderRegistry;
import org.keycloak.provider.KeycloakProvider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.quarkus.runtime.KeycloakRecorder;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

/**
 * Scans the Quarkus Jandex index for classes annotated with {@link KeycloakProvider},
 * validates them at build time, and records a runtime call to populate
 * {@link GeneratedProviderRegistry} at {@code STATIC_INIT}. A second build step records
 * a shutdown task that clears the registry, so the static state does not leak across
 * in-JVM restarts.
 *
 * Build-time validation:
 * <ul>
 *   <li>the annotation target must be a class (the annotation is already {@code @Target(TYPE)},
 *       so a non-class target indicates an illegal state),</li>
 *   <li>the class must declare a public no-arg constructor (Jandex check).</li>
 * </ul>
 * The {@link ProviderFactory} assignability check happens inside
 * {@link #loadFactoryClasses(Set)} via {@link Class#asSubclass(Class)} so we do not need
 * to pull {@code keycloak-server-spi} into the Quarkus Jandex index just to walk the
 * interface chain.
 */
class ProviderRegistryProcessor {

    private static final Logger logger = Logger.getLogger(ProviderRegistryProcessor.class);

    private static final DotName KEYCLOAK_PROVIDER = DotName.createSimple(KeycloakProvider.class.getName());

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    @Produce(ProviderRegistryBuildItem.class)
    void scanKeycloakProviders(CombinedIndexBuildItem indexBuildItem,
                              KeycloakRecorder recorder) {
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
            if (!hasPublicNoArgConstructor(classInfo)) {
                throw new IllegalStateException("@" + KeycloakProvider.class.getSimpleName()
                        + " class " + classInfo.name() + " must have a public no-arg constructor");
            }
            classNames.add(classInfo.name().toString());
        }

        // The augmentation JVM's DefaultProviderLoader.load(Spi) — called from
        // KeycloakProcessor.configureKeycloakSessionFactory.loadFactories() in the same
        // build-step phase — reads the registry directly. Install now so the baked
        // factories map includes annotated factories whose META-INF/services entry was removed.
        GeneratedProviderRegistry.install(loadFactoryClasses(classNames));

        // Also record an install for the runtime JVM in case anything at runtime queries the
        // registry through DefaultProviderLoader. The shutdown step below pairs with this.
        recorder.installProviderRegistry(classNames);
        logger.debugf("Installed %d @KeycloakProvider-annotated provider factories", classNames.size());
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void clearProviderRegistryOnShutdown(KeycloakRecorder recorder, ShutdownContextBuildItem shutdownContext) {
        recorder.clearProviderRegistryOnShutdown(shutdownContext);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Set<Class<? extends ProviderFactory<?>>> loadFactoryClasses(Set<String> classNames) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Set<Class<? extends ProviderFactory<?>>> classes = new LinkedHashSet<>(classNames.size());
        for (String className : classNames) {
            try {
                Class<? extends ProviderFactory> raw = Class.forName(className, false, classLoader).asSubclass(ProviderFactory.class);
                classes.add((Class<? extends ProviderFactory<?>>) raw);
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

    private static boolean hasPublicNoArgConstructor(ClassInfo classInfo) {
        for (MethodInfo constructor : classInfo.constructors()) {
            if (constructor.parametersCount() == 0 && Modifier.isPublic(constructor.flags())) {
                return true;
            }
        }
        return false;
    }
}
