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

package org.keycloak.tests.organization.mapper;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Base64;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType.ASTChoiceType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.assertion.AudienceRestrictionType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.organization.protocol.mappers.saml.OrganizationMembershipMapper;
import org.keycloak.organization.protocol.mappers.saml.OrganizationRoleMembershipMapper;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.GroupMembershipMapper;
import org.keycloak.protocol.saml.mappers.SAMLAudienceResolveProtocolMapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.organization.admin.AbstractOrganizationTest;
import org.keycloak.tests.saml.SamlClient;

import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

@KeycloakIntegrationTest
public class OrganizationRoleMembershipSAMLMapperTest extends AbstractOrganizationTest {

    @Test
    public void shouldMapOrganizationRolesAndKeepInternalGroupsOutOfSaml() throws Exception {
        realm.dirty();
        OrganizationRepresentation organizationRepresentation = createOrganization();
        OrganizationResource organization = realm.admin().organizations().get(organizationRepresentation.getId());
        IdentityProviderRepresentation broker = organization.identityProviders().getIdentityProviders().get(0);
        organization.identityProviders().get(broker.getAlias()).delete().close();
        MemberRepresentation member = addMember(organization);

        GroupRepresentation realmGroup = new GroupRepresentation();
        realmGroup.setName("visible-saml-group");
        try (Response response = realm.admin().groups().add(realmGroup)) {
            realmGroup.setId(ApiUtil.getCreatedId(response));
        }
        realm.admin().users().get(member.getId()).joinGroup(realmGroup.getId());

        GroupRepresentation organizationGroup = new GroupRepresentation();
        organizationGroup.setName("hidden-saml-organization-group");
        try (Response response = organization.groups().addTopLevelGroup(organizationGroup)) {
            organizationGroup.setId(ApiUtil.getCreatedId(response));
        }
        organization.groups().group(organizationGroup.getId()).addMember(member.getId());
        organizationGroup = organization.groups().group(organizationGroup.getId()).toRepresentation(false);

        RoleRepresentation directRole = createOrganizationRole(organization, "org-admin");
        RoleRepresentation childRole = createOrganizationRole(organization, "org-auditor");
        RoleRepresentation groupRole = createOrganizationRole(organization, "org-group-reviewer");
        organization.groups().group(organizationGroup.getId()).roles()
                .addOrganizationRoleMappings(List.of(groupRole));
        RoleRepresentation realmRole = new RoleRepresentation("organization-realm-composite", "", false);
        realm.admin().roles().create(realmRole);
        realmRole = realm.admin().roles().get(realmRole.getName()).toRepresentation();

        ClientRepresentation roleClient = new ClientRepresentation();
        roleClient.setClientId("organization-role-client");
        roleClient.setEnabled(true);
        roleClient.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        String roleClientId;
        try (Response response = realm.admin().clients().create(roleClient)) {
            roleClientId = ApiUtil.getCreatedId(response);
        }
        realm.admin().clients().get(roleClientId).roles().create(new RoleRepresentation("organization-client-composite", "", false));
        RoleRepresentation clientRole = realm.admin().clients().get(roleClientId).roles().get("organization-client-composite").toRepresentation();

        organization.roles().get(directRole.getId()).addComposites(List.of(childRole, realmRole, clientRole));
        UserRepresentation memberReference = new UserRepresentation();
        memberReference.setId(member.getId());
        organization.roles().get(directRole.getId()).addUserMembers(List.of(memberReference));

        String clientId = "saml-client";
        realm.admin().clients().create(ClientBuilder.create()
                .protocol(SamlProtocol.LOGIN_PROTOCOL)
                .clientId(clientId)
                .redirectUris("*")
                .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, Boolean.FALSE.toString())
                .build()).close();

        ClientRepresentation client = realm.admin().clients().findByClientId(clientId).get(0);
        ClientResource clientResource = realm.admin().clients().get(client.getId());
        ProtocolMapperRepresentation roleMapper = new ProtocolMapperRepresentation();
        roleMapper.setName("organization-roles");
        roleMapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        roleMapper.setProtocolMapper(OrganizationRoleMembershipMapper.ID);
        clientResource.getProtocolMappers().createMapper(roleMapper).close();

