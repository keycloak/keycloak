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
package org.keycloak.broker.provider;

import java.util.List;
import java.util.Objects;

import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.validation.OrganizationsValidation;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

public final class OrganizationRoleMapperHelper {

    public static final String ORGANIZATION = "organization";
    public static final String ORGANIZATION_ROLE = "organizationRole";

    private static final Logger LOG = Logger.getLogger(OrganizationRoleMapperHelper.class);

    private OrganizationRoleMapperHelper() {
    }

    public static List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty role = new ProviderConfigProperty();
        role.setName(ORGANIZATION_ROLE);
        role.setLabel("Organization Role");
        role.setHelpText("Organization role to grant from the organization linked to the identity provider.");
        role.setType(ProviderConfigProperty.ORGANIZATION_ROLE_TYPE);
        role.setRequired(true);

        return List.of(role);
    }

    static void validateConfig(KeycloakSession session, RealmModel realm, IdentityProviderModel identityProvider,
            IdentityProviderMapperModel mapperModel) throws IdentityProviderMapperConfigException {
        OrganizationModel organization = KeycloakModelUtils.getOrganizationForIdpMapper(session, identityProvider);
        if (organization == null) {
            throw new IdentityProviderMapperConfigException("Identity provider '" + identityProvider.getAlias()
                    + "' is not linked to an enabled organization on realm '" + realm.getName() + "'.");
        }

        String organizationRef = mapperModel.getConfig().get(ORGANIZATION);
        if (!StringUtil.isBlank(organizationRef) && !Objects.equals(organizationRef, organization.getId())
                && !Objects.equals(organizationRef, organization.getAlias())) {
            throw new IdentityProviderMapperConfigException("Organization '" + organizationRef + "' referenced by mapper '"
                    + mapperModel.getName() + "' is not linked to identity provider '" + identityProvider.getAlias() + "'.");
        }

        String roleId = mapperModel.getConfig().get(ORGANIZATION_ROLE);
        if (StringUtil.isBlank(roleId)) {
            throw new IdentityProviderMapperConfigException("Organization role is required for mapper '" + mapperModel.getName() + "'.");
        }

        RoleModel role = session.roles().getRoleById(organization, roleId);
        if (role == null || !role.isOrganizationRole() || !Objects.equals(organization.getId(), role.getContainerId())) {
            throw new IdentityProviderMapperConfigException("Unable to find organization role '" + roleId + "' referenced by mapper '"
                    + mapperModel.getName() + "' in organization '" + organization.getAlias() + "'.");
        }
    }

    public static void grantUserRole(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        RoleModel role = getRole(session, realm, mapperModel, context);
        if (role == null) {
            return;
        }
        validateMembership(user, role);
        user.grantRole(role);
    }

    static RoleModel getRole(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        OrganizationModel organization = getOrganization(session, realm, mapperModel, context);
        if (organization == null) {
            return null;
        }

        String roleId = mapperModel.getConfig().get(ORGANIZATION_ROLE);

        if (StringUtil.isBlank(roleId)) {
            LOG.warnf("Organization role id is required for mapper '%s' on realm '%s'.", mapperModel.getName(), realm.getName());
            return null;
        }

        RoleModel role = session.roles().getRoleById(organization, roleId);
        if (role == null || !role.isOrganizationRole() || !Objects.equals(organization.getId(), role.getContainerId())) {
            LOG.warnf("Unable to find organization role '%s' referenced by mapper '%s' in organization '%s' on realm '%s'.", roleId,
                    mapperModel.getName(), organization.getAlias(), realm.getName());
            return null;
        }

        return role;
    }

    private static OrganizationModel getOrganization(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        OrganizationModel organization = KeycloakModelUtils.getOrganizationForIdpMapper(session, context.getIdpConfig());

        if (organization == null) {
            LOG.warnf("Identity provider '%s' is not linked to an enabled organization for mapper '%s' on realm '%s'.",
                    context.getIdpConfig().getAlias(), mapperModel.getName(), realm.getName());
            return null;
        }

        String organizationRef = mapperModel.getConfig().get(ORGANIZATION);
        if (StringUtil.isBlank(organizationRef) || Objects.equals(organizationRef, organization.getId()) || Objects.equals(organizationRef, organization.getAlias())) {
            return organization;
        }

        LOG.warnf("Organization '%s' referenced by mapper '%s' on realm '%s' is not linked to identity provider '%s'.", organizationRef,
                mapperModel.getName(), realm.getName(), context.getIdpConfig().getAlias());
        return null;
    }

    private static void validateMembership(UserModel user, RoleModel role) {
        try {
            OrganizationsValidation.validateOrganizationRoleMapping(user, role);
        } catch (ModelException me) {
            throw new IdentityBrokerException(me.getMessage(), me);
        }
    }
}
