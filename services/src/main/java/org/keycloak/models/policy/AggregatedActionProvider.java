/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.policy;

import static org.keycloak.models.policy.AggregatedActionProviderFactory.CONFIG_ACTION_PROVIDER_IDS;

import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;

public class AggregatedActionProvider implements ResourceActionProvider {

    private final KeycloakSession session;
    private final ComponentModel actionModel;
    private final Logger log = Logger.getLogger(AggregatedActionProvider.class);

    public AggregatedActionProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.actionModel = model;
    }

    @Override
    public void close() {
    }

    @Override
    public void run(List<String> userIds) {
        getActionProviderIds().forEach(actionProviderId -> {
            ResourceActionProvider actionProvider = getActionProvider(actionProviderId);

            if (actionProvider == null) {
                throw new IllegalStateException("Could not find action provider with id: " + actionProviderId);
            }

            log.debugf("Running action provider '%s'", actionProviderId);
            actionProvider.run(userIds);
        });
    }

    @Override
    public boolean isRunnable() {
        return true;
    }

    private List<String> getActionProviderIds() {
        return Optional.ofNullable(actionModel.getConfig().getList(CONFIG_ACTION_PROVIDER_IDS)).orElse(List.of());
    }

    private ResourceActionProvider getActionProvider(String providerId) {
        ComponentFactory<?, ?> actionFactory = (ComponentFactory<?, ?>) session.getKeycloakSessionFactory()
                .getProviderFactory(ResourceActionProvider.class, providerId);
        return (ResourceActionProvider) actionFactory.create(session, actionModel);
    }
}
