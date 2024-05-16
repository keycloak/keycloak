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

import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateWaiting;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.readiness.Readiness;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.Mappers;
import io.quarkus.logging.Log;

import org.keycloak.common.util.CollectionUtil;
import org.keycloak.operator.Config;
import org.keycloak.operator.Constants;
import org.keycloak.operator.Utils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatus;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusAggregator;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HostnameSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HostnameSpecBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

@ControllerConfiguration(
    dependents = {
        @Dependent(type = KeycloakDeploymentDependentResource.class),
        @Dependent(type = KeycloakAdminSecretDependentResource.class),
        @Dependent(type = KeycloakIngressDependentResource.class, reconcilePrecondition = KeycloakIngressDependentResource.EnabledCondition.class),
        @Dependent(type = KeycloakServiceDependentResource.class, useEventSourceWithName = "serviceSource"),
        @Dependent(type = KeycloakDiscoveryServiceDependentResource.class, useEventSourceWithName = "serviceSource")
    })
public class KeycloakController implements Reconciler<Keycloak>, EventSourceInitializer<Keycloak>, ErrorStatusHandler<Keycloak> {

    public static final String OPENSHIFT_DEFAULT = "openshift-default";

    @Inject
    Config config;

    @Inject
    WatchedResources watchedResources;

