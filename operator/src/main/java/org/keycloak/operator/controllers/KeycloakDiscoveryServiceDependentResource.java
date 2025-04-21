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

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import org.keycloak.operator.Constants;
import org.keycloak.operator.Utils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;

@KubernetesDependent(
        informer = @Informer(labelSelector = Constants.DEFAULT_LABELS_AS_STRING)
)
public class KeycloakDiscoveryServiceDependentResource extends CRUDKubernetesDependentResource<Service, Keycloak> {

    public KeycloakDiscoveryServiceDependentResource() {
        super(Service.class);
    }

    private ServiceSpec getServiceSpec(Keycloak keycloak) {
      return new ServiceSpecBuilder()
              .addNewPort()
              .withName(Constants.KEYCLOAK_DISCOVERY_TCP_PORT_NAME)
              .withProtocol("TCP")
              .withPort(Constants.KEYCLOAK_DISCOVERY_SERVICE_PORT)
              .endPort()
              .withSelector(Utils.allInstanceLabels(keycloak))
              .withClusterIP("None")
              .withPublishNotReadyAddresses(Boolean.TRUE)
              .build();
    }

    @Override
    protected Service desired(Keycloak primary, Context<Keycloak> context) {
        Service service = new ServiceBuilder()
                .withNewMetadata()
                .withName(getName(primary))
                .withNamespace(primary.getMetadata().getNamespace())
                .addToLabels(Utils.allInstanceLabels(primary))
                .endMetadata()
                .withSpec(getServiceSpec(primary))
                .build();
        return service;
    }

    public static String getName(Keycloak keycloak) {
        return keycloak.getMetadata().getName() + Constants.KEYCLOAK_DISCOVERY_SERVICE_SUFFIX;
    }
}
