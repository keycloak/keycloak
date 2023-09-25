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
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import io.quarkus.logging.Log;

import org.keycloak.operator.Constants;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a single K8s resource that is managed by this operator (e.g. Deployment, Service, Ingress, etc.)
 *
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public abstract class OperatorManagedResource<T extends HasMetadata> {
    private static final String KEYCLOAK_OPERATOR_FIELD_MANAGER = "keycloak-operator";
    protected KubernetesClient client;
    protected HasMetadata cr;

    public OperatorManagedResource(KubernetesClient client, HasMetadata cr) {
        this.client = client;
        this.cr = cr;
    }

    protected abstract Optional<T> getReconciledResource();

    public Optional<T> createOrUpdateReconciled() {
        return getReconciledResource().map(resource -> {
            try {
                setInstanceLabels(resource);
                setOwnerReferences(resource);

                Log.debugf("Creating or updating resource: %s", resource);
                try {
                    resource = client.resource(resource).inNamespace(getNamespace()).forceConflicts().fieldManager(KEYCLOAK_OPERATOR_FIELD_MANAGER).serverSideApply();
                } catch (KubernetesClientException e) {
                    if (e.getCode() != 422) {
                        throw e;
                    }
                    Log.infof("Could not apply changes to resource %s %s/%s will try strategic merge instead",
                            resource.getKind(), resource.getMetadata().getNamespace(),
                            resource.getMetadata().getName(), e.getMessage());
                    try {
                        client.resource(resource).patch(PatchContext.of(PatchType.STRATEGIC_MERGE));
                    } catch (KubernetesClientException ex) {
                        if (ex.getCode() == 422) {
                            Log.warnf("Could not apply changes to resource %s %s/%s if you have modified the resource please revert it or delete the resource so that the operator may regain control",
                                    resource.getKind(), resource.getMetadata().getNamespace(),
                                    resource.getMetadata().getName());
                        }
                        throw ex;
                    }
                }
                Log.debugf("Successfully created or updated resource: %s %s/%s", resource.getKind(), resource.getMetadata().getNamespace(),
                        resource.getMetadata().getName());
                return resource;
            } catch (Exception e) {
                Log.errorf("Failed to create or update resource %s %s/%s", resource.getKind(), resource.getMetadata().getNamespace(),
                        resource.getMetadata().getName());
                throw KubernetesClientException.launderThrowable(e);
            }
        });
    }

    protected void setInstanceLabels(HasMetadata resource) {
        resource.getMetadata().setLabels(updateWithInstanceLabels(resource.getMetadata().getLabels(), cr.getMetadata().getName()));
    }

    protected Map<String, String> getInstanceLabels() {
        return updateWithInstanceLabels(null, cr.getMetadata().getName());
    }

    public static Map<String, String> updateWithInstanceLabels(Map<String, String> labels, String instanceName) {
        labels = Optional.ofNullable(labels).orElse(new LinkedHashMap<>());
        labels.putAll(Constants.DEFAULT_LABELS);
        labels.put(Constants.INSTANCE_LABEL, instanceName);
        return labels;
    }

    public static Map<String, String> allInstanceLabels(HasMetadata primary) {
        var labels = new LinkedHashMap<>(Constants.DEFAULT_LABELS);
        labels.put(Constants.INSTANCE_LABEL, primary.getMetadata().getName());
        return labels;
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

    protected String getNamespace() {
        return cr.getMetadata().getNamespace();
    }

    protected abstract String getName();
}
