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

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ControllerConfiguration(itemStore = WatchedStore.class)
public class WatchedSecretsController implements Reconciler<Secret>, EventSourceInitializer<Secret>, WatchedSecrets {

    private final SimpleInboundEventSource eventSource = new SimpleInboundEventSource();

    private volatile KubernetesClient client;
    private volatile IndexerResourceCache<Secret> secrets;
    private volatile WatchedStore<?> watchedStore;
    private Function<String, Stream<StatefulSet>> lister;

    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<Secret> context) {
        this.secrets = context.getPrimaryCache();
        this.client = context.getClient();
        this.watchedStore = (WatchedStore)context.getControllerConfiguration().getItemStore().orElseThrow();
        return Map.of();
    }

    @Override
    public synchronized void setStatefulSetLister(Function<String, Stream<StatefulSet>> lister) {
        this.lister = lister;
    }

    @Override
    public synchronized UpdateControl<Secret> reconcile(Secret resource, Context<Secret> context) throws Exception {
        if (lister == null) {
            return UpdateControl.noUpdate();
        }
        // find all statefulsets to notify
        //  - this could detect whether the reconciliation is even necessary if we track individual hashes
        var ret = lister.apply(resource.getMetadata().getNamespace())
                .filter(statefulSet -> getSecretNames(statefulSet).contains(resource.getMetadata().getName()))
                .map(statefulSet -> new ResourceID(statefulSet.getMetadata().getName(),
                        resource.getMetadata().getNamespace()))
                .collect(Collectors.toSet());

        if (ret.isEmpty()) {
            Log.infof("Stop watching Secret \"%s\"", resource.getMetadata().getName());
            ret.forEach(rid -> watchedStore.removeWatched(rid.getName(), rid.getNamespace().orElse(null)));
        } else {
            ret.forEach(eventSource::propagateEvent);
        }

        return UpdateControl.noUpdate();
    }

    @Override
    public EventSource getWatchedSecretsEventSource() {
        return eventSource;
    }

    @Override
    public void annotateDeployment(List<String> desiredWatchedSecretsNames, Keycloak keycloakCR, StatefulSet deployment) {
        List<Secret> currentSecrets = fetchSecrets(desiredWatchedSecretsNames, keycloakCR.getMetadata().getNamespace());
        deployment.getMetadata().getAnnotations().put(Constants.KEYCLOAK_MISSING_SECRETS_ANNOTATION,
                Boolean.valueOf(currentSecrets.size() < desiredWatchedSecretsNames.size()).toString());
        deployment.getMetadata().getAnnotations().put(Constants.KEYCLOAK_WATCHING_ANNOTATION, desiredWatchedSecretsNames.stream().collect(Collectors.joining(";")));
        deployment.getSpec().getTemplate().getMetadata().getAnnotations().put(Constants.KEYCLOAK_WATCHED_SECRET_HASH_ANNOTATION, getSecretHash(currentSecrets));
    }

    private List<Secret> fetchSecrets(List<String> secretsNames, String namespace) {
        return secretsNames.stream()
                .map(n -> Optional.ofNullable(secrets).flatMap(cache -> cache.get(new ResourceID(n, namespace)))
                        .orElseGet(() -> client.secrets().inNamespace(namespace).withName(n).get()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public String getSecretHash(List<Secret> currentSecrets) {
        try {
            // using hashes as it's more robust than resource versions that can change e.g. just when adding a label
            // Uses a fips compliant hash
            var messageDigest = MessageDigest.getInstance("SHA-256");

            currentSecrets.stream()
                    .map(s -> Serialization.asYaml(s.getData()).getBytes(StandardCharsets.UTF_8))
                    .forEachOrdered(s -> messageDigest.update(s));

            return new BigInteger(1, messageDigest.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void addWatched(StatefulSet deployment) {
        getSecretNames(deployment).forEach(s -> watchedStore.addWatched(s, deployment.getMetadata().getNamespace()));
    }

    public List<String> getSecretNames(StatefulSet deployment) {
        return Optional
                .ofNullable(deployment.getMetadata().getAnnotations().get(Constants.KEYCLOAK_WATCHING_ANNOTATION))
                .filter(watching -> !watching.isEmpty())
                .map(watching -> watching.split(";")).map(Arrays::asList).orElse(List.of());
    }

}
