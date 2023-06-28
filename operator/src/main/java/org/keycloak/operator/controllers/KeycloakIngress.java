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
import org.keycloak.operator.crds.v2alpha1.deployment.spec.IngressSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusAggregator;

import java.util.HashMap;
import java.util.Optional;

import static org.keycloak.operator.crds.v2alpha1.CRDUtils.isTlsConfigured;

public class KeycloakIngress extends OperatorManagedResource implements StatusUpdater<KeycloakStatusAggregator> {

    private final Ingress existingIngress;
    private final Keycloak keycloak;

    public KeycloakIngress(KubernetesClient client, Keycloak keycloakCR) {
        super(client, keycloakCR);
        this.keycloak = keycloakCR;
        this.existingIngress = fetchExistingIngress();
    }

    @Override
    protected Optional<HasMetadata> getReconciledResource() {
        IngressSpec ingressSpec = keycloak.getSpec().getIngressSpec();
        if (ingressSpec != null && !ingressSpec.isIngressEnabled()) {
            if (existingIngress != null && existingIngress.hasOwnerReferenceFor(keycloak)) {
                deleteExistingIngress();
            }
            return Optional.empty();
        } else {
            return Optional.of(newIngress());
        }
    }

    private Ingress newIngress() {
        var port = KeycloakService.getServicePort(keycloak);
        var annotations = new HashMap<String, String>();

        // set default annotations
        if (isTlsConfigured(keycloak)) {
            annotations.put("nginx.ingress.kubernetes.io/backend-protocol", "HTTPS");
            annotations.put("route.openshift.io/termination", "passthrough");
        } else {
            annotations.put("nginx.ingress.kubernetes.io/backend-protocol", "HTTP");
            annotations.put("route.openshift.io/termination", "edge");
        }

        var optionalSpec = Optional.ofNullable(keycloak.getSpec().getIngressSpec());
        optionalSpec.map(IngressSpec::getAnnotations).ifPresent(annotations::putAll);

        Ingress ingress = new IngressBuilder()
                .withNewMetadata()
                    .withName(getName())
                    .withNamespace(getNamespace())
                    .addToAnnotations(annotations)
                .endMetadata()
                .withNewSpec()
                    .withIngressClassName(optionalSpec.map(IngressSpec::getIngressClassName).orElse(null))
                    .withNewDefaultBackend()
                        .withNewService()
                            .withName(keycloak.getMetadata().getName() + Constants.KEYCLOAK_SERVICE_SUFFIX)
                            .withNewPort()
                                .withNumber(port)
                                .withName("") // for SSA to clear the name if already set
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
                                            .withName("") // for SSA to clear the name if already set
                                            .endPort()
                                    .endService()
                                .endBackend()
                            .endPath()
                        .endHttp()
                    .endRule()
                .endSpec()
                .build();

        final var hostnameSpec = keycloak.getSpec().getHostnameSpec();
        if (hostnameSpec != null && hostnameSpec.getHostname() != null) {
            ingress.getSpec().getRules().get(0).setHost(hostnameSpec.getHostname());
        }

        return ingress;
    }

    protected void deleteExistingIngress() {
        client.resource(existingIngress).delete();
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

    @Override
    public void updateStatus(KeycloakStatusAggregator status) {
        IngressSpec ingressSpec = keycloak.getSpec().getIngressSpec();
        if (ingressSpec == null) {
            ingressSpec = new IngressSpec();
            ingressSpec.setIngressEnabled(true);
        }
        if (ingressSpec.isIngressEnabled() && existingIngress == null) {
            status.addNotReadyMessage("No existing Keycloak Ingress found, waiting for creating a new one");
        }
    }

    @Override
    public String getName() {
        return cr.getMetadata().getName() + Constants.KEYCLOAK_INGRESS_SUFFIX;
    }
}
