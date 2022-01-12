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

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.keycloak.operator.v2alpha1.crds.Keycloak;
import org.keycloak.operator.v2alpha1.crds.KeycloakSpec;
import org.keycloak.operator.v2alpha1.crds.KeycloakStatus;

import java.net.URL;

import static org.keycloak.operator.v2alpha1.crds.KeycloakStatus.State.*;

public class KeycloakDeployment {

    KubernetesClient client = null;

    KeycloakDeployment(KubernetesClient client) {
        this.client = client;
    }

    private Deployment baseDeployment;

    public Deployment getKeycloakDeployment(Keycloak keycloak) {
        // TODO this should be done through an informer to leverage caches
        // WORKAROUND for: https://github.com/java-operator-sdk/java-operator-sdk/issues/781
        return client
                .apps()
                .deployments()
                .inNamespace(keycloak.getMetadata().getNamespace())
                .list()
                .getItems()
                .stream()
                .filter((d) -> d.getMetadata().getName().equals(org.keycloak.operator.Constants.NAME))
                .findFirst()
                .orElse(null);
//                .withName(Constants.NAME)
//                .get();
    }

    public void createKeycloakDeployment(Keycloak keycloak) {
        client
            .apps()
            .deployments()
            .inNamespace(keycloak.getMetadata().getNamespace())
            .create(newKeycloakDeployment(keycloak));
    }

    public Deployment newKeycloakDeployment(Keycloak keycloak) {
        if (baseDeployment == null) {
            URL url = this.getClass().getResource("/base-deployment.yaml");
            baseDeployment = client.apps().deployments().load(url).get();
        }

        var deployment = baseDeployment;

        deployment
                .getSpec()
                .setReplicas(keycloak.getSpec().getInstances());

        return new DeploymentBuilder(deployment).build();
    }

    public KeycloakStatus getNextStatus(KeycloakSpec desired, KeycloakStatus prev, Deployment current) {
        var isReady = (current != null &&
                current.getStatus() != null &&
                current.getStatus().getReadyReplicas() != null &&
                current.getStatus().getReadyReplicas() == desired.getInstances());

        var newStatus = new KeycloakStatus();
        if (isReady) {
            newStatus.setState(UNKNOWN);
            newStatus.setMessage("Keycloak status is unmanaged");
        } else {
            newStatus.setState(READY);
            newStatus.setMessage("Keycloak status is ready");
        }
        return newStatus;
    }

}
