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

package org.keycloak.testsuite.organization.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType.ASTChoiceType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.organization.protocol.mappers.saml.OrganizationGroupMembershipMapper;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

public class OrganizationGroupMembershipSAMLMapperTest extends AbstractOrganizationTest {

    @Test
    public void testGroupsAsAttribute() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource organization = managedRealm.admin().organizations().get(orgRep.getId());
        IdentityProviderRepresentation broker = organization.identityProviders().getIdentityProviders().get(0);
        organization.identityProviders().get(broker.getAlias()).delete().close();
        MemberRepresentation member = addMember(organization);

        // Create organization groups: engineering -> backend
        GroupRepresentation engineering = new GroupRepresentation();
        engineering.setName("engineering");
        String engineeringId;
        try (Response response = organization.groups().addTopLevelGroup(engineering)) {
            engineeringId = ApiUtil.getCreatedId(response);
        }

        GroupRepresentation backend = new GroupRepresentation();
        backend.setName("backend");
        String backendId;
        try (Response response = organization.groups().group(engineeringId).addSubGroup(backend)) {
            backendId = response.readEntity(GroupRepresentation.class).getId();
        }

        // Add member to both groups
        organization.groups().group(engineeringId).addMember(member.getId());
        organization.groups().group(backendId).addMember(member.getId());

