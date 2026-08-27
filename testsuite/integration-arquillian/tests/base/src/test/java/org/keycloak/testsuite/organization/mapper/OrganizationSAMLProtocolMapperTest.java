/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.organization.mapper;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType.ASTChoiceType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.assertion.AudienceRestrictionType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.organization.protocol.mappers.saml.OrganizationMembershipMapper;
import org.keycloak.organization.protocol.mappers.saml.OrganizationRoleMembershipMapper;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.mappers.SAMLAudienceResolveProtocolMapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.saml.RoleMapperTest;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.keycloak.testsuite.util.SamlStreams.assertionsUnencrypted;
import static org.keycloak.testsuite.util.SamlStreams.attributeStatements;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

public class OrganizationSAMLProtocolMapperTest extends AbstractOrganizationTest {

    @Test
    public void testAttribute() {
        OrganizationRepresentation organizationRepresentation = createOrganization();
        OrganizationResource organization = managedRealm.admin().organizations().get(organizationRepresentation.getId());
        IdentityProviderRepresentation broker = organization.identityProviders().getIdentityProviders().get(0);
        organization.identityProviders().get(broker.getAlias()).delete().close();
        MemberRepresentation member = addMember(organization);

        RoleRepresentation directRole = createOrganizationRole(organization, "org-admin");
        RoleRepresentation childRole = createOrganizationRole(organization, "org-auditor");
        RoleRepresentation realmRole = new RoleRepresentation("organization-realm-composite", "", false);
        managedRealm.admin().roles().create(realmRole);
        realmRole = managedRealm.admin().roles().get(realmRole.getName()).toRepresentation();

        ClientRepresentation roleClient = new ClientRepresentation();
        roleClient.setClientId("organization-role-client");
        roleClient.setEnabled(true);
        roleClient.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        String roleClientId;
        try (Response response = managedRealm.admin().clients().create(roleClient)) {
            roleClientId = ApiUtil.getCreatedId(response);
        }
        managedRealm.admin().clients().get(roleClientId).roles().create(new RoleRepresentation("organization-client-composite", "", false));
        RoleRepresentation clientRole = managedRealm.admin().clients().get(roleClientId).roles().get("organization-client-composite").toRepresentation();

        organization.roles().get(directRole.getId()).addComposites(List.of(childRole, realmRole, clientRole));
        UserRepresentation memberReference = new UserRepresentation();
        memberReference.setId(member.getId());
        organization.roles().get(directRole.getId()).addUserMembers(List.of(memberReference));

        String clientId = "saml-client";
        managedRealm.admin().clients().create(ClientBuilder.create()
                .protocol(SamlProtocol.LOGIN_PROTOCOL)
                .clientId(clientId)
                .redirectUris("*")
                .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, Boolean.FALSE.toString())
                .build()).close();

        ClientRepresentation client = managedRealm.admin().clients().findByClientId(clientId).get(0);
        ClientResource clientResource = managedRealm.admin().clients().get(client.getId());
        ProtocolMapperRepresentation roleMapper = new ProtocolMapperRepresentation();
        roleMapper.setName("organization-roles");
        roleMapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        roleMapper.setProtocolMapper(OrganizationRoleMembershipMapper.ID);
        clientResource.getProtocolMappers().createMapper(roleMapper).close();

        ProtocolMapperRepresentation audienceMapper = new ProtocolMapperRepresentation();
        audienceMapper.setName("audience-resolve");
        audienceMapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        audienceMapper.setProtocolMapper(SAMLAudienceResolveProtocolMapper.PROVIDER_ID);
        clientResource.getProtocolMappers().createMapper(audienceMapper).close();

        SAMLDocumentHolder samlResponse = new SamlClientBuilder()
                .authnRequest(RealmsResource
                        .protocolUrl(UriBuilder.fromUri(getAuthServerRoot()))
                        .build(TEST_REALM_NAME, SamlProtocol.LOGIN_PROTOCOL), clientId, RoleMapperTest.SAML_ASSERTION_CONSUMER_URL_EMPLOYEE_2, SamlClient.Binding.POST)
                .build()
                .login().user(memberEmail, memberPassword).build()
                .login().user(memberEmail, memberPassword).build()
                .getSamlResponse(SamlClient.Binding.POST);

        assertThat(samlResponse.getSamlObject(), Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        AttributeType orgAttribute = attributeStatements(assertionsUnencrypted(samlResponse.getSamlObject()))
                .flatMap((Function<AttributeStatementType, Stream<ASTChoiceType>>) attributeStatementType -> attributeStatementType.getAttributes().stream())
                .map(ASTChoiceType::getAttribute)
                .filter(attribute -> OrganizationMembershipMapper.ORGANIZATION_ATTRIBUTE_NAME.equals(attribute.getName()))
                .findAny()
                .orElse(null);
        Assertions.assertNotNull(orgAttribute);
        List<Object> values = orgAttribute.getAttributeValue();
        assertThat(values, hasSize(1));
        assertThat(values, containsInAnyOrder(organizationName));

        List<AttributeType> attributes = attributeStatements(assertionsUnencrypted(samlResponse.getSamlObject()))
                .flatMap((Function<AttributeStatementType, Stream<ASTChoiceType>>) statement -> statement.getAttributes().stream())
                .map(ASTChoiceType::getAttribute)
                .toList();

        assertAttributeValues(attributes, "organization." + organizationName + ".roles",
                "default-roles-org-" + organizationName, "org-admin", "org-auditor");
        assertAttributeValues(attributes, "organization." + organizationName + ".realm_access.roles", realmRole.getName());
        assertAttributeValues(attributes, "organization." + organizationName + ".resource_access.organization-role-client.roles",
                clientRole.getName());
        AudienceRestrictionType audience = ((ResponseType) samlResponse.getSamlObject()).getAssertions().get(0).getAssertion()
                .getConditions().getConditions().stream()
                .filter(AudienceRestrictionType.class::isInstance)
                .map(AudienceRestrictionType.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(audience.getAudience().stream().map(Object::toString).toList(),
                containsInAnyOrder(clientId, roleClient.getClientId()));
    }

    private RoleRepresentation createOrganizationRole(OrganizationResource organization, String name) {
        RoleRepresentation role = new RoleRepresentation(name, "", false);
        try (Response response = organization.roles().create(role)) {
            return organization.roles().get(ApiUtil.getCreatedId(response)).toRepresentation();
        }
    }

    private static void assertAttributeValues(List<AttributeType> attributes, String name, String... values) {
        AttributeType attribute = attributes.stream().filter(candidate -> name.equals(candidate.getName())).findAny().orElse(null);
        Assertions.assertNotNull(attribute, "Missing SAML attribute " + name);
        assertThat(attribute.getAttributeValue(), hasSize(values.length));
        assertThat(attribute.getAttributeValue(), containsInAnyOrder(values));
    }
}
