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

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.TreeSet;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;
import org.keycloak.provider.KeycloakProvider;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;

/**
 * Scans the Quarkus Jandex index for classes annotated with {@link KeycloakProvider}
 * and bakes the discovered class names into a generated resource that runtime code
 * can read in lieu of (or in addition to) {@code META-INF/services/...} descriptors.
 *
 * Coexists with {@link java.util.ServiceLoader}-based discovery: this scaffold step
 * does not yet wire the generated registry into provider loading. That is handled
 * in a follow-up step which adds the additive, deduplicating loader path.
 */
class ProviderRegistryProcessor {

    private static final Logger logger = Logger.getLogger(ProviderRegistryProcessor.class);

    /**
     * Path of the build-time-generated resource listing FQNs of annotated provider factories,
     * one per line, sorted lexicographically. Empty lines and lines starting with {@code #} are reserved.
     */
    static final String PROVIDER_LIST_RESOURCE = "META-INF/keycloak/keycloak-providers.list";

    private static final DotName KEYCLOAK_PROVIDER = DotName.createSimple(KeycloakProvider.class.getName());

    @BuildStep
    ProviderRegistryBuildItem scanKeycloakProviders(CombinedIndexBuildItem indexBuildItem,
                                                   BuildProducer<GeneratedResourceBuildItem> resources) {
        IndexView index = indexBuildItem.getIndex();
        TreeSet<String> classNames = new TreeSet<>();

        Collection<AnnotationInstance> annotations = index.getAnnotations(KEYCLOAK_PROVIDER);
        for (AnnotationInstance annotation : annotations) {
            if (annotation.target().kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }
            classNames.add(annotation.target().asClass().name().toString());
        }

        byte[] payload = String.join("\n", classNames).getBytes(StandardCharsets.UTF_8);
        resources.produce(new GeneratedResourceBuildItem(PROVIDER_LIST_RESOURCE, payload));

        logger.debugf("Discovered %d @KeycloakProvider-annotated provider factories", classNames.size());

        return new ProviderRegistryBuildItem(classNames);
    }
}