        String clientId = "saml-client";
        managedRealm.admin().clients().create(ClientBuilder.create()
                .protocol(SamlProtocol.LOGIN_PROTOCOL)
                .clientId(clientId)
                .redirectUris("*")
                .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, Boolean.FALSE.toString())
                .build()).close();

        // Add the organization group membership mapper to the client
        ClientRepresentation client = managedRealm.admin().clients().findByClientId(clientId).get(0);
        ClientResource clientResource = managedRealm.admin().clients().get(client.getId());

        ProtocolMapperRepresentation groupMapper = new ProtocolMapperRepresentation();
        groupMapper.setName("organization-groups");
        groupMapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        groupMapper.setProtocolMapper(OrganizationGroupMembershipMapper.ID);
        clientResource.getProtocolMappers().createMapper(groupMapper).close();

        SAMLDocumentHolder samlResponse = new SamlClientBuilder()
                .authnRequest(RealmsResource
                        .protocolUrl(UriBuilder.fromUri(getAuthServerRoot()))
                        .build(TEST_REALM_NAME, SamlProtocol.LOGIN_PROTOCOL), clientId, RoleMapperTest.SAML_ASSERTION_CONSUMER_URL_EMPLOYEE_2, SamlClient.Binding.POST)
                .build()
                .login().user(memberEmail, memberPassword).build()
                .login().user(memberEmail, memberPassword).build()
                .getSamlResponse(SamlClient.Binding.POST);

        assertThat(samlResponse.getSamlObject(), Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));

        // Find the organization groups attribute
        String expectedAttributeName = "organization." + organizationName + ".groups";
        AttributeType groupsAttribute = attributeStatements(assertionsUnencrypted(samlResponse.getSamlObject()))
                .flatMap((Function<AttributeStatementType, Stream<ASTChoiceType>>) attributeStatementType -> attributeStatementType.getAttributes().stream())
                .map(ASTChoiceType::getAttribute)
                .filter(attribute -> expectedAttributeName.equals(attribute.getName()))
                .findAny()
                .orElse(null);

        Assertions.assertNotNull(groupsAttribute, "Organization groups attribute should be present");

        List<Object> values = groupsAttribute.getAttributeValue();
        assertThat(values, hasSize(2));
        assertThat(values, containsInAnyOrder("/engineering", "/engineering/backend"));

        // Verify paths don't contain organization UUID (they are relative)
        values.forEach(value -> {
            String path = (String) value;
            assertThat("Path should not contain UUID", path, not(containsString(orgRep.getId())));
        });
    }

    @Test
    public void testGroupRoleMappingsAsAttributes() {
        OrganizationRepresentation orgRep = createOrganization();
        OrganizationResource organization = managedRealm.admin().organizations().get(orgRep.getId());
        IdentityProviderRepresentation broker = organization.identityProviders().getIdentityProviders().get(0);
        organization.identityProviders().get(broker.getAlias()).delete().close();
        MemberRepresentation member = addMember(organization);

        // Create org group
        GroupRepresentation engineering = new GroupRepresentation();
        engineering.setName("engineering");
        String engineeringId;
        try (Response response = organization.groups().addTopLevelGroup(engineering)) {
            engineeringId = ApiUtil.getCreatedId(response);
        }

        // Create a realm role and assign to the group
        RoleRepresentation realmRole = new RoleRepresentation("saml-org-role", "", false);
        managedRealm.admin().roles().create(realmRole);
        RoleRepresentation createdRealmRole = managedRealm.admin().roles().get("saml-org-role").toRepresentation();
        organization.groups().group(engineeringId).roles().realmLevel().add(List.of(createdRealmRole));

        // Create a client with a role and assign to the group
        ClientRepresentation roleClient = new ClientRepresentation();
        roleClient.setClientId("saml-role-test-client");
        roleClient.setEnabled(true);
        String roleClientUuid;
        try (Response response = managedRealm.admin().clients().create(roleClient)) {
            roleClientUuid = ApiUtil.getCreatedId(response);
        }
        RoleRepresentation clientRole = new RoleRepresentation("saml-org-client-role", "", false);
        managedRealm.admin().clients().get(roleClientUuid).roles().create(clientRole);
        RoleRepresentation createdClientRole = managedRealm.admin().clients().get(roleClientUuid).roles().get("saml-org-client-role").toRepresentation();
        organization.groups().group(engineeringId).roles().clientLevel(roleClientUuid).add(List.of(createdClientRole));

        // Add member to group
        organization.groups().group(engineeringId).addMember(member.getId());

        // Create SAML client with group mapper that has addGroupRoleMappings enabled
        String clientId = "saml-role-client";
        managedRealm.admin().clients().create(ClientBuilder.create()
                .protocol(SamlProtocol.LOGIN_PROTOCOL)
                .clientId(clientId)
                .redirectUris("*")
                .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, Boolean.FALSE.toString())
                .build()).close();

        ClientRepresentation client = managedRealm.admin().clients().findByClientId(clientId).get(0);
        ClientResource clientResource = managedRealm.admin().clients().get(client.getId());

        ProtocolMapperRepresentation groupMapper = new ProtocolMapperRepresentation();
        groupMapper.setName("organization-groups");
        groupMapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        groupMapper.setProtocolMapper(OrganizationGroupMembershipMapper.ID);
        Map<String, String> config = new HashMap<>();
        config.put(OrganizationGroupMembershipMapper.ADD_GROUP_ROLE_MAPPINGS, Boolean.TRUE.toString());
        groupMapper.setConfig(config);
        clientResource.getProtocolMappers().createMapper(groupMapper).close();

        SAMLDocumentHolder samlResponse = new SamlClientBuilder()
                .authnRequest(RealmsResource
                        .protocolUrl(UriBuilder.fromUri(getAuthServerRoot()))
                        .build(TEST_REALM_NAME, SamlProtocol.LOGIN_PROTOCOL), clientId, RoleMapperTest.SAML_ASSERTION_CONSUMER_URL_EMPLOYEE_2, SamlClient.Binding.POST)
                .build()
                .login().user(memberEmail, memberPassword).build()
                .login().user(memberEmail, memberPassword).build()
                .getSamlResponse(SamlClient.Binding.POST);

        assertThat(samlResponse.getSamlObject(), Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));

        // Extract all SAML attributes
        List<AttributeType> allAttributes = attributeStatements(assertionsUnencrypted(samlResponse.getSamlObject()))
                .flatMap((Function<AttributeStatementType, Stream<ASTChoiceType>>) st -> st.getAttributes().stream())
                .map(ASTChoiceType::getAttribute)
                .toList();

        // Verify realm roles attribute
        String rolesAttrName = "organization." + organizationName + ".realm_access.roles";
        AttributeType rolesAttribute = allAttributes.stream()
                .filter(a -> rolesAttrName.equals(a.getName()))
                .findAny()
                .orElse(null);
        Assertions.assertNotNull(rolesAttribute, "Organization realm roles attribute should be present");
        assertThat(rolesAttribute.getAttributeValue(), hasSize(1));
        Assertions.assertEquals("saml-org-role", rolesAttribute.getAttributeValue().get(0));

        // Verify client roles attribute
        String clientRolesAttrName = "organization." + organizationName + ".resource_access.saml-role-test-client.roles";
        AttributeType clientRolesAttribute = allAttributes.stream()
                .filter(a -> clientRolesAttrName.equals(a.getName()))
                .findAny()
                .orElse(null);
        Assertions.assertNotNull(clientRolesAttribute, "Organization client roles attribute should be present");
        assertThat(clientRolesAttribute.getAttributeValue(), hasSize(1));
        Assertions.assertEquals("saml-org-client-role", clientRolesAttribute.getAttributeValue().get(0));
    }
}
