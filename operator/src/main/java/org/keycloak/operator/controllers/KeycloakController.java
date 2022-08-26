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

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
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
import org.keycloak.operator.Config;
import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatus;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusBuilder;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusCondition;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.javaoperatorsdk.operator.api.reconciler.Constants.NO_FINALIZER;
import static io.javaoperatorsdk.operator.api.reconciler.Constants.WATCH_CURRENT_NAMESPACE;

@ControllerConfiguration(namespaces = WATCH_CURRENT_NAMESPACE, finalizerName = NO_FINALIZER)
public class KeycloakController implements Reconciler<Keycloak>, EventSourceInitializer<Keycloak>, ErrorStatusHandler<Keycloak> {

    @Inject
    KubernetesClient client;

    @Inject
    Config config;

    @Override
    public List<EventSource> prepareEventSources(EventSourceContext<Keycloak> context) {
        String namespace = context.getConfigurationService().getClientConfiguration().getNamespace();

        SharedIndexInformer<StatefulSet> deploymentInformer =
                client.apps().statefulSets().inNamespace(namespace)
                        .withLabels(Constants.DEFAULT_LABELS)
                        .runnableInformer(0);

        SharedIndexInformer<Service> servicesInformer =
                client.services().inNamespace(namespace)
                        .withLabels(Constants.DEFAULT_LABELS)
                        .runnableInformer(0);

        SharedIndexInformer<Ingress> ingressesInformer =
                client.network().v1().ingresses().inNamespace(namespace)
                        .withLabels(Constants.DEFAULT_LABELS)
                        .runnableInformer(0);

        EventSource deploymentEvent = new InformerEventSource<>(deploymentInformer, Mappers.fromOwnerReference());
        EventSource servicesEvent = new InformerEventSource<>(servicesInformer, Mappers.fromOwnerReference());
        EventSource ingressesEvent = new InformerEventSource<>(ingressesInformer, Mappers.fromOwnerReference());

        return List.of(deploymentEvent,
                servicesEvent,
                ingressesEvent,
                WatchedSecretsStore.getStoreEventSource(client, namespace),
                WatchedSecretsStore.getWatchedSecretsEventSource(client, namespace));
    }

    @Override
    public UpdateControl<Keycloak> reconcile(Keycloak kc, Context context) {
        String kcName = kc.getMetadata().getName();
        String namespace = kc.getMetadata().getNamespace();

        Log.infof("--- Reconciling Keycloak: %s in namespace: %s", kcName, namespace);

        var statusBuilder = new KeycloakStatusBuilder();

        var kcAdminSecret = new KeycloakAdminSecret(client, kc);
        kcAdminSecret.createOrUpdateReconciled();

        // TODO use caches in secondary resources; this is a workaround for https://github.com/java-operator-sdk/java-operator-sdk/issues/830
        // KeycloakDeployment deployment = new KeycloakDeployment(client, config, kc, context.getSecondaryResource(Deployment.class).orElse(null));
        var kcDeployment = new KeycloakDeployment(client, config, kc, null, kcAdminSecret.getName());
        var watchedSecrets = new WatchedSecretsStore(kcDeployment.getConfigSecretsNames(), client, kc);
        kcDeployment.createOrUpdateReconciled();
        if (watchedSecrets.changesDetected()) {
            Log.info("Config Secrets modified, restarting deployment");
            kcDeployment.rollingRestart();
        }
        kcDeployment.updateStatus(statusBuilder);
        watchedSecrets.createOrUpdateReconciled();

        var kcService = new KeycloakService(client, kc);
        kcService.updateStatus(statusBuilder);
        kcService.createOrUpdateReconciled();
        var kcDiscoveryService = new KeycloakDiscoveryService(client, kc);
        kcDiscoveryService.updateStatus(statusBuilder);
        kcDiscoveryService.createOrUpdateReconciled();

        var kcIngress = new KeycloakIngress(client, kc);
        kcIngress.updateStatus(statusBuilder);
        kcIngress.createOrUpdateReconciled();

        var status = statusBuilder.build();

        Log.info("--- Reconciliation finished successfully");

        UpdateControl<Keycloak> updateControl;
        if (status.equals(kc.getStatus())) {
            updateControl = UpdateControl.noUpdate();
        }
        else {
            kc.setStatus(status);
            updateControl = UpdateControl.updateStatus(kc);
        }

        if (status
                .getConditions()
                .stream()
                .anyMatch(c -> c.getType().equals(KeycloakStatusCondition.READY) && !c.getStatus())) {
            updateControl.rescheduleAfter(10, TimeUnit.SECONDS);
        }

        return updateControl;
    }

    @Override
    public Optional<Keycloak> updateErrorStatus(Keycloak kc, RetryInfo retryInfo, RuntimeException e) {
        Log.error("--- Error reconciling", e);
        KeycloakStatus status = new KeycloakStatusBuilder()
                .addErrorMessage("Error performing operations:\n" + e.getMessage())
                .build();

        kc.setStatus(status);

        return Optional.of(kc);
    }
}
