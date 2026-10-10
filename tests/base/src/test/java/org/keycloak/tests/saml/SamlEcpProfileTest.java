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
package org.keycloak.tests.saml;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static org.keycloak.tests.utils.matchers.Matchers.isSamlResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for the SAML ECP profile, which authenticates the user with the credentials from the
 * <code>Authorization</code> header instead of an interactive login.
 *
 * @author vinckobb
 */
@KeycloakIntegrationTest
public class SamlEcpProfileTest extends AbstractSamlTest {

    private static final String SOAP_ENVELOPE_NS = "http://schemas.xmlsoap.org/soap/envelope/";

    // From testsaml.json, the only client with saml.allow.ecp.flow enabled
    private static final String SAML_CLIENT_ID_ECP_SP = "http://localhost:8280/ecp-sp/";
    private static final String SAML_CLIENT_ECP_SP_ASSERTION_CONSUMER_URL = "http://localhost:8080/ecp-sp/";

    @InjectHttpClient(followRedirects = false)
    CloseableHttpClient httpClient;

    @InjectUser(config = EcpUserConfig.class)
    ManagedUser ecpUser;

    /**
     * The authentication scheme identifier is case-insensitive, see
     * <a href="https://www.rfc-editor.org/rfc/rfc9110.html#section-11.1">RFC 9110, section 11.1</a>.
     */
    @Test
    public void testEcpFlowWithAnyCaseOfAuthenticationScheme() throws Exception {
        for (String scheme : new String[] {"Basic", "basic", "BASIC", "BaSiC"}) {
            assertThat("Authentication scheme '" + scheme + "'", authenticate(scheme),
                    isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        }
    }

    /**
     * Only the Basic scheme may be parsed as credentials. Anything else, including a token that merely
     * starts with it, is a different auth-scheme and must leave the request unauthenticated.
     */
    @Test
    public void testEcpFlowWithAnotherAuthenticationScheme() throws Exception {
        for (String scheme : new String[] {"BasicX", "Bearer", "Negotiate"}) {
            EcpResponse response = sendEcpAuthnRequest(scheme);

            assertThat("Authentication scheme '" + scheme + "' returned: " + response.body(), response.status(),
                    is(Response.Status.BAD_REQUEST.getStatusCode()));
            assertThat("Authentication scheme '" + scheme + "'", response.body(),
                    not(containsString(JBossSAMLURIConstants.STATUS_SUCCESS.get())));
        }
    }

    private SAML2Object authenticate(String scheme) throws Exception {
        EcpResponse response = sendEcpAuthnRequest(scheme);

        assertThat("Authentication scheme '" + scheme + "' returned: " + response.body(), response.status(),
                is(Response.Status.OK.getStatusCode()));

        return (SAML2Object) SAMLParser.getInstance()
                .parse(extractSoapBodyContent(DocumentUtil.getDocument(response.body())));
    }

    private EcpResponse sendEcpAuthnRequest(String scheme) throws Exception {
        AuthnRequestType authnRequest = createLoginRequestDocument(SAML_CLIENT_ID_ECP_SP,
                SAML_CLIENT_ECP_SP_ASSERTION_CONSUMER_URL, REALM_NAME);
        Document document = SAML2Request.convert(authnRequest);

        new BaseSAML2BindingBuilder()
                .signatureAlgorithm(SignatureAlgorithm.RSA_SHA256)
                .signWith(null, samlClientSalesPostSigPrivateKeyPk, samlClientSalesPostSigPublicKeyPk)
                .signDocument(document);

        String credentials = Base64.getEncoder().encodeToString(
                (ecpUser.getUsername() + ":" + ecpUser.getPassword()).getBytes(StandardCharsets.UTF_8));

        HttpPost post = new HttpPost(getAuthServerSamlEndpoint(REALM_NAME));
        post.setHeader(HttpHeaders.AUTHORIZATION, scheme + " " + credentials);
        post.setEntity(new StringEntity(DocumentUtil.asString(wrapInSoapEnvelope(document)), ContentType.TEXT_XML));

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            return new EcpResponse(response.getStatusLine().getStatusCode(),
                    EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
        }
    }

    private Document wrapInSoapEnvelope(Document samlDocument) throws Exception {
        Document soapDocument = DocumentUtil.createDocument();

        Element envelope = soapDocument.createElementNS(SOAP_ENVELOPE_NS, "soap:Envelope");
        Element body = soapDocument.createElementNS(SOAP_ENVELOPE_NS, "soap:Body");

        soapDocument.appendChild(envelope);
        envelope.appendChild(body);
        body.appendChild(soapDocument.importNode(samlDocument.getDocumentElement(), true));

        return soapDocument;
    }

    private Node extractSoapBodyContent(Document soapDocument) {
        Node body = soapDocument.getElementsByTagNameNS(SOAP_ENVELOPE_NS, "Body").item(0);

        assertThat("SOAP Body", body, notNullValue());

        for (Node node = body.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element) {
                return node;
            }
        }

        throw new AssertionError("No SAML message in the SOAP Body: " + DocumentUtil.asString(soapDocument));
    }

    private record EcpResponse(int status, String body) {
    }

    /**
     * The ECP profile authenticates without any interaction, so the user profile has to be complete. An
     * incomplete one adds the VERIFY_PROFILE required action and the flow then fails with NoPassive.
     */
    public static class EcpUserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("ecp-user")
                    .email("ecp-user@localhost")
                    .name("Ecp", "User")
                    .emailVerified(true)
                    .password("password");
        }
    }
}
