/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientregistration.policy.impl;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientregistration.ClientRegistrationContext;
import org.keycloak.services.clientregistration.ClientRegistrationProvider;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyException;

/**
 * Rejects Dynamic Client Registration requests that would change the realm-wide default-role
 * composite through the deprecated {@code defaultRoles} field.
 *
 */
public class DefaultRolesClientRegistrationPolicy implements ClientRegistrationPolicy {

    @Override
    public void beforeRegister(ClientRegistrationContext context) throws ClientRegistrationPolicyException {
        String[] defaultRoles = context.getClient().getDefaultRoles();
        if (defaultRoles == null || defaultRoles.length == 0) {
            return;
        }
        rejectNullDefaultRoles(defaultRoles);
        throw new ClientRegistrationPolicyException("Not permitted to set defaultRoles");
    }

    @Override
    public void afterRegister(ClientRegistrationContext context, ClientModel clientModel) {
    }

    @Override
    public void beforeUpdate(ClientRegistrationContext context, ClientModel clientModel) throws ClientRegistrationPolicyException {
        ClientRepresentation rep = context.getClient();
        if (rep.getDefaultRoles() == null) {
            return;
        }
        if (clientModel == null) {
            return;
        }

        rejectNullDefaultRoles(rep.getDefaultRoles());

        Set<String> current = getDefaultRoles(clientModel);
        Set<String> requested = Arrays.stream(rep.getDefaultRoles()).collect(Collectors.toSet());
        if (!current.equals(requested)) {
            throw new ClientRegistrationPolicyException("Not permitted to modify defaultRoles");
        }
    }

    private static void rejectNullDefaultRoles(String[] defaultRoles) throws ClientRegistrationPolicyException {
        if (Arrays.stream(defaultRoles).anyMatch(Objects::isNull)) {
            throw new ClientRegistrationPolicyException("Not permitted to set null defaultRoles");
        }
    }

    @Override
    public void afterUpdate(ClientRegistrationContext context, ClientModel clientModel) {
    }

    @Override
    public void beforeView(ClientRegistrationProvider provider, ClientModel clientModel) throws ClientRegistrationPolicyException {
    }

    @Override
    public void beforeDelete(ClientRegistrationProvider provider, ClientModel clientModel) throws ClientRegistrationPolicyException {
    }

    private static Set<String> getDefaultRoles(ClientModel client) {
        return client.getRealm().getDefaultRole().getCompositesStream()
                .filter(role -> role.isClientRole() && Objects.equals(role.getContainerId(), client.getId()))
                .map(RoleModel::getName)
                .collect(Collectors.toSet());
    }
}
