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

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.keycloak.provider.GeneratedProviderRegistry;
import org.keycloak.provider.KeycloakProvider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.quarkus.runtime.KeycloakRecorder;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
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

import java.lang.reflect.Modifier;

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
 *   <li>the class must implement {@link ProviderFactory} (checked via Jandex hierarchy walk),</li>
 *   <li>the class must declare a public no-arg constructor.</li>
 * </ul>
 */
class ProviderRegistryProcessor {

    private static final Logger logger = Logger.getLogger(ProviderRegistryProcessor.class);

    private static final DotName KEYCLOAK_PROVIDER = DotName.createSimple(KeycloakProvider.class.getName());
    private static final DotName PROVIDER_FACTORY = DotName.createSimple(ProviderFactory.class.getName());

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    ProviderRegistryBuildItem scanKeycloakProviders(CombinedIndexBuildItem indexBuildItem,
                                                   KeycloakRecorder recorder) {
        IndexView index = indexBuildItem.getIndex();
        TreeSet<String> classNames = new TreeSet<>();

        Collection<AnnotationInstance> annotations = index.getAnnotations(KEYCLOAK_PROVIDER);
        for (AnnotationInstance annotation : annotations) {
            AnnotationTarget target = annotation.target();
            if (target.kind() != AnnotationTarget.Kind.CLASS) {
                throw new IllegalStateException("@" + KeycloakProvider.class.getSimpleName()
                        + " is declared @Target(TYPE) but was found on " + target
                        + " (kind=" + target.kind() + ")");
            }

            ClassInfo classInfo = target.asClass();
            validate(classInfo, index);
            classNames.add(classInfo.name().toString());
        }

        recorder.installProviderRegistry(classNames);
        logger.debugf("Recorded install of %d @KeycloakProvider-annotated provider factories", classNames.size());

        return new ProviderRegistryBuildItem(classNames);
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    @Consume(ProviderRegistryBuildItem.class)
    void clearProviderRegistryOnShutdown(KeycloakRecorder recorder, ShutdownContextBuildItem shutdownContext) {
        recorder.clearProviderRegistryOnShutdown(shutdownContext);
    }

    private static void validate(ClassInfo classInfo, IndexView index) {
        if (!implementsProviderFactory(classInfo, index)) {
            throw new IllegalStateException("@" + KeycloakProvider.class.getSimpleName()
                    + " class " + classInfo.name() + " does not implement " + PROVIDER_FACTORY
                    + " (or its hierarchy is not in the Jandex index)");
        }
        if (!hasPublicNoArgConstructor(classInfo)) {
            throw new IllegalStateException("@" + KeycloakProvider.class.getSimpleName()
                    + " class " + classInfo.name() + " must have a public no-arg constructor");
        }
    }

    private static boolean implementsProviderFactory(ClassInfo classInfo, IndexView index) {
        ClassInfo current = classInfo;
        while (current != null) {
            for (DotName iface : current.interfaceNames()) {
                if (interfaceExtendsProviderFactory(iface, index)) {
                    return true;
                }
            }
            DotName superName = current.superName();
            if (superName == null || superName.equals(DotName.OBJECT_NAME)) {
                return false;
            }
            current = index.getClassByName(superName);
        }
        return false;
    }

    private static boolean interfaceExtendsProviderFactory(DotName ifaceName, IndexView index) {
        if (PROVIDER_FACTORY.equals(ifaceName)) {
            return true;
        }
        ClassInfo ifaceInfo = index.getClassByName(ifaceName);
        if (ifaceInfo == null) {
            return false;
        }
        for (DotName parent : ifaceInfo.interfaceNames()) {
            if (interfaceExtendsProviderFactory(parent, index)) {
                return true;
            }
        }
        return false;
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
