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
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.keycloak.operator.Constants;
import org.keycloak.operator.OperatorManagedResource;
import org.keycloak.operator.v2alpha1.crds.Keycloak;

import java.util.Optional;

public class KeycloakDiscoveryService extends OperatorManagedResource {

    public KeycloakDiscoveryService(KubernetesClient client, Keycloak keycloakCR) {
        super(client, keycloakCR);
    }

    private ServiceSpec getServiceSpec() {
      return new ServiceSpecBuilder()
              .withSelector(Constants.DEFAULT_LABELS)
              .addNewPort()
                .withTargetPort(new IntOrString(Constants.KEYCLOAK_DISCOVERY_SERVICE_PORT))
                .withPort(Constants.KEYCLOAK_DISCOVERY_SERVICE_PORT)
              .endPort()
              .withClusterIP("None")
              .build();
    }

    @Override
    protected Optional<HasMetadata> getReconciledResource() {
        return Optional.of(fetchExistingService()
                .map(a -> {
                            a.setSpec(getServiceSpec());
                            return a;
                            })
                .orElse(newService()));
    }

    private Service newService() {
        Service service = new ServiceBuilder()
                .withNewMetadata()
                    .withName(getName())
                    .withLabels(Constants.DEFAULT_LABELS)
                .endMetadata()
                .withSpec(getServiceSpec())
                .build();
        return service;
    }

    private Optional<Service> fetchExistingService() {
        return Optional.ofNullable(client
                .services()
                .inNamespace(getNamespace())
                .withName(getName())
                .get());
    }

    @Override
    protected String getName() {
        return cr.getMetadata().getName() + "-discovery";
    }
}
