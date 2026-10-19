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

import java.util.Map;
import java.util.Optional;

import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * Updates a role reference a in mapper config, when a role name changes.
 *
 * @author <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 */
public class RoleConfigPropertyByRoleNameSynchronizer implements ConfigSynchronizer<RoleModel.RoleNameChangeEvent> {

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
    public void handleEvent(RoleModel.RoleNameChangeEvent event) {
        // first find all mappers that have a role config property that maps exactly to the changed path.
        String currentRoleValue = KeycloakModelUtils.buildRoleQualifier(event.getClientId(), event.getPreviousName());
        event.getKeycloakSession().identityProviders().getMappersStream(Map.of(ConfigConstants.ROLE, currentRoleValue), null, null)
                .forEach(idpMapper -> {
                    String newRoleValue = KeycloakModelUtils.buildRoleQualifier(event.getClientId(), event.getNewName());
                    idpMapper.getConfig().put(ConfigConstants.ROLE, newRoleValue);
                    logEventProcessed(ConfigConstants.ROLE, currentRoleValue, newRoleValue, event.getRealm().getName(),
                            idpMapper.getName(), idpMapper.getIdentityProviderAlias());
                    event.getKeycloakSession().identityProviders().updateMapper(idpMapper);
                });

        GrantableRoleReferences.rewrite(event.getKeycloakSession(), event.getRealm(),
                reference -> rename(event, reference), this);
    }

    private static RoleReferenceRename rename(RoleModel.RoleNameChangeEvent event, String reference) {
        return namesRole(event, reference, event.getPreviousName()) ? renamedReference(event) : RoleReferenceRename.UNAFFECTED;
    }

    /**
     * The reference to the role under its new name, unless that spelling names a different role.
     */
    private static RoleReferenceRename renamedReference(RoleModel.RoleNameChangeEvent event) {
        String newReference = KeycloakModelUtils.buildRoleQualifier(event.getClientId(), event.getNewName());
        return namesRole(event, newReference, event.getNewName())
                ? new RoleReferenceRename.Renamed(newReference)
                : new RoleReferenceRename.Ambiguous(newReference);
    }

    /**
     * Whether {@code reference} names the role of the event's container called {@code roleName}; a role rename does not
     * change which client IDs exist.
     */
    private static boolean namesRole(RoleModel.RoleNameChangeEvent event, String reference, String roleName) {
        RoleReference role = new RoleReference(Optional.ofNullable(event.getClientId()), roleName);
        return RoleReference.resolve(reference, clientId -> event.getRealm().getClientByClientId(clientId) != null)
                .equals(Optional.of(role));
    }
}
