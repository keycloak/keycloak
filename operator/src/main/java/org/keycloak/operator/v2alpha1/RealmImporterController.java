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
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.Constants;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import org.jboss.logging.Logger;
import org.keycloak.operator.v2alpha1.crds.Keycloak;
import org.keycloak.operator.v2alpha1.crds.realm.*;
import org.keycloak.representations.idm.RealmRepresentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ControllerConfiguration(namespaces = Constants.WATCH_CURRENT_NAMESPACE, finalizerName = Constants.NO_FINALIZER)
public class RealmImporterController implements Reconciler<RealmImporter>, ErrorStatusHandler<RealmImporter> {

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
        var keycloakRef = realm.getSpec().getKeycloakCRName();

        var kc = kcDeployment.getKeycloakDeployment(realm.getSpec().getKeycloakCRName(), realm.getMetadata().getNamespace());

        // Write the realm representation secret
        String content = "";
        try {
            content = jsonMapper.writeValueAsString(realm.getSpec().getRealm());
        } catch (JsonProcessingException cause) {
            throw new RuntimeException("Failed to read the Realm Representation", cause);
        }

        var realmName = realm.getSpec().getRealm().getRealm();
        updateRealmSecret(kc, realmName, content);

        // run the import job
        spawnImportJob(kc, realmName);

        return UpdateControl.noUpdate();
    }

    private String getSecretName(Deployment deployment) {
        return deployment.getMetadata().getName() + "-realms";
    }

    private void updateRealmSecret(Deployment deployment, String realmName, String content) {
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
            updatedData.put(fileName, content);

            secret.setStringData(updatedData);

            secretSelector.patch(secret);
        } else {
            logger.info("Creating Secret " + secretName);
            secretSelector.create(
                    new SecretBuilder()
                            .withNewMetadata()
                            .withName(secretName)
                            .withNamespace(namespace)
                            .withOwnerReferences(deployment.getMetadata().getOwnerReferences())
                            .endMetadata()
                            .addToStringData(fileName, content)
                            .build()
            );
        }
    }

    private void spawnImportJob(Deployment deployment, String realmName) {
        var name = "realm-" + realmName + "-importer";
        var keycloakTemplate = deployment.getSpec().getTemplate();
        var keycloakContainer = keycloakTemplate.getSpec().getContainers().get(0);

        var importMntPath = "/mnt/realm-import/";

        var javaProperties = keycloakContainer
                .getArgs()
                .stream()
                .filter((p) -> p.startsWith("-D"))
                .collect(Collectors.toList());

        var commandArgs = new ArrayList<String>();
        commandArgs.add("import");
        commandArgs.add("--file='" + importMntPath + realmName + "-realm.json");
        commandArgs.add("--override=true");

        commandArgs.addAll(javaProperties);

        keycloakContainer
                .setArgs(commandArgs);
        var volumeMounts = new ArrayList<VolumeMount>();
        volumeMounts.add(
                new VolumeMountBuilder()
                .withName("realm-import")
                .withReadOnly(true)
                .withMountPath(importMntPath)
                .build()
        );
        keycloakContainer.setVolumeMounts(volumeMounts);

        var secretVolume = new VolumeBuilder()
                .withSecret(new SecretVolumeSourceBuilder()
                        .withSecretName(getSecretName(deployment))
                        .build())
                .build();

        // TODO: add selector labels and use the informer on Jobs
        new JobBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(deployment.getMetadata().getNamespace())
                .endMetadata()
                .withNewSpec()
                .withNewTemplate()
                .withNewSpec()
                .withContainers(keycloakContainer)
                .addToVolumes(secretVolume)
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }

    @Override
    public DeleteControl cleanup(RealmImporter realm, Context context) {
        // need to cleanup ended jobs? or simply use OwnerReferences ...
        return DeleteControl.defaultDelete();
    }

    @Override
    public Optional<RealmImporter> updateErrorStatus(RealmImporter realmImporter, RetryInfo retryInfo, RuntimeException e) {
        realmImporter.getStatus().setMessage("Error: " + e.getMessage());
        return Optional.of(realmImporter);
    }
}
