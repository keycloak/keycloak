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
package org.keycloak.operator.v2alpha1;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import org.keycloak.operator.Constants;
import org.keycloak.operator.OperatorManagedResource;
import org.keycloak.operator.StatusUpdater;
import org.keycloak.operator.v2alpha1.crds.Keycloak;
import org.keycloak.operator.v2alpha1.crds.KeycloakStatusBuilder;

import java.util.HashMap;
import java.util.Optional;

public class KeycloakIngress extends OperatorManagedResource implements StatusUpdater<KeycloakStatusBuilder> {

    private Ingress existingIngress;
    private final Keycloak keycloak;

    public KeycloakIngress(KubernetesClient client, Keycloak keycloakCR) {
        super(client, keycloakCR);
        this.keycloak = keycloakCR;
        this.existingIngress = fetchExistingIngress();
    }

    @Override
    protected Optional<HasMetadata> getReconciledResource() {
        var defaultIngress = newIngress();
        if (keycloak.getSpec().isDisableDefaultIngress() && existingIngress != null) {
            client.network().v1().ingresses().delete(existingIngress);
            return Optional.empty();
        } else if (existingIngress == null) {
            return Optional.of(defaultIngress);
        } else {
            if (existingIngress.getMetadata().getAnnotations() == null) {
                existingIngress.getMetadata().setAnnotations(new HashMap<>());
            }
            existingIngress.getMetadata().getAnnotations().putAll(defaultIngress.getMetadata().getAnnotations());
            existingIngress.setSpec(defaultIngress.getSpec());
            return Optional.of(existingIngress);
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
                                .withPath("/")
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

    private Ingress fetchExistingIngress() {
        return client
                .network()
                .v1()
                .ingresses()
                .inNamespace(getNamespace())
                .withName(getName())
                .get();
    }

    public void updateStatus(KeycloakStatusBuilder status) {
        if (existingIngress == null) {
            status.addNotReadyMessage("No existing Keycloak Ingress found, waiting for creating a new one");
            return;
        }
    }

    public String getName() {
        return cr.getMetadata().getName() + Constants.KEYCLOAK_INGRESS_SUFFIX;
    }
}
