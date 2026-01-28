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
package org.keycloak.broker.oidc.mappers;

import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.ConfigConstants;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import org.jboss.logging.Logger;

/**
 * Abstract class that handles the logic for importing and updating brokered users for all mappers that map an OIDC
 * claim into a {@code Keycloak} role.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>,
 * <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 */
public abstract class AbstractClaimToRoleMapper extends AbstractClaimMapper {

    private static final Logger LOG = Logger.getLogger(AbstractClaimToRoleMapper.class);

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        RoleModel role = getRole(realm, mapperModel);
        if (role == null) {
            return;
        }

        if (applies(mapperModel, context)) {
            user.grantRole(role);
        }
    }

    @Override
    public void updateBrokeredUserLegacy(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        RoleModel role = getRole(realm, mapperModel);
        if (role == null) {
            return;
        }

        if (!applies(mapperModel, context)) {
            user.deleteRoleMapping(role);
        }
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        RoleModel role = getRole(realm, mapperModel);
        if (role == null) {
            return;
        }

        String roleName = mapperModel.getConfig().get(ConfigConstants.ROLE);
        // KEYCLOAK-8730 if a previous mapper has already granted the same role, skip the checks so we don't accidentally remove a valid role.
        if (!context.hasMapperGrantedRole(roleName)) {
            if (applies(mapperModel, context)) {
                context.addMapperGrantedRole(roleName);
                user.grantRole(role);
            } else {
                user.deleteRoleMapping(role);
            }
        }
    }


    /**
     * This method must be implemented by subclasses and they must return {@code true} if their mapping can be applied
     * (i.e. user has the OIDC claim that should be mapped) or {@code false} otherwise.
     *
     * @param mapperModel a reference to the {@link IdentityProviderMapperModel}.
     * @param context a reference to the {@link BrokeredIdentityContext}.
     * @return {@code true} if the mapping can be applied or {@code false} otherwise.*
     */
    protected abstract boolean applies(final IdentityProviderMapperModel mapperModel, final BrokeredIdentityContext context);

    /**
     * Obtains the {@link RoleModel} corresponding the role configured in the specified
     * {@link IdentityProviderMapperModel}.
     * If the role doesn't correspond to one of the realm's client roles or to one of the realm's roles, this method
     * returns {@code null}.
     *
     * @param realm a reference to the realm.
     * @param mapperModel a reference to the {@link IdentityProviderMapperModel} containing the configured role.
     * @return the {@link RoleModel} that corresponds to the mapper model role; {@code null}, when role was not found
     */
    private RoleModel getRole(final RealmModel realm, final IdentityProviderMapperModel mapperModel) {
        String roleName = mapperModel.getConfig().get(ConfigConstants.ROLE);
        RoleModel role = KeycloakModelUtils.getRoleFromString(realm, roleName);

        if (role == null) {
            LOG.warnf("Unable to find role '%s' referenced by mapper '%s' on realm '%s'.", roleName,
                    mapperModel.getName(), realm.getName());
        }

        return role;
    }
}
