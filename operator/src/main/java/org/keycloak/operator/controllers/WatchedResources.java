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

package org.keycloak.operator.controllers;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.keycloak.operator.Utils;

@ApplicationScoped
public class WatchedResources {
    public static final String KEYCLOAK_WATCHED_HASH_ANNOTATION_PREFIX = "operator.keycloak.org/watched-";
    public static final String KEYCLOAK_WATCHING_ANNOTATION_PREFIX = "operator.keycloak.org/watching-";
    public static final String KEYCLOAK_MISSING_ANNOTATION_PREFIX = "operator.keycloak.org/missing-";

    /**
     * @param deployment mutable resource being reconciled, it will be updated with
     *                   annotations
     */
    public <T extends HasMetadata> void annotateDeployment(List<String> names, Class<T> type, StatefulSet deployment,
            KubernetesClient client) {
        List<T> current = fetch(names, type, deployment.getMetadata().getNamespace(), client);
        String plural = HasMetadata.getPlural(type);
        deployment.getMetadata().getAnnotations().put(WatchedResources.KEYCLOAK_MISSING_ANNOTATION_PREFIX + plural,
                Boolean.valueOf(current.size() < names.size()).toString());
        deployment.getMetadata().getAnnotations().put(WatchedResources.KEYCLOAK_WATCHING_ANNOTATION_PREFIX + plural,
                String.join(";", names));
        deployment.getSpec().getTemplate().getMetadata().getAnnotations()
                .put(WatchedResources.KEYCLOAK_WATCHED_HASH_ANNOTATION_PREFIX + HasMetadata.getKind(type).toLowerCase() + "-hash", getHash(current));
    }

    public boolean hasMissing(StatefulSet deployment) {
        return deployment.getMetadata().getAnnotations().entrySet().stream()
                .anyMatch(e -> e.getKey().startsWith(WatchedResources.KEYCLOAK_MISSING_ANNOTATION_PREFIX)
                        && Boolean.parseBoolean(e.getValue()));
    }

    public boolean isWatching(StatefulSet deployment) {
        return deployment.getMetadata().getAnnotations().entrySet().stream()
                .anyMatch(e -> e.getKey().startsWith(WatchedResources.KEYCLOAK_WATCHING_ANNOTATION_PREFIX)
                        && e.getValue() != null && !e.getValue().isEmpty());
    }

    public <T extends HasMetadata> String getHash(List<T> current) {
        return Utils.hash(current);
    }

    private <T extends HasMetadata> List<T> fetch(List<String> names, Class<T> type, String namespace,
            KubernetesClient client) {
        return names.stream().map(n -> client.resources(type).inNamespace(namespace).withName(n).get())
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<String> getNames(StatefulSet deployment, Class<? extends HasMetadata> type) {
        return Optional
                .ofNullable(deployment.getMetadata().getAnnotations().get(WatchedResources.KEYCLOAK_WATCHING_ANNOTATION_PREFIX + HasMetadata.getPlural(type)))
                .filter(watching -> !watching.isEmpty())
                .map(watching -> watching.split(";")).map(Arrays::asList).orElse(List.of());
    }

}
