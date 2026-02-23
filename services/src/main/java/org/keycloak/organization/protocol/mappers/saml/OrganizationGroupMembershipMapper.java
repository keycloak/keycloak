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

import java.util.List;
import java.util.stream.Stream;

import org.keycloak.Config.Scope;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.ModelToRepresentation;
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
        Stream<OrganizationModel> organizations = provider.getByMember(user).filter(OrganizationModel::isEnabled);

        organizations.forEach(organization -> {
            // Get user's groups in this organization
            List<String> groupPaths = provider.getOrganizationGroupsByMember(organization, user)
                .map(ModelToRepresentation::buildGroupPath)
                .toList();

            // Create attribute for this organization's groups
            String attributeName = "organization." + organization.getAlias() + ".groups";
            AttributeType attribute = new AttributeType(attributeName);
            attribute.setFriendlyName("Organization Groups");
            attribute.setNameFormat(JBossSAMLURIConstants.ATTRIBUTE_FORMAT_BASIC.get());

            // Add each group path as an attribute value
            groupPaths.forEach(attribute::addAttributeValue);

            // Only add the attribute if there are groups
            if (!groupPaths.isEmpty()) {
                attributeStatement.addAttribute(new AttributeStatementType.ASTChoiceType(attribute));
            }
        });
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
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
