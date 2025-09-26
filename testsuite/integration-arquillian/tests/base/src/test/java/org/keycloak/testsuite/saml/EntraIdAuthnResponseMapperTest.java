/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.saml;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.protocol.saml.mappers.EntraIdMultiFactorAuthnContextMapper;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.ProtocolMappersUpdater;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

import static org.keycloak.protocol.saml.mappers.EntraIdMultiFactorAuthnContextMapper.SCHEMAS_MICROSOFT_COM_CLAIMS_MULTIPLEAUTHN;
import static org.keycloak.testsuite.saml.RoleMapperTest.createSamlProtocolMapper;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_PORT;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SCHEME;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;

/**
 *
 * @author rmartinc
 */
public class EntraIdAuthnResponseMapperTest extends AbstractSamlTest {

    public static final String SAML_ASSERTION_CONSUMER_URL_EMPLOYEE_2 = AUTH_SERVER_SCHEME + "://localhost:" + (AUTH_SERVER_SSL_REQUIRED ? AUTH_SERVER_PORT : 8080) + "/employee2/";

    private ProtocolMappersUpdater pmu;

    @Before
    public void cleanMappersAndScopes() {
        this.pmu = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_EMPLOYEE_2).protocolMappers()
                .clear()
                .update();
    }

    @After
    public void revertCleanMappersAndScopes() throws IOException {
        this.pmu.close();
    }


    @Test
    public void testEntraId() throws Exception {
        pmu.add(createSamlProtocolMapper(EntraIdMultiFactorAuthnContextMapper.PROVIDER_ID)).update();
        // remove full scope
        try (ClientAttributeUpdater cau = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_EMPLOYEE_2)
                .setFullScopeAllowed(false)
                .update()) {

            SAMLDocumentHolder document = new SamlClientBuilder()
                    .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_EMPLOYEE_2, SAML_ASSERTION_CONSUMER_URL_EMPLOYEE_2, SamlClient.Binding.POST).build()
                    .login().user(bburkeUser).build()
                    .getSamlResponse(SamlClient.Binding.POST);

            verifyAuthn(document, JBossSAMLURIConstants.AC_UNSPECIFIED.get());

            UserRepresentation userRep = UserBuilder.edit(bburkeUser)
                    .totpSecret("totpSecret")
                    .otpEnabled().build();
            UserResource user = ApiUtil.findUserByUsernameId(adminClient.realm(REALM_NAME), bburkeUser.getUsername());
            user.update(userRep);

            try {
                document = new SamlClientBuilder()
                        .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_EMPLOYEE_2, SAML_ASSERTION_CONSUMER_URL_EMPLOYEE_2, SamlClient.Binding.POST).build()
                        .login().user(bburkeUser).build()
                        .totp().secret("totpSecret").build()
                        .getSamlResponse(SamlClient.Binding.POST);

                verifyAuthn(document, SCHEMAS_MICROSOFT_COM_CLAIMS_MULTIPLEAUTHN);
            } finally {
                CredentialRepresentation cr = user.credentials().stream().filter(credentialRepresentation -> credentialRepresentation.getType().equals(OTPCredentialModel.TYPE)).findFirst().orElseThrow();
                user.removeCredential(cr.getId());
            }
        }
    }

    private static void verifyAuthn(SAMLDocumentHolder document, String uri) {
        ((ResponseType) document.getSamlObject()).getAssertions().stream()
                .map(ResponseType.RTChoiceType::getAssertion)
                .filter(Objects::nonNull)
                .flatMap(assertionType -> assertionType.getStatements().stream())
                .filter(statementAbstractType -> statementAbstractType instanceof AuthnStatementType authnStatementType && authnStatementType.getAuthnContext().getSequence().getClassRef().getValue().equals(URI.create(uri)))
                .findAny().orElseThrow(() -> new RuntimeException("didn't find authn"));
    }

}
