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

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.Constants;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import org.jboss.logging.Logger;
import org.keycloak.operator.v2alpha1.crds.realm.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.keycloak.operator.Constants.*;

@ControllerConfiguration(namespaces = Constants.WATCH_CURRENT_NAMESPACE, finalizerName = Constants.NO_FINALIZER)
public class RealmImporterController implements Reconciler<RealmImporter>, ErrorStatusHandler<RealmImporter>, EventSourceInitializer<RealmImporter> {

    @Inject
    Logger logger;

    @Inject
    KubernetesClient client;

    @Inject
    ObjectMapper jsonMapper;

    @Override
    public UpdateControl<RealmImporter> reconcile(RealmImporter realm, Context context) {
        logger.trace("Realm Importer - Reconcile loop started");
        var kcDeployment = new KeycloakDeployment(client);

        var kc = kcDeployment.getKeycloakDeployment(
                realm.getSpec().getKeycloakCRName(),
                realm.getMetadata().getNamespace()
        );

        if (kc == null) {
            var status = new RealmImporterStatus();
            status.setError(true);
            status.setState(RealmImporterStatus.State.ERROR);
            status.setMessage("Keycloak Deployment doesn't exists");
            realm.setStatus(status);
            return UpdateControl.updateStatus(realm);
        }

        // Write the realm representation secret
        String content = "";
        try {
            content = jsonMapper.writeValueAsString(realm.getSpec().getRealm());
        } catch (JsonProcessingException cause) {
            throw new RuntimeException("Failed to read the Realm Representation", cause);
        }

        var realmName = realm.getSpec().getRealm().getRealm();
        handleRealmSecret(kc, realmName, content);

        // Run the import job and get the result
        var newStatus = handleImportJob(kc, realmName, realm.getMetadata().getName());

        if (realm.getStatus() != newStatus) {
            realm.setStatus(newStatus);
            return UpdateControl.updateStatus(realm);
        } else {
            return UpdateControl.noUpdate();
        }
    }

    private String getSecretName(Deployment deployment) {
        return deployment.getMetadata().getName() + "-realms";
    }

    private void handleRealmSecret(Deployment deployment, String realmName, String content) {
        var secretName = getSecretName(deployment);
        var namespace = deployment.getMetadata().getNamespace();
        var fileName = realmName + "-realm.json";

        var secretSelector = client
                .secrets()
                .inNamespace(namespace)
                .withName(secretName);

        var secret = secretSelector.get();

        if (secret != null) {
            logger.info("Updating Secret " + secretName);
            var updatedData = secret.getStringData();
            if (updatedData != null) {
                if (updatedData.containsKey(fileName) && updatedData.get(fileName).equals(content)) {
                    logger.info("Secret was already updated");
                } else {
                    updatedData.put(fileName, content);
                    secret.setStringData(updatedData);
                    secretSelector.patch(secret);
                }
            } else {
                updatedData = new HashMap<String, String>();
                updatedData.put(fileName, content);
                secret.setStringData(updatedData);
                secretSelector.patch(secret);
            }
        } else {
            logger.info("Creating Secret " + secretName);
            secretSelector.create(
                    new SecretBuilder()
                            .withNewMetadata()
                            .withName(secretName)
                            .withNamespace(namespace)
                            .addToLabels(MANAGED_BY_LABEL, MANAGED_BY_VALUE)
                            .withOwnerReferences(deployment.getMetadata().getOwnerReferences())
                            .endMetadata()
                            .addToStringData(fileName, content)
                            .build()
            );
        }
    }

