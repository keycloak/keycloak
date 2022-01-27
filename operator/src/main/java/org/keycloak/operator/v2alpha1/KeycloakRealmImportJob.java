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
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.quarkus.logging.Log;
import org.keycloak.operator.OperatorManagedResource;
import org.keycloak.operator.v2alpha1.crds.KeycloakRealmImport;
import org.keycloak.operator.v2alpha1.crds.KeycloakRealmImportStatusBuilder;

import java.util.List;
import java.util.Optional;

public class KeycloakRealmImportJob extends OperatorManagedResource {

    private final KeycloakRealmImport realmCR;
    private final Deployment existingDeployment;
    private final Job existingJob;
    private final String secretName;
    private final String volumeName;

    public KeycloakRealmImportJob(KubernetesClient client, KeycloakRealmImport realmCR, String secretName) {
        super(client, realmCR);
        this.realmCR = realmCR;
        this.secretName = secretName;
        this.volumeName = KubernetesResourceUtil.sanitizeName(secretName + "-volume");

        this.existingJob = fetchExistingJob();
        this.existingDeployment = fetchExistingDeployment();
    }

    @Override
    protected Optional<HasMetadata> getReconciledResource() {
        if (existingJob == null) {
            Log.info("Creating a new Job");
            return Optional.of(createImportJob());
        } else {
            Log.info("Job already available");
            return Optional.empty();
        }
    }

    private Job fetchExistingJob() {
        return client
                .batch()
                .v1()
                .jobs()
                .inNamespace(getNamespace())
                .withName(getName())
                .get();
    }

    private Deployment fetchExistingDeployment() {
        return client
                .apps()
                .deployments()
                .inNamespace(getNamespace())
                .withName(getKeycloakName())
                .get();
    }

    private Job buildJob(Container keycloakContainer, Volume secretVolume) {
        return new JobBuilder()
                .withNewMetadata()
                .withName(getName())
                .withNamespace(getNamespace())
                .endMetadata()
                .withNewSpec()
                .withNewTemplate()
                .withNewSpec()
                .withContainers(keycloakContainer)
                .addToVolumes(secretVolume)
                .withRestartPolicy("Never")
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }

    private Volume buildSecretVolume() {
        return new VolumeBuilder()
                .withName(volumeName)
                .withSecret(new SecretVolumeSourceBuilder()
                        .withSecretName(secretName)
                        .build())
                .build();
    }

    private Job createImportJob() {
        var keycloakContainer = buildKeycloakJobContainer();
        var secretVolume = buildSecretVolume();
        var importJob = buildJob(keycloakContainer, secretVolume);

        return importJob;
    }

    private Container buildKeycloakJobContainer() {
        var keycloakContainer =
            this
                .existingDeployment
                .getSpec()
                .getTemplate()
                .getSpec()
                .getContainers()
                .get(0);

        var importMntPath = "/mnt/realm-import/";

        var command = List.of("/bin/bash");

        var override = "--override=false";

        var commandArgs = List.of("-c",
                "/opt/keycloak/bin/kc.sh build && " +
                    "/opt/keycloak/bin/kc.sh import --file='" + importMntPath + getRealmName() + "-realm.json' " + override);

        keycloakContainer
                .setCommand(command);
        keycloakContainer
                .setArgs(commandArgs);
        var volumeMounts = List.of(
            new VolumeMountBuilder()
                    .withName(volumeName)
                    .withReadOnly(true)
                    .withMountPath(importMntPath)
                    .build());

        keycloakContainer.setVolumeMounts(volumeMounts);

        // Disable probes since we are not really starting the server
        keycloakContainer.setReadinessProbe(null);
        keycloakContainer.setLivenessProbe(null);

        return keycloakContainer;
    }


    public void updateStatus(KeycloakRealmImportStatusBuilder status) {
        if (existingDeployment == null) {
            status.addNotReadyMessage("No existing Deployment found, waiting for it to be created");
            return;
        }

        if (existingJob == null) {
            Log.info("Job about to start");
            status.addStartedMessage("Import Job will start soon");
        } else {
            Log.info("Job already executed - not recreating");
            var oldStatus = existingJob.getStatus();
            var lastReportedStatus = realmCR.getStatus();

            if (oldStatus == null) {
                Log.info("Job started");
                status.addStartedMessage("Import Job started");
            } else if (oldStatus.getSucceeded() != null && oldStatus.getSucceeded() > 0) {
                if (!lastReportedStatus.isDone()) {
                    Log.info("Job finished performing a rolling restart of the deployment");
                    rollingRestart();
                }
                status.addDone();
            } else if (oldStatus.getFailed() != null && oldStatus.getFailed() > 0) {
                Log.info("Job Failed");
                status.addErrorMessage("Import Job failed");
            } else {
                Log.info("Job running");
                status.addStartedMessage("Import Job running");
            }
        }
    }

    private String getName() {
        return realmCR.getMetadata().getName();
    }

    private String getNamespace() {
        return realmCR.getMetadata().getNamespace();
    }

    private String getKeycloakName() { return realmCR.getSpec().getKeycloakCRName(); }

    private String getRealmName() { return realmCR.getSpec().getRealm().getRealm(); }

    private void rollingRestart() {
        client.apps().deployments()
                .inNamespace(getNamespace())
                .withName(getKeycloakName())
                .rolling().restart();
    }
}
