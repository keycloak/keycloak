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

package org.keycloak.operator.controllers.config;

import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import org.keycloak.operator.controllers.KeycloakDeployment;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusBuilder;

import java.util.List;

/**
 * Deployment configuration for Hostname properties
 */
public class HostnameConfig implements DeploymentConfig {

    @Override
    public void validateConfiguration(KeycloakDeployment keycloakDeployment, KeycloakStatusBuilder status) {
    }

    @Override
    public void configure(KeycloakDeployment keycloakDeployment, StatefulSet deployment) {
        var kcContainer = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
        var keycloakCR = keycloakDeployment.getKeycloakCR();
        var hostname = keycloakCR.getSpec().getHostname();
        var envVars = kcContainer.getEnv();

        if (keycloakCR.getSpec().isHostnameDisabled()) {
            var disableStrictHostname = List.of(
                    new EnvVarBuilder()
                            .withName("KC_HOSTNAME_STRICT")
                            .withValue("false")
                            .build(),
                    new EnvVarBuilder()
                            .withName("KC_HOSTNAME_STRICT_BACKCHANNEL")
                            .withValue("false")
                            .build());

            envVars.addAll(disableStrictHostname);
        } else {
            var enabledStrictHostname = List.of(
                    new EnvVarBuilder()
                            .withName("KC_HOSTNAME")
                            .withValue(hostname)
                            .build());

            envVars.addAll(enabledStrictHostname);
        }
    }
}
