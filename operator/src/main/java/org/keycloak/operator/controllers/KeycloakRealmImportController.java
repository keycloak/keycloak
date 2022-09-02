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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.RetryInfo;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.Mappers;
import io.quarkus.logging.Log;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImport;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImportStatus;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImportStatusBuilder;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImportStatusCondition;

import javax.inject.Inject;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.javaoperatorsdk.operator.api.reconciler.Constants.NO_FINALIZER;
import static io.javaoperatorsdk.operator.api.reconciler.Constants.WATCH_CURRENT_NAMESPACE;

@ControllerConfiguration(namespaces = WATCH_CURRENT_NAMESPACE, finalizerName = NO_FINALIZER)
public class KeycloakRealmImportController implements Reconciler<KeycloakRealmImport>, EventSourceInitializer<KeycloakRealmImport>, ErrorStatusHandler<KeycloakRealmImport> {

    @Inject
    KubernetesClient client;

    @Inject
    ObjectMapper jsonMapper;

    @Override
    public List<EventSource> prepareEventSources(EventSourceContext<KeycloakRealmImport> context) {
        SharedIndexInformer<Job> jobInformer =
                client.batch().v1().jobs().inNamespace(context.getConfigurationService().getClientConfiguration().getNamespace())
                        .withLabels(org.keycloak.operator.Constants.DEFAULT_LABELS)
                        .runnableInformer(0);

        return List.of(new InformerEventSource<>(jobInformer, Mappers.fromOwnerReference()));
    }

    @Override
    public UpdateControl<KeycloakRealmImport> reconcile(KeycloakRealmImport realm, Context context) {
        String realmName = realm.getMetadata().getName();
        String realmNamespace = realm.getMetadata().getNamespace();

        Log.infof("--- Reconciling Keycloak Realm: %s in namespace: %s", realmName, realmNamespace);

        var statusBuilder = new KeycloakRealmImportStatusBuilder();

        var realmImportSecret = new KeycloakRealmImportSecret(client, realm, jsonMapper);
        realmImportSecret.createOrUpdateReconciled();

        var realmImportJob = new KeycloakRealmImportJob(client, realm, realmImportSecret.getSecretName());
        realmImportJob.createOrUpdateReconciled();
        realmImportJob.updateStatus(statusBuilder);

        var status = statusBuilder.build();

        Log.info("--- Realm reconciliation finished successfully");

        UpdateControl<KeycloakRealmImport> updateControl;
        if (status.equals(realm.getStatus())) {
            updateControl = UpdateControl.noUpdate();
        } else {
            realm.setStatus(status);
            updateControl = UpdateControl.updateStatus(realm);
        }

        if (status
                .getConditions()
                .stream()
                .anyMatch(c -> c.getType().equals(KeycloakRealmImportStatusCondition.DONE) && !c.getStatus())) {
            updateControl.rescheduleAfter(10, TimeUnit.SECONDS);
        }

        return updateControl;
    }

    @Override
    public Optional<KeycloakRealmImport> updateErrorStatus(KeycloakRealmImport realm, RetryInfo retryInfo, RuntimeException e) {
        Log.error("--- Error reconciling", e);
        KeycloakRealmImportStatus status = new KeycloakRealmImportStatusBuilder()
                .addErrorMessage("Error performing operations:\n" + e.getMessage())
                .build();

        realm.setStatus(status);
        return Optional.of(realm);
    }
}
