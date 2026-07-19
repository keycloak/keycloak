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

package org.keycloak.organization.protocol.mappers.saml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.Config.Scope;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.mappers.AbstractSAMLProtocolMapper;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.SAMLAttributeStatementMapper;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;

import static org.keycloak.organization.utils.Organizations.isEnabledAndOrganizationsPresent;

public class OrganizationGroupMembershipMapper extends AbstractSAMLProtocolMapper implements SAMLAttributeStatementMapper, EnvironmentDependentProviderFactory {

    public static final String ID = "saml-organization-group-membership-mapper";
    public static final String ADD_GROUP_ROLE_MAPPINGS = "addGroupRoleMappings";

    public static ProtocolMapperModel create() {
        ProtocolMapperModel mapper = new ProtocolMapperModel();

        mapper.setName("organization-groups");
        mapper.setProtocolMapper(ID);
        mapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);

        return mapper;
    }

    @Override
    public void transformAttributeStatement(AttributeStatementType attributeStatement, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        OrganizationProvider provider = session.getProvider(OrganizationProvider.class);

        if (!isEnabledAndOrganizationsPresent(provider)) {
            return;
        }

        UserModel user = userSession.getUser();
        boolean includeRoles = isAddGroupRoleMappings(mappingModel);
        Stream<OrganizationModel> organizations = provider.getByMember(user).filter(OrganizationModel::isEnabled);

        organizations.forEach(organization -> {
            List<GroupModel> userOrgGroups = provider.getOrganizationGroupsByMember(organization, user)
                .collect(Collectors.toList());

            List<String> groupPaths = userOrgGroups.stream()
                .map(ModelToRepresentation::buildGroupPath)
                .toList();

            String orgAlias = organization.getAlias();

            // Create attribute for this organization's groups
            String attributeName = "organization." + orgAlias + ".groups";
            AttributeType attribute = new AttributeType(attributeName);
            attribute.setFriendlyName("Organization Groups");
            attribute.setNameFormat(JBossSAMLURIConstants.ATTRIBUTE_FORMAT_BASIC.get());

            groupPaths.forEach(attribute::addAttributeValue);

            if (!groupPaths.isEmpty()) {
                attributeStatement.addAttribute(new AttributeStatementType.ASTChoiceType(attribute));
            }

            // Add role attributes if configured
            if (includeRoles) {
                Set<RoleModel> roleMappings = userOrgGroups.stream()
                    .flatMap(GroupModel::getRoleMappingsStream)
                    .collect(Collectors.toSet());
                roleMappings = RoleUtils.expandCompositeRoles(roleMappings);

                List<String> realmRoles = new ArrayList<>();
                Map<String, List<String>> clientRoles = new HashMap<>();

                for (RoleModel role : roleMappings) {
                    if (role.getContainer() instanceof RealmModel) {
                        realmRoles.add(role.getName());
                    } else if (role.getContainer() instanceof ClientModel clientModel) {
                        clientRoles.computeIfAbsent(clientModel.getClientId(), k -> new ArrayList<>())
                            .add(role.getName());
                    }
                }

                if (!realmRoles.isEmpty()) {
                    AttributeType rolesAttr = new AttributeType("organization." + orgAlias + ".realm_access.roles");
                    rolesAttr.setFriendlyName("Organization Realm Roles");
                    rolesAttr.setNameFormat(JBossSAMLURIConstants.ATTRIBUTE_FORMAT_BASIC.get());
                    realmRoles.forEach(rolesAttr::addAttributeValue);
                    attributeStatement.addAttribute(new AttributeStatementType.ASTChoiceType(rolesAttr));
                }

                clientRoles.forEach((clientId, roles) -> {
                    AttributeType clientRolesAttr = new AttributeType("organization." + orgAlias + ".resource_access." + clientId + ".roles");
                    clientRolesAttr.setFriendlyName("Organization Client Roles");
                    clientRolesAttr.setNameFormat(JBossSAMLURIConstants.ATTRIBUTE_FORMAT_BASIC.get());
                    roles.forEach(clientRolesAttr::addAttributeValue);
                    attributeStatement.addAttribute(new AttributeStatementType.ASTChoiceType(clientRolesAttr));
                });
            }
        });
    }

    private boolean isAddGroupRoleMappings(ProtocolMapperModel model) {
        return Boolean.parseBoolean(model.getConfig().getOrDefault(ADD_GROUP_ROLE_MAPPINGS, Boolean.FALSE.toString()));
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> properties = new ArrayList<>();
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(ADD_GROUP_ROLE_MAPPINGS);
        property.setLabel(ADD_GROUP_ROLE_MAPPINGS + ".label");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue(Boolean.FALSE.toString());
        property.setHelpText(ADD_GROUP_ROLE_MAPPINGS + ".help");
        properties.add(property);
        return properties;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getDisplayType() {
        return "Organization Group Membership";
    }

    @Override
    public String getDisplayCategory() {
        return AttributeStatementHelper.ATTRIBUTE_STATEMENT_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Add attributes to the assertion with information about the organization group membership.";
    }

    @Override
    public boolean isSupported(Scope config) {
        return Profile.isFeatureEnabled(Feature.ORGANIZATION);
    }

    @Override
    public int getPriority() {
        // Run after OrganizationMembershipMapper
        return 10;
    }
}
