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

import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.ExecActionBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Configuration for the KeycloakDeployment
 */
public class KeycloakDeploymentConfig {
    private final Keycloak keycloakCR;
    private final StatefulSet deployment;
    private final KubernetesClient client;

    public KeycloakDeploymentConfig(Keycloak keycloakCR, StatefulSet deployment, KubernetesClient client) {
        this.keycloakCR = keycloakCR;
        this.deployment = deployment;
        this.client = client;
    }

    /**
     * Specify first-class citizens fields which should not be added as general server configuration property
     */
    private final static List<String> FIRST_CLASS_FIELDS = List.of(
            "hostname",
            "tlsSecret",
            "features",
            "features-disabled",
            "transaction-xa-enabled"
    );

    /**
     * Configure configuration properties for the KeycloakDeployment
     */
    protected void configureProperties() {
        configureHostname();
        configureTLS();
        configureFeatures();
        configureTransactions();
    }

    /**
     * Validate all deployment configuration properties and update status of the Keycloak deployment
     *
     * @param status Keycloak Status builder
     */
    protected void validateProperties(KeycloakStatusBuilder status) {
        assumeFirstClassCitizens(status);
    }

    /* ---------- Configuration of first-class citizen fields ---------- */

    public void configureHostname() {
        var kcContainer = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
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

    public void configureTLS() {
        var kcContainer = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
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

        var userRelativePath = readConfigurationValue(Constants.KEYCLOAK_HTTP_RELATIVE_PATH_KEY);
        var kcRelativePath = (userRelativePath == null) ? "" : userRelativePath;
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

    public void configureFeatures() {
        var featureSpec = keycloakCR.getSpec().getFeatureSpec();
        if (featureSpec == null) return;

        var kcContainer = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
        var envVars = kcContainer.getEnv();
        var enabledFeatures = featureSpec.getEnabledFeatures();
        var disabledFeatures = featureSpec.getDisabledFeatures();

        if (CollectionUtil.isNotEmpty(enabledFeatures)) {
            envVars.add(new EnvVarBuilder()
                    .withName("KC_FEATURES")
                    .withValue(CollectionUtil.join(enabledFeatures, ","))
                    .build());
        }

        if (CollectionUtil.isNotEmpty(disabledFeatures)) {
            envVars.add(new EnvVarBuilder()
                    .withName("KC_FEATURES_DISABLED")
                    .withValue(CollectionUtil.join(disabledFeatures, ","))
                    .build());
        }
    }

    public void configureTransactions() {
        var transactionsSpec = keycloakCR.getSpec().getTransactionsSpec();
        if (transactionsSpec == null) return;

        var kcContainer = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
        var envVars = kcContainer.getEnv();

        if (transactionsSpec.isXaEnabled() != null) {
            envVars.add(new EnvVarBuilder()
                    .withName("KC_TRANSACTION_XA_ENABLED")
                    .withValue(String.valueOf(transactionsSpec.isXaEnabled()))
                    .build());
        }
    }

    /* ---------- END of configuration of first-class citizen fields ---------- */

    protected String readConfigurationValue(String key) {
        if (keycloakCR != null &&
                keycloakCR.getSpec() != null &&
                keycloakCR.getSpec().getServerConfiguration() != null
        ) {

            var serverConfigValue = keycloakCR
                    .getSpec()
                    .getServerConfiguration()
                    .stream()
                    .filter(sc -> sc.getName().equals(key))
                    .findFirst();
            if (serverConfigValue.isPresent()) {
                if (serverConfigValue.get().getValue() != null) {
                    return serverConfigValue.get().getValue();
                } else {
                    var secretSelector = serverConfigValue.get().getSecret();
                    if (secretSelector == null) {
                        throw new IllegalStateException("Secret " + serverConfigValue.get().getName() + " not defined");
                    }
                    var secret = client.secrets().inNamespace(keycloakCR.getMetadata().getNamespace()).withName(secretSelector.getName()).get();
                    if (secret == null) {
                        throw new IllegalStateException("Secret " + secretSelector.getName() + " not found in cluster");
                    }
                    if (secret.getData().containsKey(secretSelector.getKey())) {
                        return new String(Base64.getDecoder().decode(secret.getData().get(secretSelector.getKey())), StandardCharsets.UTF_8);
                    } else {
                        throw new IllegalStateException("Secret " + secretSelector.getName() + " doesn't contain the expected key " + secretSelector.getKey());
                    }
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Assume the specified first-class citizens are not included in the general server configuration
     *
     * @param status                    Status of the deployment
     */
    protected void assumeFirstClassCitizens(KeycloakStatusBuilder status) {
        final var serverConfigNames = keycloakCR
                .getSpec()
                .getServerConfiguration()
                .stream()
                .map(ValueOrSecret::getName)
                .collect(Collectors.toSet());

        final var sameItems = CollectionUtil.intersection(serverConfigNames, FIRST_CLASS_FIELDS);
        if (CollectionUtil.isNotEmpty(sameItems)) {
            status.addWarningMessage("You need to specify these fields as the first-class citizen of the CR: "
                    + CollectionUtil.join(sameItems, ","));
        }
    }
}
