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

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

import io.quarkus.logging.Log;
import org.keycloak.operator.Constants;
import org.keycloak.operator.Utils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.IngressSpec;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Optional;

import static org.keycloak.operator.crds.v2alpha1.CRDUtils.isTlsConfigured;

@KubernetesDependent(labelSelector = Constants.DEFAULT_LABELS_AS_STRING)
public class KeycloakIngressDependentResource extends CRUDKubernetesDependentResource<Ingress, Keycloak> {

    public static class EnabledCondition implements Condition<Ingress, Keycloak> {
        @Override
        public boolean isMet(DependentResource<Ingress, Keycloak> dependentResource, Keycloak primary,
                Context<Keycloak> context) {
            return isIngressEnabled(primary);
        }
    }

    public KeycloakIngressDependentResource() {
        super(Ingress.class);
    }

    public static boolean isIngressEnabled(Keycloak keycloak) {
        return Optional.ofNullable(keycloak.getSpec().getIngressSpec()).map(IngressSpec::isIngressEnabled).orElse(true);
    }

    @Override
    public Ingress desired(Keycloak keycloak, Context<Keycloak> context) {
        var annotations = new HashMap<String, String>();
        boolean tlsConfigured = isTlsConfigured(keycloak);
        var port = KeycloakServiceDependentResource.getServicePort(tlsConfigured, keycloak);

        if (tlsConfigured) {
            annotations.put("nginx.ingress.kubernetes.io/backend-protocol", "HTTPS");
            annotations.put("route.openshift.io/termination", "passthrough");
        } else {
            annotations.put("nginx.ingress.kubernetes.io/backend-protocol", "HTTP");
            annotations.put("route.openshift.io/termination", "edge");
        }

        var optionalSpec = Optional.ofNullable(keycloak.getSpec().getIngressSpec());
        optionalSpec.map(IngressSpec::getAnnotations).ifPresent(annotations::putAll);

        Ingress ingress = new IngressBuilder()
                .withNewMetadata()
                    .withName(getName(keycloak))
                    .withNamespace(keycloak.getMetadata().getNamespace())
                    .addToLabels(Utils.allInstanceLabels(keycloak))
                    .addToAnnotations(annotations)
                .endMetadata()
                .withNewSpec()
                    .withIngressClassName(optionalSpec.map(IngressSpec::getIngressClassName).orElse(null))
                    .withNewDefaultBackend()
                        .withNewService()
                            .withName(KeycloakServiceDependentResource.getServiceName(keycloak))
                            .withNewPort()
                                .withNumber(port)
                            .endPort()
                        .endService()
                    .endDefaultBackend()
                    .addNewRule()
                        .withNewHttp()
                            .addNewPath()
                                .withPathType("ImplementationSpecific")
                                .withNewBackend()
                                    .withNewService()
                                        .withName(KeycloakServiceDependentResource.getServiceName(keycloak))
                                        .withNewPort()
                                            .withNumber(port)
                                            .endPort()
                                    .endService()
                                .endBackend()
                            .endPath()
                        .endHttp()
                    .endRule()
                .endSpec()
                .build();

        final var hostnameSpec = keycloak.getSpec().getHostnameSpec();
        if (hostnameSpec != null && hostnameSpec.getHostname() != null) {
            String hostname = hostnameSpec.getHostname();

            try {
                hostname = new URL(hostname).getHost();
                Log.debug("Hostname is a URL, extracting host: " + hostname);
            }
            catch (MalformedURLException e) {
                Log.debug("Hostname is not a URL, using as is: " + hostname);
            }

            ingress.getSpec().getRules().get(0).setHost(hostname);
        }

        return ingress;
    }

    public static String getName(Keycloak keycloak) {
        return keycloak.getMetadata().getName() + Constants.KEYCLOAK_INGRESS_SUFFIX;
    }
}
