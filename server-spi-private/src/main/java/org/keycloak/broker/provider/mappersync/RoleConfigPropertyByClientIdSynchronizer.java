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

package org.keycloak.broker.provider.mappersync;

import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.function.Consumer;

/**
 * Updates a role reference in a mapper config, when a client ID changes.
 *
 * @author <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 */
public class RoleConfigPropertyByClientIdSynchronizer
        extends AbstractConfigPropertySynchronizer<ClientModel.ClientIdChangeEvent> {

    public static final RoleConfigPropertyByClientIdSynchronizer INSTANCE =
            new RoleConfigPropertyByClientIdSynchronizer();

    private RoleConfigPropertyByClientIdSynchronizer() {
        // noop
    }

    @Override
    public Class<ClientModel.ClientIdChangeEvent> getEventClass() {
        return ClientModel.ClientIdChangeEvent.class;
    }

    @Override
    public RealmModel extractRealm(ClientModel.ClientIdChangeEvent event) {
        return event.getUpdatedClient().getRealm();
    }

    @Override
    public String getConfigPropertyName() {
        return ConfigConstants.ROLE;
    }

    @Override
    protected void updateConfigPropertyIfNecessary(ClientModel.ClientIdChangeEvent event, String currentPropertyValue,
            Consumer<String> propertyUpdater) {
        String[] parsedConfiguredRoleQualifier = KeycloakModelUtils.parseRole(currentPropertyValue);
        String configuredClientId = parsedConfiguredRoleQualifier[0];
        if (configuredClientId == null) {
            // a realm role is configured for the mapper, event is not relevant
            return;
        }

        String configuredRoleName = parsedConfiguredRoleQualifier[1];

        if (configuredClientId.equals(event.getPreviousClientId())) {
            String newRoleQualifier = KeycloakModelUtils.buildRoleQualifier(event.getNewClientId(), configuredRoleName);
            propertyUpdater.accept(newRoleQualifier);
        }
    }

}
