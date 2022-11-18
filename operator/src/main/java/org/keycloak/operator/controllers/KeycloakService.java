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
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HttpSpec;

import java.util.Optional;

import static org.keycloak.operator.crds.v2alpha1.CRDUtils.getValueFromSubSpec;
import static org.keycloak.operator.crds.v2alpha1.CRDUtils.isTlsConfigured;

public class KeycloakService extends OperatorManagedResource implements StatusUpdater<KeycloakStatusBuilder> {

    private Service existingService;
    private final Keycloak keycloak;

    public KeycloakService(KubernetesClient client, Keycloak keycloakCR) {
        super(client, keycloakCR);
        this.keycloak = keycloakCR;
        this.existingService = fetchExistingService();
    }

    private ServiceSpec getServiceSpec() {
        return new ServiceSpecBuilder()
              .addNewPort()
              .withPort(getServicePort(keycloak))
              .withProtocol(Constants.KEYCLOAK_SERVICE_PROTOCOL)
              .endPort()
              .withSelector(Constants.DEFAULT_LABELS)
              .build();
    }

    @Override
    protected Optional<HasMetadata> getReconciledResource() {
        var service = fetchExistingService();
        if (service == null) {
            service = newService();
        } else {
            service.setSpec(getServiceSpec());
        }

        return Optional.of(service);
    }

    private Service newService() {
        Service service = new ServiceBuilder()
                .withNewMetadata()
                .withName(getName())
                .withNamespace(getNamespace())
                .endMetadata()
                .withSpec(getServiceSpec())
                .build();
        return service;
    }

    private Service fetchExistingService() {
        return client
                .services()
                .inNamespace(getNamespace())
                .withName(getName())
                .get();
    }

    public void updateStatus(KeycloakStatusBuilder status) {
        if (existingService == null) {
            status.addNotReadyMessage("No existing Keycloak Service found, waiting for creating a new one");
            return;
        }
    }

    public String getName() {
        return cr.getMetadata().getName() + Constants.KEYCLOAK_SERVICE_SUFFIX;
    }

    public static int getServicePort(Keycloak keycloak) {
        // we assume HTTP when TLS is not configureed
        if (!isTlsConfigured(keycloak)) {
            return getValueFromSubSpec(keycloak.getSpec().getHttpSpec(), HttpSpec::getHttpPort).orElse(Constants.KEYCLOAK_HTTP_PORT);
        } else {
            return getValueFromSubSpec(keycloak.getSpec().getHttpSpec(), HttpSpec::getHttpsPort).orElse(Constants.KEYCLOAK_HTTPS_PORT);
        }
    }
}
