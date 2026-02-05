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
import org.keycloak.organization.protocol.mappers.saml.OrganizationGroupMembershipMapper;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.MemberRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.organization.admin.AbstractOrganizationTest;
import org.keycloak.testsuite.saml.RoleMapperTest;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.junit.Assert;
import org.junit.Test;

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
        OrganizationResource organization = testRealm().organizations().get(orgRep.getId());
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
        testRealm().clients().create(ClientBuilder.create()
                .protocol(SamlProtocol.LOGIN_PROTOCOL)
                .clientId(clientId)
                .redirectUris("*")
                .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, Boolean.FALSE.toString())
                .build()).close();

        // Add the organization group membership mapper to the client
        ClientRepresentation client = testRealm().clients().findByClientId(clientId).get(0);
        ClientResource clientResource = testRealm().clients().get(client.getId());

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

        Assert.assertNotNull("Organization groups attribute should be present", groupsAttribute);

        List<Object> values = groupsAttribute.getAttributeValue();
        assertThat(values, hasSize(2));
        assertThat(values, containsInAnyOrder("/engineering", "/engineering/backend"));

        // Verify paths don't contain organization UUID (they are relative)
        values.forEach(value -> {
            String path = (String) value;
            assertThat("Path should not contain UUID", path, not(containsString(orgRep.getId())));
        });
    }
}