    private RealmImporterStatus handleImportJob(Deployment deployment, String realmName, String realmCRName) {
        var name = "realm-" + realmName + "-importer";
        var namespace = deployment.getMetadata().getNamespace();
        var secretName = getSecretName(deployment);
        var volumeName = secretName + "-volume";
        var keycloakTemplate = deployment.getSpec().getTemplate();
        var keycloakContainer = keycloakTemplate.getSpec().getContainers().get(0);

        var importMntPath = "/mnt/realm-import/";

        var dbOptions = keycloakContainer
                .getArgs()
                .stream()
                .filter((p) -> p.startsWith("--db"))
                .collect(Collectors.toList());

        var command = new ArrayList<String>();
        command.add("/bin/bash");

        var commandArgs = new ArrayList<String>();
        var dbOptsString = dbOptions.stream().reduce("", (o, n) -> o + " " + n);
        commandArgs.add("-c");
        commandArgs.add("/opt/keycloak/bin/kc.sh build" + dbOptsString + " && " +
                "/opt/keycloak/bin/kc.sh import --file='" + importMntPath + realmName + "-realm.json' --override=true");

        keycloakContainer
                .setCommand(command);
        keycloakContainer
                .setArgs(commandArgs);
        var volumeMounts = new ArrayList<VolumeMount>();
        volumeMounts.add(
                new VolumeMountBuilder()
                .withName(volumeName)
                .withReadOnly(true)
                .withMountPath(importMntPath)
                .build()
        );
        keycloakContainer.setVolumeMounts(volumeMounts);

        var secretVolume = new VolumeBuilder()
                .withName(volumeName)
                .withSecret(new SecretVolumeSourceBuilder()
                        .withSecretName(secretName)
                        .build())
                .build();

        var job = new JobBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .addToLabels(PART_OF_LABEL, realmCRName)
                .addToLabels(MANAGED_BY_LABEL, MANAGED_BY_VALUE)
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

        var prevJob = client
                .batch()
                .v1()
                .jobs()
                .inNamespace(namespace)
                .withName(name)
                .get();

        if (prevJob != null) {
            logger.info("Job already executed - not recreating");
            var oldStatus = prevJob.getStatus();
            var newStatus = new RealmImporterStatus();

            if (oldStatus != null) {
                if (oldStatus.getSucceeded() != null && oldStatus.getSucceeded() >= 0) {
                    newStatus.setState(RealmImporterStatus.State.DONE);
                } else if (oldStatus.getFailed() != null && oldStatus.getFailed() >= 0) {
                    newStatus.setError(true);
                    newStatus.setState(RealmImporterStatus.State.ERROR);
                } else {
                    newStatus.setState(RealmImporterStatus.State.STARTED);
                }
            } else {
                newStatus.setState(RealmImporterStatus.State.UNKNOWN);
            }
            return newStatus;
        } else {
            client
                    .batch()
                    .v1()
                    .jobs()
                    .create(job);

            var status = new RealmImporterStatus();
            status.setState(RealmImporterStatus.State.STARTED);
            return status;
        }
    }

    @Override
    public DeleteControl cleanup(RealmImporter realm, Context context) {
        // need to cleanup ended jobs? or simply use OwnerReferences ...
        return DeleteControl.defaultDelete();
    }

    @Override
    public Optional<RealmImporter> updateErrorStatus(RealmImporter realmImporter, RetryInfo retryInfo, RuntimeException e) {
        var status = realmImporter.getStatus();
        if (status == null) {
            status = new RealmImporterStatus();
        }
        status.setState(RealmImporterStatus.State.ERROR);
        status.setError(true);
        status.setMessage("Error: " + e.getMessage());
        return Optional.of(realmImporter);
    }

    @Override
    public List<EventSource> prepareEventSources(EventSourceContext<RealmImporter> context) {
        return List.of(new InformerEventSource<>(
                client, Job.class, job -> {
                    if (job.getMetadata().getLabels() != null &&
                            job.getMetadata().getLabels().containsKey(PART_OF_LABEL)) {
                        return context.getPrimaryCache()
                                .list(kc -> kc.getMetadata().getName().equals(job.getMetadata().getLabels().get(PART_OF_LABEL)))
                                .map(ResourceID::fromResource)
                                .collect(Collectors.toSet());
                    } else {
                        return Set.of();
                    }
        },
                (RealmImporter realm) -> new ResourceID(realm.getCRDName(),
                        realm.getMetadata().getNamespace()),
                true));
    }
}
