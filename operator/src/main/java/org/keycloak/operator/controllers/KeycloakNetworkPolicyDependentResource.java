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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyFluent;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPeer;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import org.jboss.logging.Logger;
import org.keycloak.operator.Constants;
import org.keycloak.operator.Utils;
import org.keycloak.operator.crds.v2alpha1.CRDUtils;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HttpManagementSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HttpSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.NetworkPolicySpec;

import static org.keycloak.operator.Constants.KEYCLOAK_JGROUPS_DATA_PORT;
import static org.keycloak.operator.Constants.KEYCLOAK_JGROUPS_FD_PORT;
import static org.keycloak.operator.Constants.KEYCLOAK_JGROUPS_PROTOCOL;
import static org.keycloak.operator.Constants.KEYCLOAK_SERVICE_PROTOCOL;

@KubernetesDependent(
        informer = @Informer(labelSelector = Constants.DEFAULT_LABELS_AS_STRING)
)
public class KeycloakNetworkPolicyDependentResource extends CRUDKubernetesDependentResource<NetworkPolicy, Keycloak> {

    private static final Logger LOG = Logger.getLogger(KeycloakNetworkPolicyDependentResource.class.getName());
    
    public KeycloakNetworkPolicyDependentResource() {
        super(NetworkPolicy.class);
    }

    public static class EnabledCondition implements Condition<NetworkPolicy, Keycloak> {
        @Override
        public boolean isMet(DependentResource<NetworkPolicy, Keycloak> dependentResource, Keycloak primary,
                             Context<Keycloak> context) {
            return NetworkPolicySpec.isNetworkPolicyEnabled(primary);
        }
    }

    @Override
    public NetworkPolicy desired(Keycloak primary, Context<Keycloak> context) {
        var builder = new NetworkPolicyBuilder();
        addMetadata(builder, primary);

        var specBuilder = builder.withNewSpec()
                .withPolicyTypes("Ingress");

        addPodSelector(specBuilder, primary);
        addApplicationPorts(specBuilder, primary);

        if (CRDUtils.isJGroupEnabled(primary)) {
            addJGroupsPorts(specBuilder, primary);
        }

        // see org.keycloak.quarkus.runtime.configuration.mappers.ManagementPropertyMappers.isManagementEnabled()
        if (CRDUtils.isManagementEndpointEnabled(primary)) {
            addManagementPorts(specBuilder, primary);
        }

        var np = specBuilder.endSpec().build();
        LOG.debugf("Create a Network Policy => %s", np);
        return np;
    }

    private static void addPodSelector(NetworkPolicyFluent<NetworkPolicyBuilder>.SpecNested<NetworkPolicyBuilder> builder, Keycloak keycloak) {
        builder.withNewPodSelector()
                .withMatchLabels(Utils.allInstanceLabels(keycloak))
                .endPodSelector();
    }

    private static void addApplicationPorts(NetworkPolicyFluent<NetworkPolicyBuilder>.SpecNested<NetworkPolicyBuilder> builder, Keycloak keycloak) {
        var tlsEnabled = CRDUtils.isTlsConfigured(keycloak);
        var httpEnabled = Optional.ofNullable(keycloak.getSpec())
                .map(KeycloakSpec::getHttpSpec)
                .map(HttpSpec::getHttpEnabled)
                .orElse(false);
        if (!tlsEnabled || httpEnabled) {
            addIngress(builder, HttpSpec.httpPort(keycloak), NetworkPolicySpec.httpRules(keycloak));
        }

        if (tlsEnabled) {
            addIngress(builder, HttpSpec.httpsPort(keycloak), NetworkPolicySpec.httpsRules(keycloak));
        }
    }

    private static void addManagementPorts(NetworkPolicyFluent<NetworkPolicyBuilder>.SpecNested<NetworkPolicyBuilder> builder, Keycloak keycloak) {
        addIngress(builder, HttpManagementSpec.managementPort(keycloak), NetworkPolicySpec.managementRules(keycloak));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static void addIngress(NetworkPolicyFluent<NetworkPolicyBuilder>.SpecNested<NetworkPolicyBuilder> builder,
                                   int port,
                                   Optional<List<NetworkPolicyPeer>> networkPolicyPeers) {
        var ingress = builder.addNewIngress();
        ingress.addNewPort()
                .withPort(new IntOrString(port))
                .withProtocol(KEYCLOAK_SERVICE_PROTOCOL)
                .endPort();

        networkPolicyPeers
                .filter(Predicate.not(Collection::isEmpty))
                .ifPresent(ingress::addAllToFrom);
        ingress.endIngress();
    }

    private static void addJGroupsPorts(NetworkPolicyFluent<NetworkPolicyBuilder>.SpecNested<NetworkPolicyBuilder> builder, Keycloak keycloak) {
        var ingressBuilder = builder.addNewIngress();
        ingressBuilder.addNewPort()
                .withPort(new IntOrString(KEYCLOAK_JGROUPS_DATA_PORT))
                .withProtocol(KEYCLOAK_JGROUPS_PROTOCOL)
                .endPort();
        ingressBuilder.addNewPort()
                .withPort(new IntOrString(KEYCLOAK_JGROUPS_FD_PORT))
                .withProtocol(KEYCLOAK_JGROUPS_PROTOCOL)
                .endPort();
        ingressBuilder.addNewFrom()
                .withNewPodSelector()
                .addToMatchLabels(Utils.allInstanceLabels(keycloak))
                .endPodSelector()
                .endFrom();
        ingressBuilder.endIngress();
    }

    private static void addMetadata(NetworkPolicyBuilder builder, Keycloak keycloak) {
        builder.withNewMetadata()
                .withName(NetworkPolicySpec.networkPolicyName(keycloak))
                .withNamespace(keycloak.getMetadata().getNamespace())
                .addToLabels(Utils.allInstanceLabels(keycloak))
                .endMetadata();
    }
}
