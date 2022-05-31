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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.Mappers;
import io.quarkus.logging.Log;
import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a version store of Secrets that are watched by a CR but is not owned by it. E.g. Secrets with
 * credentials provided by user.
 *
 * It is backed by a Secret which holds a list of watched Secrets together with their last observed version. It marks
 * all the watched Secrets with a label indicating which CRs are watching that resource.
 *
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class WatchedSecretsStore extends OperatorManagedResource {
    public static final String COMPONENT = "secrets-store";
    public static final String WATCHED_SECRETS_LABEL_VALUE = "watched-secret";
    public static final String STORE_SUFFIX = "-" + COMPONENT;

    private final Secret existingStore; // a Secret to store the last observed versions

    // key is name of the secret
    private final Map<String, String> lastObservedVersions;
    private final Map<String, String> currentVersions;
    private final Set<Secret> currentSecrets;

    public WatchedSecretsStore(Set<String> desiredWatchedSecretsNames, KubernetesClient client, Keycloak kc) {
        super(client, kc);
        existingStore = fetchExistingStore();
        lastObservedVersions = getNewLastObservedVersions();
        currentSecrets = fetchCurrentSecrets(desiredWatchedSecretsNames);
        currentVersions = getNewCurrentVersions();
    }

    /**
     * @return true if any of the watched Secrets was changed, false otherwise (incl. if it's a newly watched Secret)
     */
    public boolean changesDetected() {
        return currentVersions.entrySet().stream().anyMatch(e -> {
            String prevVersion = lastObservedVersions.get(e.getKey());
            return prevVersion != null && !prevVersion.equals(e.getValue());
        });
    }

    @Override
    protected Optional<HasMetadata> getReconciledResource() {
        Secret secret = existingStore != null ? existingStore : getNewStore();
        secret.setData(null);
        secret.setStringData(currentVersions);

        return Optional.of(secret);
    }

    @Override
    protected void setDefaultLabels(HasMetadata resource) {
        super.setDefaultLabels(resource);
        resource.getMetadata().getLabels().put(Constants.COMPONENT_LABEL, COMPONENT);
    }

    @Override
    public void createOrUpdateReconciled() {
        super.createOrUpdateReconciled();
        addLabelsToWatchedSecrets();
    }

    public void addLabelsToWatchedSecrets() {
        for (Secret secret : currentSecrets) {
            if (secret.getMetadata() == null
                    || secret.getMetadata().getLabels() == null
                    || !secret.getMetadata().getLabels().containsKey(Constants.KEYCLOAK_COMPONENT_LABEL)) {

                Log.infof("Adding label to Secret \"%s\"", secret.getMetadata().getName());

                secret = new SecretBuilder(secret)
                        .editMetadata()
                        .addToLabels(Constants.KEYCLOAK_COMPONENT_LABEL, WATCHED_SECRETS_LABEL_VALUE)
                        .endMetadata()
                        .build();

                client.secrets().inNamespace(secret.getMetadata().getNamespace()).withName(secret.getMetadata().getName()).patch(secret);
            }
        }
    }

    private Secret fetchExistingStore() {
        return client.secrets().inNamespace(getNamespace()).withName(getName()).get();
    }

    private Secret getNewStore() {
        return new SecretBuilder()
                .withNewMetadata()
                    .withName(getName())
                    .withNamespace(getNamespace())
                .endMetadata()
                .build();
    }

    private Map<String, String> getNewLastObservedVersions() {
        if (existingStore != null && existingStore.getData() != null) {
            return existingStore.getData().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> new String(Base64.getDecoder().decode(e.getValue()))
                    ));
        }
        else {
            return Collections.emptyMap();
        }
    }

    private Map<String, String> getNewCurrentVersions() {
        return currentSecrets.stream()
                .collect(Collectors.toMap(s -> s.getMetadata().getName(), this::getSecretVersion));
    }

    private String getSecretVersion(Secret secret) {
        String serializedData = Serialization.asYaml(secret.getData());
        try {
            // using hashes as it's more robust than resource versions that can change e.g. just when adding a label
            byte[] bytes = MessageDigest.getInstance("MD5").digest(serializedData.getBytes(StandardCharsets.UTF_8));
            return new BigInteger(1, bytes).toString(16);
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<Secret> fetchCurrentSecrets(Set<String> secretsNames) {
        return secretsNames.stream()
                .map(n -> {
                    Secret secret = client.secrets().inNamespace(getNamespace()).withName(n).get();
                    if (secret == null) {
                        throw new IllegalStateException("Secret " + n + " not found");
                    }
                    return secret;
                })
                .collect(Collectors.toSet());
    }

    @Override
    public String getName() {
        return cr.getMetadata().getName() + STORE_SUFFIX;
    }

    public static EventSource getStoreEventSource(KubernetesClient client, String namespace) {
        SharedIndexInformer<Secret> informer =
                client.secrets()
                        .inNamespace(namespace)
                        .withLabel(Constants.COMPONENT_LABEL, COMPONENT)
                        .runnableInformer(0);

        return new InformerEventSource<>(informer, Mappers.fromOwnerReference()) {
            @Override
            public String name() {
                return "watchedResourcesStoreEventSource";
            }
        };
    }

    private static void cleanObsoleteLabelFromSecret(KubernetesClient client, Secret secret) {
        secret.getMetadata().getLabels().remove(Constants.KEYCLOAK_COMPONENT_LABEL);
        client.secrets().inNamespace(secret.getMetadata().getNamespace()).withName(secret.getMetadata().getName()).patch(secret);
    }

    public static EventSource getWatchedSecretsEventSource(KubernetesClient client, String namespace) {
        SharedIndexInformer<Secret> informer =
                client.secrets()
                        .inNamespace(namespace)
                        .withLabel(Constants.KEYCLOAK_COMPONENT_LABEL, WATCHED_SECRETS_LABEL_VALUE)
                        .runnableInformer(0);

        return new InformerEventSource<>(informer, secret -> {
            // get all stores
            List<Secret> stores = client.secrets().inNamespace(namespace).withLabel(Constants.COMPONENT_LABEL, COMPONENT).list().getItems();

            // find all CR names that are watching this Secret
            var ret = stores.stream()
                    // check if any of the stores tracks this secret
                    .filter(store -> store.getData().containsKey(secret.getMetadata().getName()))
                    .map(store -> {
                        String crName = store.getMetadata().getName().split(STORE_SUFFIX)[0];
                        return new ResourceID(crName, namespace);
                    })
                    .collect(Collectors.toSet());

            if (ret.isEmpty()) {
                Log.infof("No CRs watching \"%s\" Secret, cleaning up labels", secret.getMetadata().getName());
                cleanObsoleteLabelFromSecret(client, secret);
                Log.debug("Labels removed");
            }

            return ret;
        }) {
            @Override
            public String name() {
                return "watchedSecretsEventSource";
            }
        };
    }
}
