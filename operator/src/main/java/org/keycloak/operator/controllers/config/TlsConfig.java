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
import io.fabric8.kubernetes.api.model.ExecActionBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import org.keycloak.operator.Constants;
import org.keycloak.operator.controllers.KeycloakDeployment;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Deployment configuration for TLS properties
 */
public class TlsConfig implements DeploymentConfig {

    @Override
    public void validateConfiguration(KeycloakDeployment keycloakDeployment, KeycloakStatusBuilder status) {

    }

    @Override
    public void configure(KeycloakDeployment keycloakDeployment, StatefulSet deployment) {
        var kcContainer = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
        var keycloakCR = keycloakDeployment.getKeycloakCR();
        var tlsSecret = keycloakCR.getSpec().getTlsSecret();
        var envVars = kcContainer.getEnv();

        if (keycloakCR.getSpec().isHttp()) {
            var disableTls = List.of(
                    new EnvVarBuilder()
                            .withName("KC_HTTP_ENABLED")
                            .withValue("true")
                            .build(),
                    new EnvVarBuilder()
                            .withName("KC_HOSTNAME_STRICT_HTTPS")
                            .withValue("false")
                            .build(),
                    new EnvVarBuilder()
                            .withName("KC_PROXY")
                            .withValue("edge")
                            .build());

            envVars.addAll(disableTls);
        } else {
            var enabledTls = List.of(
                    new EnvVarBuilder()
                            .withName("KC_HTTPS_CERTIFICATE_FILE")
                            .withValue(Constants.CERTIFICATES_FOLDER + "/tls.crt")
                            .build(),
                    new EnvVarBuilder()
                            .withName("KC_HTTPS_CERTIFICATE_KEY_FILE")
                            .withValue(Constants.CERTIFICATES_FOLDER + "/tls.key")
                            .build(),
                    new EnvVarBuilder()
                            .withName("KC_PROXY")
                            .withValue("passthrough")
                            .build());

            envVars.addAll(enabledTls);

            var volume = new VolumeBuilder()
                    .withName("keycloak-tls-certificates")
                    .withNewSecret()
                    .withSecretName(tlsSecret)
                    .withOptional(false)
                    .endSecret()
                    .build();

            var volumeMount = new VolumeMountBuilder()
                    .withName(volume.getName())
                    .withMountPath(Constants.CERTIFICATES_FOLDER)
                    .build();

            deployment.getSpec().getTemplate().getSpec().getVolumes().add(volume);
            kcContainer.getVolumeMounts().add(volumeMount);
        }

        var kcRelativePath = keycloakDeployment.readConfigurationValue(Constants.KEYCLOAK_HTTP_RELATIVE_PATH_KEY).orElse("");
        var protocol = (keycloakCR.getSpec().isHttp()) ? "http" : "https";
        var kcPort = (keycloakCR.getSpec().isHttp()) ? Constants.KEYCLOAK_HTTP_PORT : Constants.KEYCLOAK_HTTPS_PORT;

        var baseProbe = new ArrayList<>(List.of("curl", "--head", "--fail", "--silent"));

        if (!keycloakCR.getSpec().isHttp()) {
            baseProbe.add("--insecure");
        }

        var readyProbe = new ArrayList<>(baseProbe);
        readyProbe.add(protocol + "://127.0.0.1:" + kcPort + kcRelativePath + "/health/ready");
        var liveProbe = new ArrayList<>(baseProbe);
        liveProbe.add(protocol + "://127.0.0.1:" + kcPort + kcRelativePath + "/health/live");

        kcContainer
                .getReadinessProbe()
                .setExec(new ExecActionBuilder().withCommand(readyProbe).build());
        kcContainer
                .getLivenessProbe()
                .setExec(new ExecActionBuilder().withCommand(liveProbe).build());
    }
}
