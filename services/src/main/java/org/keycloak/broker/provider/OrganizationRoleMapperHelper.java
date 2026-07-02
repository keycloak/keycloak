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

public final class OrganizationRoleMapperHelper {

    public static final String ORGANIZATION = "organization";
    public static final String ORGANIZATION_ROLE = "organizationRole";

    private OrganizationRoleMapperHelper() {
    }

    public static List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty organization = new ProviderConfigProperty();
        organization.setName(ORGANIZATION);
        organization.setLabel("Organization");
        organization.setHelpText("Organization id or alias linked to the identity provider. If empty, the mapper uses the linked organization.");
        organization.setType(ProviderConfigProperty.STRING_TYPE);

        ProviderConfigProperty role = new ProviderConfigProperty();
        role.setName(ORGANIZATION_ROLE);
        role.setLabel("Organization Role");
        role.setHelpText("Organization role id to grant to the user.");
        role.setType(ProviderConfigProperty.STRING_TYPE);

        return List.of(organization, role);
    }

    public static void grantUserRole(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        RoleModel role = getRole(session, realm, mapperModel, context);
        validateMembership(user, role);
        user.grantRole(role);
    }

    static RoleModel getRole(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        OrganizationModel organization = getOrganization(session, mapperModel, context);
        String roleId = mapperModel.getConfig().get(ORGANIZATION_ROLE);

        if (StringUtil.isBlank(roleId)) {
            throw new IdentityBrokerException("Organization role id is required for mapper '" + mapperModel.getName() + "' on realm '" + realm.getName() + "'.");
        }

        RoleModel role = session.roles().getRoleById(organization, roleId);
        if (role == null || !role.isOrganizationRole() || !Objects.equals(organization.getId(), role.getContainerId())) {
            throw new IdentityBrokerException("Unable to find organization role '" + roleId + "' referenced by mapper '" + mapperModel.getName()
                    + "' in organization '" + organization.getAlias() + "' on realm '" + realm.getName() + "'.");
        }

        return role;
    }

    private static OrganizationModel getOrganization(KeycloakSession session, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        OrganizationModel organization = KeycloakModelUtils.getOrganizationForIdpMapper(session, context.getIdpConfig());

        if (organization == null) {
            throw new IdentityBrokerException("Identity provider '" + context.getIdpConfig().getAlias() + "' is not linked to an enabled organization.");
        }

        String organizationRef = mapperModel.getConfig().get(ORGANIZATION);
        if (StringUtil.isBlank(organizationRef) || Objects.equals(organizationRef, organization.getId()) || Objects.equals(organizationRef, organization.getAlias())) {
            return organization;
        }

        throw new IdentityBrokerException("Organization '" + organizationRef + "' referenced by mapper '" + mapperModel.getName()
                + "' is not linked to identity provider '" + context.getIdpConfig().getAlias() + "'.");
    }

    private static void validateMembership(UserModel user, RoleModel role) {
        try {
            OrganizationsValidation.validateOrganizationRoleMapping(user, role);
        } catch (ModelException me) {
            throw new IdentityBrokerException(me.getMessage(), me);
        }
    }
}
