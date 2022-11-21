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
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.function.Consumer;

/**
 * Updates a role reference a in mapper config, when a role name changes.
 *
 * @author <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 */
public class RoleConfigPropertyByRoleNameSynchronizer
        extends AbstractConfigPropertySynchronizer<RoleModel.RoleNameChangeEvent> {

    public static final RoleConfigPropertyByRoleNameSynchronizer INSTANCE =
            new RoleConfigPropertyByRoleNameSynchronizer();

    private RoleConfigPropertyByRoleNameSynchronizer() {
        // noop
    }

    @Override
    public Class<RoleModel.RoleNameChangeEvent> getEventClass() {
        return RoleModel.RoleNameChangeEvent.class;
    }

    @Override
    public RealmModel extractRealm(RoleModel.RoleNameChangeEvent event) {
        return event.getRealm();
    }

    @Override
    public String getConfigPropertyName() {
        return ConfigConstants.ROLE;
    }

    @Override
    protected void updateConfigPropertyIfNecessary(RoleModel.RoleNameChangeEvent event, String currentPropertyValue,
            Consumer<String> propertyUpdater) {

        String previousRoleQualifier =
                KeycloakModelUtils.buildRoleQualifier(event.getClientId(), event.getPreviousName());
        if (previousRoleQualifier.equals(currentPropertyValue)) {
            String newRoleQualifier = KeycloakModelUtils.buildRoleQualifier(event.getClientId(), event.getNewName());
            propertyUpdater.accept(newRoleQualifier);
        }
    }

}