    @Inject
    KeycloakDistConfigurator distConfigurator;

    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<Keycloak> context) {
        var namespaces = context.getControllerConfiguration().getNamespaces();

        InformerConfiguration<Service> servicesIC = InformerConfiguration
                .from(Service.class)
                .withLabelSelector(Constants.DEFAULT_LABELS_AS_STRING)
                .withNamespaces(namespaces)
                .withSecondaryToPrimaryMapper(Mappers.fromOwnerReference())
                .build();

        EventSource servicesEvent = new InformerEventSource<>(servicesIC, context);

        Map<String, EventSource> sources = new HashMap<>();
        sources.put("serviceSource", servicesEvent);
        return sources;
    }

    @Override
    public UpdateControl<Keycloak> reconcile(Keycloak kc, Context<Keycloak> context) {
        String kcName = kc.getMetadata().getName();
        String namespace = kc.getMetadata().getNamespace();

        Log.debugf("--- Reconciling Keycloak: %s in namespace: %s", kcName, namespace);

        boolean modifiedSpec = false;
        if (kc.getSpec().getInstances() == null) {
            // explicitly set defaults - and let another reconciliation happen
            // this avoids ensuring unintentional modifications have not been made to the cr
            kc.getSpec().setInstances(1);
            modifiedSpec = true;
        }
        if (kc.getSpec().getIngressSpec() != null && kc.getSpec().getIngressSpec().isIngressEnabled()
                && OPENSHIFT_DEFAULT.equals(kc.getSpec().getIngressSpec().getIngressClassName())
                && Optional.ofNullable(kc.getSpec().getHostnameSpec()).map(HostnameSpec::getHostname).isEmpty()) {
            var optionalHostname = generateOpenshiftHostname(kc, context);
            if (optionalHostname.isPresent()) {
                kc.getSpec().setHostnameSpec(new HostnameSpecBuilder(kc.getSpec().getHostnameSpec())
                        .withHostname(optionalHostname.get()).build());
                modifiedSpec = true;
            }
        }

        if (modifiedSpec) {
            return UpdateControl.updateResource(kc);
        }

        var statusAggregator = new KeycloakStatusAggregator(kc.getStatus(), kc.getMetadata().getGeneration());

        updateStatus(kc, context.getSecondaryResource(StatefulSet.class).orElse(null), statusAggregator, context);
        var status = statusAggregator.build();

        Log.debug("--- Reconciliation finished successfully");

        UpdateControl<Keycloak> updateControl;
        if (status.equals(kc.getStatus())) {
            updateControl = UpdateControl.noUpdate();
        }
        else {
            kc.setStatus(status);
            updateControl = UpdateControl.updateStatus(kc);
        }

        var statefulSet = context.getSecondaryResource(StatefulSet.class);

        if (!status.isReady() || statefulSet.filter(watchedResources::hasMissing).isPresent()) {
            updateControl.rescheduleAfter(10, TimeUnit.SECONDS);
        } else if (statefulSet.filter(watchedResources::isWatching).isPresent()) {
            updateControl.rescheduleAfter(config.keycloak().pollIntervalSeconds(), TimeUnit.SECONDS);
        }

        return updateControl;
    }

    @Override
    public ErrorStatusUpdateControl<Keycloak> updateErrorStatus(Keycloak kc, Context<Keycloak> context, Exception e) {
        Log.error("--- Error reconciling", e);
        KeycloakStatus status = new KeycloakStatusAggregator(kc.getStatus(), kc.getMetadata().getGeneration())
                .addErrorMessage("Error performing operations:\n" + e.getMessage())
                .build();

        kc.setStatus(status);

        return ErrorStatusUpdateControl.updateStatus(kc);
    }

    public static Optional<String> generateOpenshiftHostname(Keycloak keycloak, Context<Keycloak> context) {
        return getAppsDomain(context).map(s -> KubernetesResourceUtil.sanitizeName(String.format("%s-%s",
                KeycloakIngressDependentResource.getName(keycloak), keycloak.getMetadata().getNamespace())) + "." + s);
    }

    public static Optional<String> getAppsDomain(Context<Keycloak> context) {
        return Optional
                .ofNullable(context.getClient().resources(io.fabric8.openshift.api.model.config.v1.Ingress.class)
                        .withName("cluster").get())
                .map(i -> Optional.ofNullable(i.getSpec().getAppsDomain()).orElse(i.getSpec().getDomain()));
    }

    public void updateStatus(Keycloak keycloakCR, StatefulSet existingDeployment, KeycloakStatusAggregator status, Context<Keycloak> context) {
        status.apply(b -> b.withSelector(Utils.toSelectorString(Utils.allInstanceLabels(keycloakCR))));
        validatePodTemplate(keycloakCR, status);
        if (existingDeployment == null) {
            status.addNotReadyMessage("No existing StatefulSet found, waiting for creating a new one");
            return;
        }

        if (existingDeployment.getStatus() == null) {
            status.addNotReadyMessage("Waiting for deployment status");
        } else {
            status.apply(b -> b.withInstances(existingDeployment.getStatus().getReadyReplicas()));
            if (Optional.ofNullable(existingDeployment.getStatus().getReadyReplicas()).orElse(0) < keycloakCR.getSpec().getInstances()) {
                checkForPodErrors(status, keycloakCR, existingDeployment, context);
                status.addNotReadyMessage("Waiting for more replicas");
            }
        }

        if (Optional
                .ofNullable(existingDeployment.getMetadata().getAnnotations().get(Constants.KEYCLOAK_MIGRATING_ANNOTATION))
                .map(Boolean::valueOf).orElse(false)) {
            status.addNotReadyMessage("Performing Keycloak upgrade, scaling down the deployment");
        } else if (existingDeployment.getStatus() != null
                && existingDeployment.getStatus().getCurrentRevision() != null
                && existingDeployment.getStatus().getUpdateRevision() != null
                && !existingDeployment.getStatus().getCurrentRevision().equals(existingDeployment.getStatus().getUpdateRevision())) {
            status.addRollingUpdateMessage("Rolling out deployment update");
        }

        distConfigurator.validateOptions(keycloakCR, status);
    }

    public void validatePodTemplate(Keycloak keycloakCR, KeycloakStatusAggregator status) {
        var spec = KeycloakDeploymentDependentResource.getPodTemplateSpec(keycloakCR);
        if (spec.isEmpty()) {
            return;
        }
        var overlayTemplate = spec.orElseThrow();

        if (overlayTemplate.getMetadata() != null) {
            if (overlayTemplate.getMetadata().getName() != null) {
                status.addWarningMessage("The name of the podTemplate cannot be modified");
            }
            if (overlayTemplate.getMetadata().getNamespace() != null) {
                status.addWarningMessage("The namespace of the podTemplate cannot be modified");
            }
        }

        Optional.ofNullable(overlayTemplate.getSpec()).map(PodSpec::getContainers).flatMap(l -> l.stream().findFirst())
                .ifPresent(container -> {
                    if (container.getName() != null) {
                        status.addWarningMessage("The name of the keycloak container cannot be modified");
                    }
                    if (container.getImage() != null) {
                        status.addWarningMessage(
                                "The image of the keycloak container cannot be modified using podTemplate");
                    }
                    if (container.getResources() != null) {
                        status.addWarningMessage("Resources requirements of the Keycloak container cannot be modified using podTemplate");
                    }
                });

        if (overlayTemplate.getSpec() != null &&
            CollectionUtil.isNotEmpty(overlayTemplate.getSpec().getImagePullSecrets())) {
            status.addWarningMessage("The imagePullSecrets of the keycloak container cannot be modified using podTemplate");
        }
    }

    private void checkForPodErrors(KeycloakStatusAggregator status, Keycloak keycloak, StatefulSet existingDeployment, Context<Keycloak> context) {
        context.getClient().pods().inNamespace(existingDeployment.getMetadata().getNamespace())
                .withLabel("controller-revision-hash", existingDeployment.getStatus().getUpdateRevision())
                .withLabels(Utils.allInstanceLabels(keycloak))
                .list().getItems().stream()
                .filter(p -> !Readiness.isPodReady(p)
                        && Optional.ofNullable(p.getStatus()).map(PodStatus::getContainerStatuses).isPresent())
                .sorted((p1, p2) -> p1.getMetadata().getName().compareTo(p2.getMetadata().getName()))
                .forEachOrdered(p -> {
                    Optional.of(p.getStatus()).map(s -> s.getContainerStatuses()).stream().flatMap(List::stream)
                            .filter(cs -> !Boolean.TRUE.equals(cs.getReady()))
                            .sorted((cs1, cs2) -> cs1.getName().compareTo(cs2.getName())).forEachOrdered(cs -> {
                                if (Optional.ofNullable(cs.getState()).map(ContainerState::getWaiting)
                                        .map(ContainerStateWaiting::getReason).map(String::toLowerCase)
                                        .filter(s -> s.contains("err") || s.equals("crashloopbackoff")).isPresent()) {
                                    Log.infof("Found unhealthy container on pod %s/%s: %s",
                                            p.getMetadata().getNamespace(), p.getMetadata().getName(),
                                            Serialization.asYaml(cs));
                                    status.addErrorMessage(
                                            String.format("Waiting for %s/%s due to %s: %s", p.getMetadata().getNamespace(),
                                                    p.getMetadata().getName(), cs.getState().getWaiting().getReason(),
                                                    cs.getState().getWaiting().getMessage()));
                                }
                            });
                });
    }

}
