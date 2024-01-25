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

package org.keycloak.operator.controllers;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class WatchedResources {
    public static final String WATCHED_LABEL_VALUE_PREFIX = "watched-";
    public static final String KEYCLOAK_WATCHED_HASH_ANNOTATION_PREFIX = "operator.keycloak.org/watched-";
    public static final String KEYCLOAK_WATCHING_ANNOTATION_PREFIX = "operator.keycloak.org/watching-";
    public static final String KEYCLOAK_MISSING_ANNOTATION_PREFIX = "operator.keycloak.org/missing-";

    @Inject
    WatchedSecretsController watchedSecretsController;
    @Inject
    WatchedConfigMapController watchedConfigMapController;

    /**
     * @param deployment mutable resource being reconciled, it will be updated with
     *                   annotations
     */
    public void annotateDeployment(List<String> names, Class<?> type, Keycloak keycloakCR, StatefulSet deployment) {
        if (type == Secret.class) {
            watchedSecretsController.annotateDeployment(names, keycloakCR, deployment);
        } else if (type == ConfigMap.class) {
            watchedConfigMapController.annotateDeployment(names, keycloakCR, deployment);
        } else {
            throw new AssertionError(type + " is not a watched type");
        }
    }

    public EventSource[] getEventSources() {
        return new EventSource[] { watchedSecretsController.getEventSource(),
                watchedConfigMapController.getEventSource() };
    }

    public void addLabelsToWatched(StatefulSet deployment) {
        watchedSecretsController.addLabelsToWatched(deployment);
        watchedConfigMapController.addLabelsToWatched(deployment);
    }

}