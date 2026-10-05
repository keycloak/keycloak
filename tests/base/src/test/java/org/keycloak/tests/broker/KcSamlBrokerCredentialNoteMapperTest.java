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
package org.keycloak.tests.broker;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.broker.provider.UserAuthenticationIdentityProvider;
import org.keycloak.common.Profile;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.UserSessionNoteStatementMapper;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.saml.SamlClient;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import static org.keycloak.tests.utils.matchers.Matchers.isSamlResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;


@KeycloakIntegrationTest(config = KcSamlBrokerCredentialNoteMapperTest.IdentityBrokeringAPIV2ServerConfig.class)
public class KcSamlBrokerCredentialNoteMapperTest extends AbstractKcSamlBrokerTest {

    private static final String LEAKED_ATTRIBUTE = "leaked-token";
    private static final String MAPPER_NAME = "pre-existing-leak";
    private static final int MAX_REDIRECTS = 10;

    private static final Pattern SAML_RESPONSE_INPUT =
            Pattern.compile("name=\"SAMLResponse\"[^>]*value=\"([^\"]+)\"");

    @InjectRunOnServer(realmRef = "consumer")
    RunOnServerClient runOnServer;

    @InjectHttpClient
    CloseableHttpClient httpClient;

    @Test
    public void shouldNotIssueBrokerCredentialNoteAsSamlAttribute() throws Exception {
        // The SAML provider only keeps its assertion in the user session when told to, and only under the V2
        // brokering API. Without both, the note would be absent and the test would pass without proving anything.
        storeBrokerTokenInSession();
        disableUpdateProfileOnFirstLogin();
        installBlockedMapperDirectlyOnTheModel();

        logInAsUserInIDP();
        updateAccountInformationIfPresent();

        assertThat("Identity provider did not store an assertion in the user session",
                brokerAccessTokenNote(), notNullValue());

        SAMLDocumentHolder samlResponse = requestAssertionForSalesPost();
        assertThat(samlResponse.getSamlObject(), isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));