        ProtocolMapperRepresentation groupMapper = new ProtocolMapperRepresentation();
        groupMapper.setName("groups");
        groupMapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        groupMapper.setProtocolMapper(GroupMembershipMapper.PROVIDER_ID);
        groupMapper.setConfig(java.util.Map.of(
                AttributeStatementHelper.SAML_ATTRIBUTE_NAME, "groups",
                GroupMembershipMapper.SINGLE_GROUP_ATTRIBUTE, Boolean.TRUE.toString(),
                "full.path", Boolean.TRUE.toString()));
        clientResource.getProtocolMappers().createMapper(groupMapper).close();

        ProtocolMapperRepresentation audienceMapper = new ProtocolMapperRepresentation();
        audienceMapper.setName("audience-resolve");
        audienceMapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        audienceMapper.setProtocolMapper(SAMLAudienceResolveProtocolMapper.PROVIDER_ID);
        clientResource.getProtocolMappers().createMapper(audienceMapper).close();

        SAMLDocumentHolder samlResponse = login(clientId);
        ResponseType response = (ResponseType) samlResponse.getSamlObject();
        Assertions.assertEquals(JBossSAMLURIConstants.STATUS_SUCCESS.get(), response.getStatus().getStatusCode().getValue().toString());
        List<AttributeType> attributes = response.getAssertions().get(0).getAssertion().getAttributeStatements().stream()
                .flatMap(statement -> statement.getAttributes().stream())
                .map(ASTChoiceType::getAttribute)
                .toList();
        AttributeType orgAttribute = attributes.stream()
                .filter(attribute -> OrganizationMembershipMapper.ORGANIZATION_ATTRIBUTE_NAME.equals(attribute.getName()))
                .findFirst().orElse(null);
        Assertions.assertNotNull(orgAttribute);
        List<Object> values = orgAttribute.getAttributeValue();
        assertThat(values, hasSize(1));
        assertThat(values, containsInAnyOrder(organizationName));

        assertAttributeValues(attributes, "organization." + organizationName + ".roles",
                "default-roles-org-" + organizationName, "org-admin", "org-auditor", "org-group-reviewer");
        assertAttributeValues(attributes, "organization." + organizationName + ".realm_access.roles", realmRole.getName());
        assertAttributeValues(attributes, "organization." + organizationName + ".resource_access.organization-role-client.roles",
                clientRole.getName());
        assertAttributeValues(attributes, "groups", "/visible-saml-group");
        List<Object> groupValues = attributes.stream().filter(attribute -> "groups".equals(attribute.getName()))
                .findAny().orElseThrow().getAttributeValue();
        Assertions.assertFalse(groupValues.contains(organizationGroup.getId()));
        Assertions.assertFalse(groupValues.contains(organizationGroup.getPath()));
        AudienceRestrictionType audience = ((ResponseType) samlResponse.getSamlObject()).getAssertions().get(0).getAssertion()
                .getConditions().getConditions().stream()
                .filter(AudienceRestrictionType.class::isInstance)
                .map(AudienceRestrictionType.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(audience.getAudience().stream().map(Object::toString).toList(),
                containsInAnyOrder(clientId, roleClient.getClientId()));
    }

    private SAMLDocumentHolder login(String clientId) throws Exception {
        URI endpoint = URI.create(realm.getBaseUrl() + "/protocol/saml");
        var request = SamlClient.createLoginRequestDocument(clientId, "http://localhost/saml-consumer", endpoint);
        URI loginUrl = new BaseSAML2BindingBuilder().redirectBinding(SAML2Request.convert(request)).requestURI(endpoint.toString());
        try (WebClient browser = new WebClient()) {
            browser.getOptions().setJavaScriptEnabled(false);
            HtmlPage page = browser.getPage(loginUrl.toURL());
            // Organization login may first ask for the username, then the password.
            for (int step = 0; step < 2 && page.getFirstByXPath("//input[@name='SAMLResponse']") == null; step++) {
                HtmlInput username = (HtmlInput) page.getElementById("username");
                HtmlInput password = (HtmlInput) page.getElementById("password");
                if (username != null) {
                    username.setValue(memberEmail);
                }
                if (password != null) {
                    password.setValue(memberPassword);
                }
                page = page.getHtmlElementById("kc-login").click();
            }
            HtmlInput response = page.getFirstByXPath("//input[@name='SAMLResponse']");
            Assertions.assertNotNull(response, "Expected a SAML response after organization login");
            return SAML2Request.getSAML2ObjectFromStream(new ByteArrayInputStream(Base64.getDecoder().decode(response.getValue())));
        }
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
