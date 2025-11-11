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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.keycloak.operator.Utils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WatchedResources {

    public static class Watched {
        public static Watched of(String... values) {
            Watched result = new Watched();
            Stream.of(values).forEach(v -> result.add(v, null));
            return result;
        }

        Map<String, Boolean> map = new LinkedHashMap<String, Boolean>();

        public void add(String name, Boolean optional) {
            map.merge(name, optional != null && optional, (b1, b2) -> b1 && b2);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Watched other = (Watched) obj;
            return Objects.equals(map, other.map);
        }

    }

    public static final String KEYCLOAK_WATCHED_HASH_ANNOTATION_PREFIX = "operator.keycloak.org/watched-";
    public static final String KEYCLOAK_WATCHING_ANNOTATION_PREFIX = "operator.keycloak.org/watching-";
    public static final String KEYCLOAK_MISSING_ANNOTATION_PREFIX = "operator.keycloak.org/missing-";

    /**
     * @param deployment mutable resource being reconciled, it will be updated with
     *                   annotations
     */
    public <T extends HasMetadata> void annotateDeployment(Watched watched, Class<T> type, StatefulSet deployment, Context<Keycloak> context) {
        if (watched.map.isEmpty()) {
            return;
        }

        List<T> current = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        watched.map.entrySet().stream().forEach(e -> {
            var resource = context.getClient().resources(type)
            .inNamespace(deployment.getMetadata().getNamespace()).withName(e.getKey()).get();
            if (resource == null) {
                if (!e.getValue()) {
                    missing.add(e.getKey());
                }
            } else {
                current.add(resource);
            }
        });

        String plural = HasMetadata.getPlural(type);
        if (!missing.isEmpty()) {
            deployment.getMetadata().getAnnotations().put(WatchedResources.KEYCLOAK_MISSING_ANNOTATION_PREFIX + plural, String.join(", ", missing));
        }
        deployment.getMetadata().getAnnotations().put(WatchedResources.KEYCLOAK_WATCHING_ANNOTATION_PREFIX + plural, Boolean.TRUE.toString());
        deployment.getSpec().getTemplate().getMetadata().getAnnotations()
                .put(WatchedResources.KEYCLOAK_WATCHED_HASH_ANNOTATION_PREFIX + HasMetadata.getKind(type).toLowerCase() + "-hash", Utils.hash(current));
    }

    public Optional<String> getMissing(StatefulSet deployment, Class<?> type) {
        String plural = HasMetadata.getPlural(type);
        return Optional.ofNullable(deployment.getMetadata().getAnnotations().get(WatchedResources.KEYCLOAK_MISSING_ANNOTATION_PREFIX + plural));
    }

    public boolean isWatching(StatefulSet deployment) {
        return deployment.getMetadata().getAnnotations().entrySet().stream()
                .anyMatch(e -> e.getKey().startsWith(WatchedResources.KEYCLOAK_WATCHING_ANNOTATION_PREFIX)
                        && e.getValue() != null && !e.getValue().isEmpty());
    }

}