        assertThat("The identity provider assertion was exposed as a SAML attribute",
                attributeNamesOf((ResponseType) samlResponse.getSamlObject()), not(hasItem(LEAKED_ATTRIBUTE)));
    }

    private void storeBrokerTokenInSession() {
        IdentityProviderResource idpResource = getConsumerRealm().admin().identityProviders().get(getIdpAlias());
        IdentityProviderRepresentation idp = idpResource.toRepresentation();
        idp.getConfig().put(IdentityProviderModel.STORE_TOKEN_IN_SESSION, Boolean.TRUE.toString());
        idpResource.update(idp);
    }

    /**
     * Sends an {@code AuthnRequest} over the brokered SSO session to issue an assertion without re-authentication,
     * extracting it directly from the POST-binding HTML form response.
     */
    private SAMLDocumentHolder requestAssertionForSalesPost() throws Exception {
        String consumerBaseUrl = getConsumerRealm().getBaseUrl();
        String consumerServerRoot = consumerBaseUrl.substring(0, consumerBaseUrl.indexOf("/realms/"));
        URI samlEndpoint = URI.create(consumerBaseUrl + "/protocol/saml");

        AuthnRequestType loginRequest = SamlClient.createLoginRequestDocument(
                SAML_CLIENT_ID_SALES_POST, consumerServerRoot + "/sales-post/saml", samlEndpoint);
        // Without this the response comes back on the redirect binding, which puts the assertion in a query
        // parameter on a redirect to the SP rather than in a form this test can read
        loginRequest.setProtocolBinding(SamlClient.Binding.POST.getBindingUri());
        Document samlRequest = SAML2Request.convert(loginRequest);
        HttpPost post = SamlClient.Binding.POST.createSamlUnsignedRequest(samlEndpoint, null, samlRequest);

        String body = followRedirects(post, brokeredSsoSession(consumerBaseUrl));

        var samlResponseInput = SAML_RESPONSE_INPUT.matcher(body);
        assertTrue(samlResponseInput.find(),
                "Expected a SAML response form, the SSO session was probably not reused. Got: " + body);

        return SAMLRequestParser.parseResponsePostBinding(samlResponseInput.group(1));
    }

    /**
     * Follows redirects (left to the caller by the injected client) so
     * the authentication endpoint recognizes the SSO cookie and renders the response form.
     */
    private String followRedirects(HttpUriRequest request, HttpClientContext context) throws IOException {
        HttpUriRequest current = request;
        for (int hop = 0; hop < MAX_REDIRECTS; hop++) {
            try (CloseableHttpResponse response = httpClient.execute(current, context)) {
                int status = response.getStatusLine().getStatusCode();
                Header location = response.getFirstHeader(HttpHeaders.LOCATION);
                if (status >= 300 && status < 400 && location != null) {
                    EntityUtils.consumeQuietly(response.getEntity());
                    current = new HttpGet(location.getValue());
                    continue;
                }
                return response.getEntity() == null
                        ? ""
                        : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        }
        throw new IllegalStateException("Gave up after " + MAX_REDIRECTS + " redirects");
    }

    /**
     * Copies the SSO cookies the brokered login left in the browser into an {@link HttpClientContext}, so the
     * {@code AuthnRequest} below runs on the session that holds the identity provider token.
     */
    private HttpClientContext brokeredSsoSession(String consumerBaseUrl) {
        // Point the browser at the {project_name} host first, cookies are only readable for the current document
        webDriver.open(consumerBaseUrl);

        String host = URI.create(consumerBaseUrl).getHost();
        BasicCookieStore cookieStore = new BasicCookieStore();
        // The browser scopes some cookies per path and so reports them more than once. Flattening every cookie to
        // "/" would then send duplicates, so keep one of each name.
        Map<String, String> byName = new LinkedHashMap<>();
        for (org.openqa.selenium.Cookie cookie : webDriver.cookies().getAll()) {
            byName.putIfAbsent(cookie.getName(), cookie.getValue());
        }
        byName.forEach((name, value) -> {
            BasicClientCookie copy = new BasicClientCookie(name, value);
            copy.setDomain(host);
            copy.setPath("/");
            cookieStore.addCookie(copy);
        });

        HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(cookieStore);
        return context;
    }

    /**
     * Adds the mapper through the model rather than the admin API, which is the only way to end up with one now that
     * the admin API rejects it.
     */
    private void installBlockedMapperDirectlyOnTheModel() {
        String realmName = getConsumerRealm().getName();
        String clientId = SAML_CLIENT_ID_SALES_POST;
        String noteName = brokerAccessTokenNoteName();

        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            ClientModel client = realm.getClientByClientId(clientId);

            ProtocolMapperModel mapper = new ProtocolMapperModel();
            mapper.setName(MAPPER_NAME);
            mapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
            mapper.setProtocolMapper(UserSessionNoteStatementMapper.PROVIDER_ID);
            mapper.setConfig(Map.of(
                    UserSessionNoteStatementMapper.NOTE_CONFIG_KEY, noteName,
                    AttributeStatementHelper.SAML_ATTRIBUTE_NAME, LEAKED_ATTRIBUTE,
                    AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, AttributeStatementHelper.BASIC));

            client.addProtocolMapper(mapper);
        });
    }

    private String brokerAccessTokenNote() {
        String realmName = getConsumerRealm().getName();
        String userLogin = getUserLogin();
        String noteName = brokerAccessTokenNoteName();

        return runOnServer.fetch(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            UserModel user = session.users().getUserByUsername(realm, userLogin);
            return session.sessions().getUserSessionsStream(realm, user)
                    .map(userSession -> userSession.getNote(noteName))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }, String.class);
    }

    private String brokerAccessTokenNoteName() {
        return UserAuthenticationIdentityProvider.FEDERATED_ACCESS_TOKEN + ":" + getIdpAlias();
    }

    private static Set<String> attributeNamesOf(ResponseType response) {
        return response.getAssertions().stream()
                .map(ResponseType.RTChoiceType::getAssertion)
                .filter(Objects::nonNull)
                .flatMap(assertion -> assertion.getAttributeStatements().stream())
                .flatMap(statement -> statement.getAttributes().stream())
                .map(AttributeStatementType.ASTChoiceType::getAttribute)
                .filter(Objects::nonNull)
                .map(AttributeType::getName)
                .collect(Collectors.toSet());
    }

    static class IdentityBrokeringAPIV2ServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.IDENTITY_BROKERING_API_V2);
        }
    }
}
