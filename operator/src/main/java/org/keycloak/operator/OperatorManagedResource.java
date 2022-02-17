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

package org.keycloak.operator;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.logging.Log;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a single K8s resource that is managed by this operator (e.g. Deployment, Service, Ingress, etc.)
 *
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public abstract class OperatorManagedResource {
    protected KubernetesClient client;
    protected CustomResource<?, ?> cr;

    public OperatorManagedResource(KubernetesClient client, CustomResource<?, ?> cr) {
        this.client = client;
        this.cr = cr;
    }

    protected abstract Optional<HasMetadata> getReconciledResource();

    public void createOrUpdateReconciled() {
        getReconciledResource().ifPresent(resource -> {
            try {
                setDefaultLabels(resource);
                setOwnerReferences(resource);

                Log.debugf("Creating or updating resource: %s", resource);
                resource = client.resource(resource).createOrReplace();
                Log.debugf("Successfully created or updated resource: %s", resource);
            } catch (Exception e) {
                Log.error("Failed to create or update resource");
                Log.error(Serialization.asYaml(resource));
                throw e;
            }
        });
    }

    protected void setDefaultLabels(HasMetadata resource) {
        Map<String, String> labels = Optional.ofNullable(resource.getMetadata().getLabels()).orElse(new HashMap<>());
        labels.putAll(Constants.DEFAULT_LABELS);
        resource.getMetadata().setLabels(labels);
    }

    protected void setOwnerReferences(HasMetadata resource) {
        if (!cr.getMetadata().getNamespace().equals(resource.getMetadata().getNamespace())) {
            return;
        }

        OwnerReference owner = new OwnerReferenceBuilder()
                .withApiVersion(cr.getApiVersion())
                .withKind(cr.getKind())
                .withName(cr.getMetadata().getName())
                .withUid(cr.getMetadata().getUid())
                .withBlockOwnerDeletion(true)
                .withController(true)
                .build();

        resource.getMetadata().setOwnerReferences(Collections.singletonList(owner));
    }
}
