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
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * Updates a role reference in a mapper config, when a client ID changes.
 *
 * @author <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 */
public class RoleConfigPropertyByClientIdSynchronizer implements ConfigSynchronizer<ClientModel.ClientIdChangeEvent> {

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
    public void handleEvent(ClientModel.ClientIdChangeEvent event) {
        // find all mappers that have a role config property that maps to a role associated with the changed client.
        event.getKeycloakSession().identityProviders().getMappersStream(Map.of(ConfigConstants.ROLE, event.getPreviousClientId() + ".*"), null, null)
                .forEach(idpMapper -> {
                    String currentRoleValue = idpMapper.getConfig().get(ConfigConstants.ROLE);
                    String configuredRoleName = KeycloakModelUtils.parseRole(currentRoleValue)[1];
                    String newRoleValue = KeycloakModelUtils.buildRoleQualifier(event.getNewClientId(), configuredRoleName);
                    idpMapper.getConfig().put(ConfigConstants.ROLE, newRoleValue);
                    logEventProcessed(ConfigConstants.ROLE, currentRoleValue, newRoleValue, event.getUpdatedClient().getRealm().getName(),
                            idpMapper.getName(), idpMapper.getIdentityProviderAlias());
                    event.getKeycloakSession().identityProviders().updateMapper(idpMapper);
                });

        clientIdChange(event).ifPresent(change -> rewriteRoleReferences(event.getKeycloakSession(), change));
    }

    /**
     * @param previousClientId the client ID before the change
     * @param newClientId      the client ID after the change, different from {@code previousClientId}
     */
    private record ClientIdChange(RealmModel realm, String previousClientId, String newClientId) {
    }

    /**
     * The change of the client ID; empty when the update did not set a new client ID.
     */
    private static Optional<ClientIdChange> clientIdChange(ClientModel.ClientIdChangeEvent event) {
        return Optional.ofNullable(event.getNewClientId())
                .filter(newClientId -> !newClientId.equals(event.getPreviousClientId()))
                .map(newClientId -> new ClientIdChange(event.getUpdatedClient().getRealm(), event.getPreviousClientId(), newClientId));
    }

    private void rewriteRoleReferences(KeycloakSession session, ClientIdChange change) {
        GrantableRoleReferences.rewrite(session, change.realm(), reference -> rename(change, reference), this);
    }

    private static RoleReferenceRename rename(ClientIdChange change, String reference) {
        return roleOfRenamedClient(change, reference)
                .map(roleName -> renamedReference(change, roleName))
                .orElse(RoleReferenceRename.UNAFFECTED);
    }

    /**
     * The reference to the role under the new client ID, unless that spelling names a role of a different client.
     */
    private static RoleReferenceRename renamedReference(ClientIdChange change, String roleName) {
        String newReference = KeycloakModelUtils.buildRoleQualifier(change.newClientId(), roleName);
        return namesRenamedClientRole(change, newReference, roleName)
                ? new RoleReferenceRename.Renamed(newReference)
                : new RoleReferenceRename.Ambiguous(newReference);
    }

    private static boolean namesRenamedClientRole(ClientIdChange change, String reference, String roleName) {
        RoleReference renamedClientRole = new RoleReference(Optional.of(change.newClientId()), roleName);
        return RoleReference.resolve(reference, clientId -> existsAfterRename(change, clientId))
                .equals(Optional.of(renamedClientRole));
    }

    /**
     * The role name that {@code reference} named under the renamed client before the rename; empty when it named a role
     * of another client or a realm role.
     */
    private static Optional<String> roleOfRenamedClient(ClientIdChange change, String reference) {
        return RoleReference.resolve(reference, clientId -> existedBeforeRename(change, clientId))
                .filter(role -> role.clientId().equals(Optional.ofNullable(change.previousClientId())))
                .map(RoleReference::roleName);
    }

    private static boolean existedBeforeRename(ClientIdChange change, String clientId) {
        return clientId.equals(change.previousClientId())
                || (!clientId.equals(change.newClientId()) && isOtherClient(change, clientId));
    }

    private static boolean existsAfterRename(ClientIdChange change, String clientId) {
        return clientId.equals(change.newClientId())
                || (!clientId.equals(change.previousClientId()) && isOtherClient(change, clientId));
    }

    private static boolean isOtherClient(ClientIdChange change, String clientId) {
        return change.realm().getClientByClientId(clientId) != null;
    }
}
