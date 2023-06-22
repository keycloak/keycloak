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

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;

import org.keycloak.operator.Constants;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import static io.javaoperatorsdk.operator.api.reconciler.Constants.WATCH_CURRENT_NAMESPACE;

@ApplicationScoped
@ControllerConfiguration(namespaces = WATCH_CURRENT_NAMESPACE, labelSelector = Constants.KEYCLOAK_COMPONENT_LABEL + "=" + WatchedSecrets.WATCHED_SECRETS_LABEL_VALUE)
public class WatchedSecretsController implements Reconciler<Secret>, EventSourceInitializer<Secret> {

    @Inject
    KubernetesClient client;

    @Inject
    WatchedSecretsStatefulSetController watchedSecretsStatefulSetController;

    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<Secret> context) {
        watchedSecretsStatefulSetController.setSecrets(context.getPrimaryCache());
        return Map.of();
    }

    @Override
    public UpdateControl<Secret> reconcile(Secret resource, Context<Secret> context) throws Exception {
        return watchedSecretsStatefulSetController.reconcileSecret(resource, context);
    }

}
