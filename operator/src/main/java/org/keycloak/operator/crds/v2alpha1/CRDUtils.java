/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.operator.crds.v2alpha1;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.FeatureSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HttpSpec;

import com.fasterxml.jackson.databind.JsonNode;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.javaoperatorsdk.operator.api.reconciler.Context;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class CRDUtils {
    private static final String HEALTH_ENABLED = "health-enabled";
    public static final String HTTP_MANAGEMENT_HEALTH_ENABLED = "http-management-health-enabled";
    public static final String METRICS_ENABLED = "metrics-enabled";
    public static final String LEGACY_MANAGEMENT_ENABLED = "legacy-observability-interface";

    public static boolean isTlsConfigured(Keycloak keycloakCR) {
        var tlsSecret = keycloakSpecOf(keycloakCR).map(KeycloakSpec::getHttpSpec).map(HttpSpec::getTlsSecret);
        return tlsSecret.isPresent() && !tlsSecret.get().trim().isEmpty();
    }

    public static boolean isJGroupEnabled(Keycloak keycloak) {
        // If multi-site or clusterless are present, JGroups is not enabled.
        return CRDUtils.keycloakSpecOf(keycloak)
                .map(KeycloakSpec::getFeatureSpec)
                .map(FeatureSpec::getEnabledFeatures)
                .filter(features -> features.contains("multi-site") || features.contains("clusterless"))
                .isEmpty();
    }

    public static boolean isManagementEndpointEnabled(Keycloak keycloak) {
        var options = configuredOptions(keycloak);
        // Legacy management enabled
        if (Boolean.parseBoolean(options.get(LEGACY_MANAGEMENT_ENABLED))) {
            return false;
        }

        return Boolean.parseBoolean(options.get(METRICS_ENABLED)) || (Boolean.parseBoolean(options.get(HEALTH_ENABLED))
                && Boolean.parseBoolean(options.getOrDefault(HTTP_MANAGEMENT_HEALTH_ENABLED, Boolean.toString(true))));
    }

    public static Map<String, String> configuredOptions(Keycloak keycloak) {
        Map<String, String> options = new HashMap<>();
        // add default options
        Constants.DEFAULT_DIST_CONFIG_LIST
              .forEach(valueOrSecret -> options.put(valueOrSecret.getName(), valueOrSecret.getValue()));
        // overwrite the configured ones
        keycloakSpecOf(keycloak)
              .map(KeycloakSpec::getAdditionalOptions)
              .stream()
              .flatMap(Collection::stream)
              .forEach(valueOrSecret -> options.put(valueOrSecret.getName(), valueOrSecret.getValue()));
        return options;
    }

    public static Optional<KeycloakSpec> keycloakSpecOf(Keycloak keycloak) {
        return Optional.ofNullable(keycloak)
                .map(Keycloak::getSpec);
    }

    public static Optional<Container> firstContainerOf(StatefulSet statefulSet) {
        return Optional.ofNullable(statefulSet)
                .map(StatefulSet::getSpec)
                .map(StatefulSetSpec::getTemplate)
                .map(PodTemplateSpec::getSpec)
                .map(PodSpec::getContainers)
                .filter(Predicate.not(List::isEmpty))
                .map(containers -> containers.get(0));
    }

    public static <T> JsonNode toJsonNode(T value, Context<Keycloak> context) {
        final var kubernetesSerialization = context.getClient().getKubernetesSerialization();
        return kubernetesSerialization.convertValue(value, JsonNode.class);
    }

    public static Optional<Boolean> fetchIsRecreateUpdate(StatefulSet statefulSet) {
        var value = statefulSet.getMetadata().getAnnotations().get(Constants.KEYCLOAK_RECREATE_UPDATE_ANNOTATION);
        return Optional.ofNullable(value).map(Boolean::parseBoolean);
    }

    public static Optional<String> findUpdateReason(StatefulSet statefulSet) {
        return Optional.ofNullable(statefulSet.getMetadata().getAnnotations().get(Constants.KEYCLOAK_UPDATE_REASON_ANNOTATION));
    }

    public static Optional<String> getRevision(StatefulSet statefulSet) {
        return Optional.ofNullable(statefulSet)
                .map(StatefulSet::getMetadata)
                .map(ObjectMeta::getAnnotations)
                .map(annotations -> annotations.get(Constants.KEYCLOAK_UPDATE_REVISION_ANNOTATION));
    }

    public static Optional<String> getUpdateHash(StatefulSet statefulSet) {
        return Optional.ofNullable(statefulSet)
                .map(StatefulSet::getMetadata)
                .map(ObjectMeta::getAnnotations)
                .map(annotations -> annotations.get(Constants.KEYCLOAK_UPDATE_HASH_ANNOTATION));
    }
}
