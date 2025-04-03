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

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetStatus;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Workflow;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import org.keycloak.operator.Config;
import org.keycloak.operator.ContextUtils;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImport;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImportStatus;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImportStatusBuilder;
import org.keycloak.operator.crds.v2alpha1.realmimport.KeycloakRealmImportStatusCondition;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Workflow(
explicitInvocation = true,
dependents = {
    @Dependent(type = KeycloakRealmImportJobDependentResource.class, dependsOn = KeycloakRealmImportSecretDependentResource.DEPENDENT_NAME),
    @Dependent(type = KeycloakRealmImportSecretDependentResource.class, name = KeycloakRealmImportSecretDependentResource.DEPENDENT_NAME)
})
public class KeycloakRealmImportController implements Reconciler<KeycloakRealmImport> {

    @Inject
    Config config;

    @Override
    public UpdateControl<KeycloakRealmImport> reconcile(KeycloakRealmImport realm, Context<KeycloakRealmImport> context) {
        String realmName = realm.getMetadata().getName();
        String realmNamespace = realm.getMetadata().getNamespace();

        Log.infof("--- Reconciling Keycloak Realm: %s in namespace: %s", realmName, realmNamespace);

        var statusBuilder = new KeycloakRealmImportStatusBuilder();

        Job existingJob = context.getSecondaryResource(Job.class).orElse(null);
        StatefulSet existingDeployment = context.getClient().resources(StatefulSet.class).inNamespace(realm.getMetadata().getNamespace())
                .withName(realm.getSpec().getKeycloakCRName()).get();

        if (existingDeployment != null) {
            ContextUtils.storeOperatorConfig(context, config);
            ContextUtils.storeCurrentStatefulSet(context, existingDeployment);
            if (getReadyReplicas(existingDeployment) > 0) {
                context.managedWorkflowAndDependentResourceContext().reconcileManagedWorkflow();
            }
        }

        updateStatus(statusBuilder, realm, existingJob, existingDeployment, context.getClient());

        var status = statusBuilder.build();

        Log.info("--- Realm reconciliation finished successfully");

        UpdateControl<KeycloakRealmImport> updateControl;
        if (status.equals(realm.getStatus())) {
            updateControl = UpdateControl.noUpdate();
        } else {
            realm.setStatus(status);
            updateControl = UpdateControl.patchStatus(realm);
        }

        if (status
                .getConditions()
                .stream()
                .anyMatch(c -> c.getType().equals(KeycloakRealmImportStatusCondition.DONE) && !Boolean.TRUE.equals(c.getStatus()))) {
            updateControl.rescheduleAfter(10, TimeUnit.SECONDS);
        }

        return updateControl;
    }

    @Override
    public ErrorStatusUpdateControl<KeycloakRealmImport> updateErrorStatus(KeycloakRealmImport realm, Context<KeycloakRealmImport> context, Exception e) {
        Log.debug("--- Error reconciling", e);
        KeycloakRealmImportStatus status = new KeycloakRealmImportStatusBuilder()
                .addErrorMessage("Error performing operations:\n" + e.getMessage())
                .build();

        realm.setStatus(status);
        return ErrorStatusUpdateControl.patchStatus(realm);
    }

    public void updateStatus(KeycloakRealmImportStatusBuilder status, KeycloakRealmImport realmCR, Job existingJob, StatefulSet existingDeployment, KubernetesClient client) {
        if (existingDeployment == null) {
            status.addErrorMessage("No existing Deployment found, waiting for it to be created");
            return;
        }

        if (getReadyReplicas(existingDeployment) < 1) {
            status.addErrorMessage("Deployment not yet ready, waiting for it to be ready");
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
                    rollingRestart(realmCR, client); // could be based upon a hash annotation on the deployment instead
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

    private Integer getReadyReplicas(StatefulSet existingDeployment) {
        return Optional.ofNullable(existingDeployment.getStatus()).map(StatefulSetStatus::getReadyReplicas).orElse(0);
    }

    private void rollingRestart(KeycloakRealmImport realmCR, KubernetesClient client) {
        client.apps().statefulSets()
                .inNamespace(realmCR.getMetadata().getNamespace())
                .withName(realmCR.getSpec().getKeycloakCRName())
                .rolling().restart();
    }

}
