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
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusBuilder;

import java.util.HashMap;
import java.util.Optional;

public class KeycloakIngress extends OperatorManagedResource implements StatusUpdater<KeycloakStatusBuilder> {

    private final Ingress existingIngress;
    private final Keycloak keycloak;

    public KeycloakIngress(KubernetesClient client, Keycloak keycloakCR) {
        super(client, keycloakCR);
        this.keycloak = keycloakCR;
        this.existingIngress = fetchExistingIngress();
    }

    @Override
    protected Optional<HasMetadata> getReconciledResource() {
        if (keycloak.getSpec().isDisableDefaultIngress()) {
            if (existingIngress != null) {
                deleteExistingIngress();
            }
            return Optional.empty();
        } else {
            var defaultIngress = newIngress();
            var resultIngress = (existingIngress != null) ? existingIngress : defaultIngress;

            if (resultIngress.getMetadata().getAnnotations() == null) {
                resultIngress.getMetadata().setAnnotations(new HashMap<>());
            }
            resultIngress.getMetadata().getAnnotations().putAll(defaultIngress.getMetadata().getAnnotations());
            resultIngress.setSpec(defaultIngress.getSpec());
            return Optional.of(resultIngress);
        }
    }

    private Ingress newIngress() {
        var port = (keycloak.getSpec().isHttp()) ? Constants.KEYCLOAK_HTTP_PORT : Constants.KEYCLOAK_HTTPS_PORT;
        var backendProtocol = (keycloak.getSpec().isHttp()) ? "HTTP" : "HTTPS";

        Ingress ingress = new IngressBuilder()
                .withNewMetadata()
                    .withName(getName())
                    .withNamespace(getNamespace())
                    .addToAnnotations("nginx.ingress.kubernetes.io/backend-protocol", backendProtocol)
                    .addToAnnotations("route.openshift.io/termination", "passthrough")
                .endMetadata()
                .withNewSpec()
                    .withNewDefaultBackend()
                        .withNewService()
                            .withName(keycloak.getMetadata().getName() + Constants.KEYCLOAK_SERVICE_SUFFIX)
                            .withNewPort()
                                .withNumber(port)
                            .endPort()
                        .endService()
                    .endDefaultBackend()
                    .addNewRule()
                        .withNewHttp()
                            .addNewPath()
                                .withPath("")
                                .withPathType("ImplementationSpecific")
                                .withNewBackend()
                                    .withNewService()
                                        .withName(keycloak.getMetadata().getName() + Constants.KEYCLOAK_SERVICE_SUFFIX)
                                        .withNewPort()
                                            .withNumber(port)
                                            .endPort()
                                    .endService()
                                .endBackend()
                            .endPath()
                        .endHttp()
                    .endRule()
                .endSpec()
                .build();

        if (!keycloak.getSpec().isHostnameDisabled()) {
            ingress.getSpec().getRules().get(0).setHost(keycloak.getSpec().getHostname());
        }

        return ingress;
    }

    protected void deleteExistingIngress() {
        client.network().v1().ingresses().inNamespace(getNamespace()).delete(existingIngress);
    }

    protected Ingress fetchExistingIngress() {
        return client
                .network()
                .v1()
                .ingresses()
                .inNamespace(getNamespace())
                .withName(getName())
                .get();
    }

    public void updateStatus(KeycloakStatusBuilder status) {
        if (!keycloak.getSpec().isDisableDefaultIngress() && existingIngress == null) {
            status.addNotReadyMessage("No existing Keycloak Ingress found, waiting for creating a new one");
            return;
        }
    }

    public String getName() {
        return cr.getMetadata().getName() + Constants.KEYCLOAK_INGRESS_SUFFIX;
    }
}
