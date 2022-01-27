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

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.logging.Log;
import org.keycloak.operator.Config;
import org.keycloak.operator.Constants;
import org.keycloak.operator.OperatorManagedResource;
import org.keycloak.operator.v2alpha1.crds.Keycloak;
import org.keycloak.operator.v2alpha1.crds.KeycloakStatusBuilder;

import java.net.URL;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

public class KeycloakDeployment extends OperatorManagedResource {

//    public static final Pattern CONFIG_SECRET_PATTERN = Pattern.compile("^\\$\\{secret:([^:]+):(.+)}$");

    private final Config config;
    private final Keycloak keycloakCR;
    private final Deployment existingDeployment;
    private final Deployment baseDeployment;

    public KeycloakDeployment(KubernetesClient client, Config config, Keycloak keycloakCR, Deployment existingDeployment) {
        super(client, keycloakCR);
        this.config = config;
        this.keycloakCR = keycloakCR;

        if (existingDeployment != null) {
            Log.info("Existing Deployment provided by controller");
            this.existingDeployment = existingDeployment;
        }
        else {
            Log.info("Trying to fetch existing Deployment from the API");
            this.existingDeployment = fetchExistingDeployment();
        }

        baseDeployment = createBaseDeployment();
    }

    @Override
    protected Optional<HasMetadata> getReconciledResource() {
        Deployment baseDeployment = new DeploymentBuilder(this.baseDeployment).build(); // clone not to change the base template
        Deployment reconciledDeployment;
        if (existingDeployment == null) {
            Log.info("No existing Deployment found, using the default");
            reconciledDeployment = baseDeployment;
        }
        else {
            Log.info("Existing Deployment found, updating specs");
            reconciledDeployment = existingDeployment;
            // don't override metadata, just specs
            reconciledDeployment.setSpec(baseDeployment.getSpec());
        }

        return Optional.of(reconciledDeployment);
    }

    private Deployment fetchExistingDeployment() {
        return client
                .apps()
                .deployments()
                .inNamespace(getNamespace())
                .withName(getName())
                .get();
    }

    private Deployment createBaseDeployment() {
        URL url = this.getClass().getResource("/base-keycloak-deployment.yaml");
        Deployment baseDeployment = client.apps().deployments().load(url).get();

        baseDeployment.getMetadata().setName(getName());
        baseDeployment.getMetadata().setNamespace(getNamespace());
        baseDeployment.getSpec().getSelector().setMatchLabels(Constants.DEFAULT_LABELS);
        baseDeployment.getSpec().setReplicas(keycloakCR.getSpec().getInstances());
        baseDeployment.getSpec().getTemplate().getMetadata().setLabels(Constants.DEFAULT_LABELS);

        Container container = baseDeployment.getSpec().getTemplate().getSpec().getContainers().get(0);
        container.setImage(Optional.ofNullable(keycloakCR.getSpec().getImage()).orElse(config.keycloak().image()));

        var serverConfig = new HashMap<>(Constants.DEFAULT_DIST_CONFIG);
        if (keycloakCR.getSpec().getServerConfiguration() != null) {
            serverConfig.putAll(keycloakCR.getSpec().getServerConfiguration());
        }

        container.setEnv(serverConfig.entrySet().stream()
                .map(e -> new EnvVarBuilder().withName(e.getKey()).withValue(e.getValue()).build())
                .collect(Collectors.toList()));

//        Set<String> configSecretsNames = new HashSet<>();
//        List<EnvVar> configEnvVars = serverConfig.entrySet().stream()
//                .map(e -> {
//                    EnvVarBuilder builder = new EnvVarBuilder().withName(e.getKey());
//                    Matcher matcher = CONFIG_SECRET_PATTERN.matcher(e.getValue());
//                    // check if given config var is actually a secret reference
//                    if (matcher.matches()) {
//                        builder.withValueFrom(
//                                new EnvVarSourceBuilder()
//                                        .withNewSecretKeyRef(matcher.group(2), matcher.group(1), false)
//                                        .build());
//                        configSecretsNames.add(matcher.group(1)); // for watching it later
//                    } else {
//                        builder.withValue(e.getValue());
//                    }
//                    builder.withValue(e.getValue());
//                    return builder.build();
//                })
//                .collect(Collectors.toList());
//        container.setEnv(configEnvVars);
//        this.configSecretsNames = Collections.unmodifiableSet(configSecretsNames);
//        Log.infof("Found config secrets names: %s", configSecretsNames);

        return baseDeployment;
    }

    public void updateStatus(KeycloakStatusBuilder status) {
        if (existingDeployment == null) {
            status.addNotReadyMessage("No existing Deployment found, waiting for creating a new one");
            return;
        }

        var replicaFailure = existingDeployment.getStatus().getConditions().stream()
                .filter(d -> d.getType().equals("ReplicaFailure")).findFirst();
        if (replicaFailure.isPresent()) {
            status.addNotReadyMessage("Deployment failures");
            status.addErrorMessage("Deployment failure: " + replicaFailure.get());
            return;
        }

        if (existingDeployment.getStatus() == null
                || existingDeployment.getStatus().getReadyReplicas() == null
                || existingDeployment.getStatus().getReadyReplicas() < keycloakCR.getSpec().getInstances()) {
            status.addNotReadyMessage("Waiting for more replicas");
        }
    }

//    public Set<String> getConfigSecretsNames() {
//        return configSecretsNames;
//    }

    public String getName() {
        return keycloakCR.getMetadata().getName();
    }

    public String getNamespace() {
        return keycloakCR.getMetadata().getNamespace();
    }

    public void rollingRestart() {
        client.apps().deployments()
                .inNamespace(getNamespace())
                .withName(getName())
                .rolling().restart();
    }
}
