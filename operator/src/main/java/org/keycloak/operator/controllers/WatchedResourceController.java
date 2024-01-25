/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.IndexerResourceCache;
import io.javaoperatorsdk.operator.processing.event.source.inbound.SimpleInboundEventSource;
import io.quarkus.logging.Log;

import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class WatchedResourceController<T extends HasMetadata> implements Reconciler<T>, EventSourceInitializer<T> {

    private volatile KubernetesClient client;
    private final Class<T> type;

    private final SimpleInboundEventSource eventSource = new SimpleInboundEventSource();

    private volatile IndexerResourceCache<T> cache;

    public WatchedResourceController(Class<T> type) {
        this.type = type;
    }

    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<T> context) {
        this.cache = context.getPrimaryCache();
        this.client = context.getClient();
        return Map.of();
    }

    @Override
    public UpdateControl<T> reconcile(T resource, Context<T> context) throws Exception {
        // find all statefulsets to notify
        //  - this could detect whether the reconciliation is even necessary if we track individual hashes
        var ret = client.apps().statefulSets().inNamespace(resource.getMetadata().getNamespace())
                .withLabels(Constants.DEFAULT_LABELS).list().getItems().stream()
                .filter(statefulSet -> getNames(statefulSet).contains(resource.getMetadata().getName()))
                .map(statefulSet -> new ResourceID(statefulSet.getMetadata().getName(),
                        resource.getMetadata().getNamespace()))
                .collect(Collectors.toSet());

        if (ret.isEmpty()) {
            Log.infof("Removing label from %s \"%s\"", resource.getKind(), resource.getMetadata().getName());
            resource.getMetadata().getLabels().remove(Constants.KEYCLOAK_COMPONENT_LABEL);
            return UpdateControl.updateResource(resource);
        } else {
            ret.forEach(eventSource::propagateEvent);
        }

        return UpdateControl.noUpdate();
    }

    public EventSource getEventSource() {
        return eventSource;
    }

    public void annotateDeployment(List<String> desiredNames, Keycloak keycloakCR, StatefulSet deployment) {
        List<T> current = fetch(desiredNames, keycloakCR.getMetadata().getNamespace());
        String kind = HasMetadata.getKind(type).toLowerCase();
        deployment.getMetadata().getAnnotations().put(WatchedResources.KEYCLOAK_MISSING_ANNOTATION_PREFIX + kind,
                Boolean.valueOf(current.size() < desiredNames.size()).toString());
        deployment.getMetadata().getAnnotations().put(WatchedResources.KEYCLOAK_WATCHING_ANNOTATION_PREFIX + kind, desiredNames.stream().collect(Collectors.joining(";")));
        deployment.getSpec().getTemplate().getMetadata().getAnnotations().put(WatchedResources.KEYCLOAK_WATCHED_HASH_ANNOTATION_PREFIX + kind, getHash(current));
    }

    private List<T> fetch(List<String> names, String namespace) {
        return names.stream()
                .map(n -> Optional.ofNullable(cache).flatMap(cache -> cache.get(new ResourceID(n, namespace)))
                        .orElseGet(() -> client.resources(type).inNamespace(namespace).withName(n).get()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public String getHash(List<T> current) {
        try {
            // using hashes as it's more robust than resource versions that can change e.g. just when adding a label
            // Uses a fips compliant hash
            var messageDigest = MessageDigest.getInstance("SHA-256");

            current.stream()
                    .map(s -> Serialization.asYaml(getData(s)).getBytes(StandardCharsets.UTF_8))
                    .forEachOrdered(s -> messageDigest.update(s));

            return new BigInteger(1, messageDigest.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    abstract Map<String, String> getData(T resource);

    public void addLabelsToWatched(StatefulSet deployment) {
        for (T resource : fetch(getNames(deployment), deployment.getMetadata().getNamespace())) {
            if (!resource.getMetadata().getLabels().containsKey(Constants.KEYCLOAK_COMPONENT_LABEL)) {

                Log.infof("Adding label to %s \"%s\"", resource.getKind(), resource.getMetadata().getName());

                client.resource(resource).accept(s -> {
                    s.getMetadata().getLabels().put(Constants.KEYCLOAK_COMPONENT_LABEL, WatchedResources.WATCHED_LABEL_VALUE_PREFIX + resource.getKind().toLowerCase());
                    s.getMetadata().setResourceVersion(null);
                });
            }
        }
    }

    public List<String> getNames(StatefulSet deployment) {
        return Optional
                .ofNullable(deployment.getMetadata().getAnnotations().get(WatchedResources.KEYCLOAK_WATCHING_ANNOTATION_PREFIX + HasMetadata.getKind(type).toLowerCase()))
                .filter(watching -> !watching.isEmpty())
                .map(watching -> watching.split(";")).map(Arrays::asList).orElse(List.of());
    }

}