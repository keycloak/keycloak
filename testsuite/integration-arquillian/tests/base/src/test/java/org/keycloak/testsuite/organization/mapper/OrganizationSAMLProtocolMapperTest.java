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

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType.ASTChoiceType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.organization.protocol.mappers.saml.OrganizationMembershipMapper;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.services.resources.RealmsResource;
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

public class OrganizationSAMLProtocolMapperTest extends AbstractOrganizationTest {

    @Test
    public void testAttribute() {
        OrganizationResource organization = testRealm().organizations().get(createOrganization().getId());
        IdentityProviderRepresentation broker = organization.identityProviders().getIdentityProviders().get(0);
        organization.identityProviders().get(broker.getAlias()).delete().close();
        addMember(organization);
        String clientId = "saml-client";
        testRealm().clients().create(ClientBuilder.create()
                .protocol(SamlProtocol.LOGIN_PROTOCOL)
                .clientId(clientId)
                .redirectUris("*")
                .attribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE, Boolean.FALSE.toString())
                .build()).close();

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
        Assert.assertNotNull(orgAttribute);
        List<Object> values = orgAttribute.getAttributeValue();
        Assert.assertEquals(1, values.size());
        Assert.assertEquals(organizationName, values.get(0));
    }
}
