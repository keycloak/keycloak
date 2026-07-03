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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType.ASTChoiceType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.organization.protocol.mappers.OrganizationRoleMapperUtils;
import org.keycloak.organization.protocol.mappers.saml.OrganizationRoleMembershipMapper;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
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

import static org.keycloak.testsuite.util.SamlStreams.assertionsUnencrypted;
import static org.keycloak.testsuite.util.SamlStreams.attributeStatements;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

public class OrganizationRoleMembershipSAMLMapperTest extends AbstractOrganizationTest {

    @Test
    public void testOrganizationRolesAndCompositesAsAttributes() {
        OrganizationResource acme = createOrganizationWithoutBroker("acme");
        OrganizationResource other = createOrganizationWithoutBroker("other");
        MemberRepresentation member = addMember(acme);
        other.members().addMember(member.getId()).close();

        RoleRepresentation acmeRole = createOrganizationRole(acme, "acme-admin");
        RoleRepresentation acmeChildRole = createOrganizationRole(acme, "acme-auditor");
        RoleRepresentation otherRole = createOrganizationRole(other, "other-admin");
        RoleRepresentation realmRole = createRealmRole("saml-organization-realm-composite");
        ClientRole clientRole = createClientRole("saml-organization-role-client", "saml-organization-client-composite");

        acme.roles().get(acmeRole.getId()).addComposites(List.of(acmeChildRole, realmRole, clientRole.role()));
        assignOrganizationRole(acme, acmeRole, member.getId());
        assignOrganizationRole(other, otherRole, member.getId());

        String clientId = "organization-role-saml-client";
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

        SAMLDocumentHolder response = new SamlClientBuilder()
                .authnRequest(RealmsResource.protocolUrl(UriBuilder.fromUri(getAuthServerRoot()))
                                .build(TEST_REALM_NAME, SamlProtocol.LOGIN_PROTOCOL),
                        clientId, RoleMapperTest.SAML_ASSERTION_CONSUMER_URL_EMPLOYEE_2, SamlClient.Binding.POST)
                .build()
                .login().user(memberEmail, memberPassword).build()
                .login().user(memberEmail, memberPassword).build()
                .getSamlResponse(SamlClient.Binding.POST);

        assertThat(response.getSamlObject(), Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        Map<String, AttributeType> attributes = attributeStatements(assertionsUnencrypted(response.getSamlObject()))
                .flatMap((Function<AttributeStatementType, Stream<ASTChoiceType>>) statement -> statement.getAttributes().stream())
                .map(ASTChoiceType::getAttribute)
                .filter(attribute -> attribute.getName().startsWith("organization."))
                .collect(Collectors.toMap(AttributeType::getName, Function.identity()));

        String acmePrefix = "organization.acme.";
        AttributeType acmeRoles = attributes.get(acmePrefix + OrganizationRoleMapperUtils.ORGANIZATION_ROLES);
        assertThat(acmeRoles, notNullValue());
        assertThat(acmeRoles.getAttributeValue(), hasItem("acme-admin"));
        assertThat(acmeRoles.getAttributeValue(), hasItem("acme-auditor"));
        assertThat(acmeRoles.getAttributeValue(), not(hasItem("other-admin")));

        AttributeType realmRoles = attributes.get(acmePrefix + OrganizationRoleMapperUtils.REALM_ACCESS + "." + OrganizationRoleMapperUtils.ROLES);
        assertThat(realmRoles, notNullValue());
        assertThat(realmRoles.getAttributeValue(), hasItem(realmRole.getName()));

        AttributeType clientRoles = attributes.get(acmePrefix + OrganizationRoleMapperUtils.RESOURCE_ACCESS + "."
                + clientRole.clientId() + "." + OrganizationRoleMapperUtils.ROLES);
        assertThat(clientRoles, notNullValue());
        assertThat(clientRoles.getAttributeValue(), hasItem(clientRole.role().getName()));

        AttributeType otherRoles = attributes.get("organization.other." + OrganizationRoleMapperUtils.ORGANIZATION_ROLES);
        assertThat(otherRoles, notNullValue());
        assertThat(otherRoles.getAttributeValue(), hasItem("other-admin"));
        assertThat(otherRoles.getAttributeValue(), not(hasItem("acme-admin")));
    }

    private OrganizationResource createOrganizationWithoutBroker(String name) {
        OrganizationRepresentation representation = createOrganization(name);
        OrganizationResource organization = managedRealm.admin().organizations().get(representation.getId());
        IdentityProviderRepresentation broker = organization.identityProviders().getIdentityProviders().get(0);
        organization.identityProviders().get(broker.getAlias()).delete().close();
        return organization;
    }

    private RoleRepresentation createOrganizationRole(OrganizationResource organization, String name) {
        try (Response response = organization.roles().create(new RoleRepresentation(name, "", false))) {
            assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
            return organization.roles().get(ApiUtil.getCreatedId(response)).toRepresentation();
        }
    }

    private RoleRepresentation createRealmRole(String name) {
        managedRealm.admin().roles().create(new RoleRepresentation(name, "", false));
        return managedRealm.admin().roles().get(name).toRepresentation();
    }

    private ClientRole createClientRole(String clientId, String roleName) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        client.setEnabled(true);
        String clientUuid;
        try (Response response = managedRealm.admin().clients().create(client)) {
            clientUuid = ApiUtil.getCreatedId(response);
        }
        managedRealm.admin().clients().get(clientUuid).roles().create(new RoleRepresentation(roleName, "", false));
        RoleRepresentation role = managedRealm.admin().clients().get(clientUuid).roles().get(roleName).toRepresentation();
        return new ClientRole(clientId, role);
    }

    private static void assignOrganizationRole(OrganizationResource organization, RoleRepresentation role, String userId) {
        UserRepresentation user = new UserRepresentation();
        user.setId(userId);
        organization.roles().get(role.getId()).addUserMembers(List.of(user));
    }

    private record ClientRole(String clientId, RoleRepresentation role) {
    }
}
