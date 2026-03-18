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

package org.keycloak.tests.oid4vc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.OAuth2Constants;
import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ComponentsResource;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.TimeProvider;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBuilder;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.JwtCredentialBuilder;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.SdJwtCredentialBuilder;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.DisplayObject;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oidc.utils.OAuth2Code;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.remote.providers.runonserver.RunOnServerException;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialIssuerMetadataResponse;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.config.UPConfigUtils;
import org.keycloak.util.JsonSerialization;
import org.keycloak.validate.validators.PatternValidator;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_VC;
import static org.keycloak.jose.jwe.JWEConstants.RSA_OAEP_256;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_CONFIGURATION_ID;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint.CREDENTIAL_OFFER_URI_CODE_SCOPE;
import static org.keycloak.userprofile.DeclarativeUserProfileProvider.UP_COMPONENT_CONFIG_KEY;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_ADMIN;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_USER;
import static org.keycloak.util.JsonSerialization.valueAsPrettyString;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public abstract class OID4VCIssuerEndpointTest extends OID4VCIssuerTestBase {

    private static final Logger LOGGER = Logger.getLogger(OID4VCIssuerEndpointTest.class);

    protected static final TimeProvider TIME_PROVIDER = new StaticTimeProvider(1000);

    protected ClientScopeRepresentation sdJwtTypeCredentialClientScope;
    protected ClientScopeRepresentation jwtTypeCredentialClientScope;
    protected ClientScopeRepresentation minimalJwtTypeCredentialClientScope;

    protected CloseableHttpClient httpClient;
    protected ClientRepresentation client;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    record OAuth2CodeEntry(String key, OAuth2Code code) {}

    @Override
    public void configureTestRealm() {
        super.configureTestRealm();

        ComponentsResource components = testRealm.admin().components();
        components.add(getRsaKeyProvider(getRsaKey_Default())).close();
        components.add(getRsaEncKeyProvider(RSA_OAEP_256, "enc-key-oaep256", 100)).close();
        components.add(getUserProfileProvider());
    }

    @BeforeEach
    public void setup() {
        httpClient = HttpClientBuilder.create().build();
        client = requireExistingClient(OID4VCI_CLIENT_ID);
        sdJwtTypeCredentialClientScope = requireExistingClientScope(sdJwtTypeCredentialScopeName);
        jwtTypeCredentialClientScope = requireExistingClientScope(jwtTypeCredentialScopeName);
        minimalJwtTypeCredentialClientScope = requireExistingClientScope(minimalJwtTypeCredentialScopeName);
    }

    protected static OAuth2CodeEntry prepareSessionCode(
            KeycloakSession session,
            AppAuthManager.BearerTokenAuthenticator authenticator,
            String note) {

        AuthenticationManager.AuthResult authResult = authenticator.authenticate();
        UserSessionModel userSessionModel = authResult.session();
        AuthenticatedClientSessionModel authenticatedClientSessionModel =
                userSessionModel.getAuthenticatedClientSessionByClient(authResult.client().getId());

        OAuth2Code oauth2Code = new OAuth2Code(
                SecretGenerator.getInstance().randomString(),
                Time.currentTime() + 6000,
                SecretGenerator.getInstance().randomString(),
                CREDENTIAL_OFFER_URI_CODE_SCOPE,
                authenticatedClientSessionModel.getUserSession().getId()
        );

        String nonce = OAuth2CodeParser.persistCode(session, authenticatedClientSessionModel, oauth2Code);
        authenticatedClientSessionModel.setNote(nonce, note);

        return new OAuth2CodeEntry(nonce, oauth2Code);
    }

    protected static OID4VCIssuerEndpoint prepareIssuerEndpoint(
            KeycloakSession session,
            AppAuthManager.BearerTokenAuthenticator authenticator) {

        JwtCredentialBuilder jwtCredentialBuilder = new JwtCredentialBuilder(TIME_PROVIDER, session);
        SdJwtCredentialBuilder sdJwtCredentialBuilder = new SdJwtCredentialBuilder();

        Map<String, CredentialBuilder> credentialBuilders = Map.of(
                jwtCredentialBuilder.getSupportedFormat(), jwtCredentialBuilder,
                sdJwtCredentialBuilder.getSupportedFormat(), sdJwtCredentialBuilder
        );

        return prepareIssuerEndpoint(session, authenticator, credentialBuilders);
    }

    protected static OID4VCIssuerEndpoint prepareIssuerEndpoint(
            KeycloakSession session,
            AppAuthManager.BearerTokenAuthenticator authenticator,
            Map<String, CredentialBuilder> credentialBuilders) {

        return new OID4VCIssuerEndpoint(
                session,
                credentialBuilders,
                authenticator,
                TIME_PROVIDER,
                30
        );
    }

    protected ClientScopeRepresentation createOptionalClientScope(
            String scopeName,
            String issuerDid,
            String credentialConfigurationId,
            String credentialIdentifier,
            String vct,
            String format,
            String protocolMapperReferenceFile,
            List<String> acceptedKeyAttestationValues) {

        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName(scopeName);
        clientScope.setProtocol(OID4VCIConstants.OID4VC_PROTOCOL);

        Map<String, String> attributes = new HashMap<>();
        attributes.put(ClientScopeModel.INCLUDE_IN_TOKEN_SCOPE, "true");
        attributes.put(CredentialScopeModel.VC_EXPIRY_IN_SECONDS, "15");

        if (issuerDid != null) {
            attributes.put(CredentialScopeModel.VC_ISSUER_DID, issuerDid);
        }
        if (credentialConfigurationId != null) {
            attributes.put(CredentialScopeModel.VC_CONFIGURATION_ID, credentialConfigurationId);
        }
        if (credentialIdentifier != null) {
            attributes.put(CredentialScopeModel.VC_IDENTIFIER, credentialIdentifier);
        }
        if (format != null) {
            attributes.put(CredentialScopeModel.VC_FORMAT, format);
        }

        attributes.put(CredentialScopeModel.VCT, Optional.ofNullable(vct).orElse(credentialIdentifier));

        if (credentialConfigurationId != null) {
            try {
                String vcDisplay = JsonSerialization.writeValueAsString(List.of(
                        new DisplayObject().setName(credentialConfigurationId).setLocale("en-EN"),
                        new DisplayObject().setName(credentialConfigurationId).setLocale("de-DE")
                ));
                attributes.put(CredentialScopeModel.VC_DISPLAY, vcDisplay);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (acceptedKeyAttestationValues != null) {
            attributes.put(CredentialScopeModel.VC_KEY_ATTESTATION_REQUIRED, "true");
            if (!acceptedKeyAttestationValues.isEmpty()) {
                String joinedValues = String.join(",", acceptedKeyAttestationValues);
                attributes.put(CredentialScopeModel.VC_KEY_ATTESTATION_REQUIRED_KEY_STORAGE, joinedValues);
                attributes.put(CredentialScopeModel.VC_KEY_ATTESTATION_REQUIRED_USER_AUTH, joinedValues);
            }
        }
        clientScope.setAttributes(attributes);

        List<ProtocolMapperRepresentation> protocolMappers;
        if (protocolMapperReferenceFile == null) {
            protocolMappers = getProtocolMappers(scopeName);
        } else {
            protocolMappers = resolveProtocolMappers(protocolMapperReferenceFile);
            protocolMappers.add(getStaticClaimMapper(scopeName));
        }

        clientScope.setProtocolMappers(protocolMappers);
        return clientScope;
    }

    protected ClientScopeRepresentation registerOptionalClientScope(ClientScopeRepresentation clientScope) {
        // Automatically removed when a test method is finished.
        try (Response res = testRealm.admin().clientScopes().create(clientScope)) {
            String scopeId = ApiUtil.getCreatedId(res);
            testRealm.cleanup().add(realm -> realm.clientScopes().get(scopeId).remove());
            clientScope.setId(scopeId);
        }
        return clientScope;
    }

    protected ClientRepresentation requireExistingClient(String clientId) {
        return testRealm.admin().clients().findByClientId(clientId).stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("No such client: " + clientId));
    }

    protected ClientScopeRepresentation requireExistingClientScope(String scopeName) {
        // Check if the client scope already exists
        return testRealm.admin().clientScopes().findAll().stream()
                .filter(scope -> scope.getName().equals(scopeName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No such client scope: " + scopeName));
    }

    public static List<ProtocolMapperRepresentation> resolveProtocolMappers(String protocolMapperReferenceFile) {
        if (protocolMapperReferenceFile == null) {
            return null;
        }
        try (InputStream inputStream = OID4VCIssuerEndpointTest.class.getResourceAsStream(protocolMapperReferenceFile)) {
            return JsonSerialization.mapper.readValue(inputStream, ClientScopeRepresentation.class).getProtocolMappers();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Tests the AuthZCode complete flow without scope from
    // 1. Get authorization code without scope specified by wallet
    // 2. Using the code to get access token
    // 3. Get the credential configuration id from issuer metadata at .wellKnown
    // 4. With the access token, get the credential
    protected void testCredentialIssuanceWithAuthZCodeFlow(ClientScopeRepresentation credScope,
                                                           Function<String, String> f,
                                                           Consumer<Map<String, Object>> c) {

        // Use credential_identifier if available, otherwise use configuration_id for error testing
        String testCredentialConfigurationId = credScope.getAttributes().get(VC_CONFIGURATION_ID);
        testCredentialIssuanceWithAuthZCodeFlow(credScope, f, c, (credentialIdentifier) -> {
            CredentialRequest request = new CredentialRequest();
            if (credentialIdentifier != null) {
                request.setCredentialIdentifier(credentialIdentifier);
            } else {
                request.setCredentialConfigurationId(testCredentialConfigurationId);
            }
            return request;
        });
    }

    protected void testCredentialIssuanceWithAuthZCodeFlow(
            ClientScopeRepresentation clientScope,
            Function<String, String> f,
            Consumer<Map<String, Object>> c,
            Function<String, CredentialRequest> crf) {

        String testScope = clientScope.getName();
        String testFormat = clientScope.getAttributes().get(CredentialScopeModel.VC_FORMAT);
        String testCredentialConfigurationId = clientScope.getAttributes().get(CredentialScopeModel.VC_CONFIGURATION_ID);

        // Use credential_identifier if available, otherwise use configuration_id for error testing
        if (crf == null) {
            crf = (credentialIdentifier) -> {
                CredentialRequest request = new CredentialRequest();
                if (credentialIdentifier != null) {
                    request.setCredentialIdentifier(credentialIdentifier);
                } else {
                    request.setCredentialConfigurationId(testCredentialConfigurationId);
                }
                return request;
            };
        }


        try (Client restClient = Keycloak.getClientProvider().newRestEasyClient(null, null, true)) {
            String metadataUrl = getRealmMetadataPath(testRealm.getName());
            WebTarget oid4vciDiscoveryTarget = restClient.target(metadataUrl);

            // 1. Get authorization code without scope specified by wallet
            // 2. Using the code to get the AccessToken
            String token = f.apply(testScope);

            // Extract credential_identifier from the token (client-side parsing)
            String credentialIdentifier = null;
            try {
                JsonWebToken jwt = new JWSInput(token).readJsonContent(JsonWebToken.class);
                Object authDetails = jwt.getOtherClaims().get(OAuth2Constants.AUTHORIZATION_DETAILS);
                if (authDetails != null) {
                    List<OID4VCAuthorizationDetail> authDetailsResponse = JsonSerialization.readValue(
                            JsonSerialization.writeValueAsString(authDetails),
                            new TypeReference<>() {}
                    );
                    if (!authDetailsResponse.isEmpty() &&
                            authDetailsResponse.get(0).getCredentialIdentifiers() != null &&
                            !authDetailsResponse.get(0).getCredentialIdentifiers().isEmpty()) {
                        credentialIdentifier = authDetailsResponse.get(0).getCredentialIdentifiers().get(0);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to extract credential_identifier from token", e);
            }

            // 3. Get the credential configuration id from issuer metadata at .wellKnown
            try (Response discoveryResponse = oid4vciDiscoveryTarget.request().get()) {
                CredentialIssuer oid4vciIssuerConfig = JsonSerialization.readValue(
                        discoveryResponse.readEntity(String.class),
                        CredentialIssuer.class
                );

                assertEquals(200, discoveryResponse.getStatus());
                assertEquals(getRealmPath(testRealm.getName()), oid4vciIssuerConfig.getCredentialIssuer());
                assertEquals(getBasePath(testRealm.getName()) + "credential", oid4vciIssuerConfig.getCredentialEndpoint());

                // 4. With the access token, get the credential
                try (Client clientForCredentialRequest = Keycloak.getClientProvider().newRestEasyClient(null, null, true)) {
                    UriBuilder credentialUriBuilder = UriBuilder.fromUri(oid4vciIssuerConfig.getCredentialEndpoint());
                    URI credentialUri = credentialUriBuilder.build();
                    WebTarget credentialTarget = clientForCredentialRequest.target(credentialUri);

                    CredentialRequest request = crf.apply(credentialIdentifier);

                    assertEquals(testFormat, oid4vciIssuerConfig.getCredentialsSupported().get(testCredentialConfigurationId).getFormat());
                    assertEquals(testCredentialConfigurationId, oid4vciIssuerConfig.getCredentialsSupported().get(testCredentialConfigurationId).getId());

                    c.accept(Map.of(
                            "accessToken", token,
                            "credentialTarget", credentialTarget,
                            "credentialRequest", request
                    ));
                }
            }
        } catch (IOException | AssertionError e) {
            throw new RuntimeException(e);
        }
    }

    protected String getBasePath(String realm) {
        return getRealmPath(realm) + "/protocol/oid4vc/";
    }

    protected String getRealmPath(String realm) {
        return keycloakUrls.getBaseUrl() + "/realms/" + realm;
    }

    protected String getRealmMetadataPath(String realm) {
        return keycloakUrls.getBaseUrl() + "/.well-known/openid-credential-issuer/realms/" + realm;
    }

    protected void requestCredentialWithIdentifier(
            String token,
            String credentialEndpoint,
            String credentialIdentifier,
            CredentialResponseHandler responseHandler,
            ClientScopeRepresentation expectedClientScope) throws IOException, VerificationException {

        CredentialRequest request = new CredentialRequest();
        request.setCredentialIdentifier(credentialIdentifier);

        StringEntity stringEntity = new StringEntity(JsonSerialization.writeValueAsString(request), ContentType.APPLICATION_JSON);

        HttpPost postCredential = new HttpPost(credentialEndpoint);
        postCredential.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        postCredential.setEntity(stringEntity);

        CredentialResponse credentialResponse;
        try (CloseableHttpResponse credentialRequestResponse = httpClient.execute(postCredential)) {
            assertEquals(HttpStatus.SC_OK, credentialRequestResponse.getStatusLine().getStatusCode());
            String s = IOUtils.toString(credentialRequestResponse.getEntity().getContent(), StandardCharsets.UTF_8);
            credentialResponse = JsonSerialization.readValue(s, CredentialResponse.class);
        }

        // Use response handler to customize checks based on formats.
        responseHandler.handleCredentialResponse(credentialResponse, expectedClientScope);
    }

    public CredentialIssuer getCredentialIssuerMetadata() {
        CredentialIssuerMetadataResponse metadataResponse = oauth.oid4vc().doIssuerMetadataRequest();
        return metadataResponse.getMetadata();
    }

    private ComponentRepresentation getUserProfileProvider() {
        // Add the User DID attribute, with the same logic as in DeclarativeUserProfileProviderFactory
        UPConfig profileConfig = UPConfigUtils.parseSystemDefaultConfig();

        if (profileConfig.getAttribute(UserModel.DID) == null) {
            UPAttribute attr = new UPAttribute(UserModel.DID);
            attr.setDisplayName("${did}");
            attr.setPermissions(new UPAttributePermissions(Set.of(ROLE_ADMIN, ROLE_USER), Set.of(ROLE_ADMIN, ROLE_USER)));
            attr.setValidations(Map.of(
                    PatternValidator.ID, Map.of(
                            "pattern", "^did:.+:.+$",
                            "error-message", "Value must start with 'did:scheme:'"
                    )
            ));
            profileConfig.addOrReplaceAttribute(attr);
        }

        ComponentRepresentation componentRepresentation = new ComponentRepresentation();
        componentRepresentation.setId(UUID.randomUUID().toString());
        componentRepresentation.setProviderId("declarative-user-profile");
        componentRepresentation.setProviderType(UserProfileProvider.class.getName());
        componentRepresentation.setConfig(new MultivaluedHashMap<>(
                Map.of(UP_COMPONENT_CONFIG_KEY, List.of(JsonSerialization.valueAsString(profileConfig)))
        ));

        return componentRepresentation;
    }

    protected void withCausePropagation(Runnable r) throws Throwable {
        try {
            r.run();
        } catch (Exception e) {
            if (e instanceof RunOnServerException) {
                throw e.getCause();
            }
            throw e;
        }
    }

    protected static class CredentialResponseHandler {
        final Logger log = Logger.getLogger(OID4VCIssuerEndpointTest.class);

        protected void handleCredentialResponse(CredentialResponse credentialResponse, ClientScopeRepresentation clientScope) throws VerificationException {
            assertNotNull(credentialResponse.getCredentials(), "The credentials array should be present in the response.");
            assertFalse(credentialResponse.getCredentials().isEmpty(), "The credentials array should not be empty.");

            // Get the first credential from the array (maintaining compatibility with single credential tests)
            CredentialResponse.Credential credentialObj = credentialResponse.getCredentials().get(0);
            assertNotNull(credentialObj, "The first credential in the array should not be null.");

            JsonWebToken jsonWebToken = TokenVerifier.create((String) credentialObj.getCredential(), JsonWebToken.class).getToken();
            Map<String, Object> otherClaims = jsonWebToken.getOtherClaims();

            log.infof("JsonWebToken: %s", valueAsPrettyString(jsonWebToken));
            assertNotNull(jsonWebToken.getId(), "Expected jti claim");
            assertNotNull(jsonWebToken.getExp(), "Expected exp claim");
            assertNotNull(jsonWebToken.getNbf(), "Expected nbf claim");
            assertNotNull(jsonWebToken.getIssuer(), "Expected iss claim");
            assertNotNull(jsonWebToken.getSubject(), "Expected sub claim");

            assertNull(jsonWebToken.getAudience(), "Unexpected aud claim");
            assertNull(jsonWebToken.getIat(), "Unexpected iat claim");

            assertEquals("did:web:test.org", jsonWebToken.getIssuer());
            assertEquals(Set.of(CLAIM_NAME_VC), otherClaims.keySet());

            @SuppressWarnings("unchecked")
            Map<String, ?> vc = (Map<String, ?>) otherClaims.get("vc");
            VerifiableCredential credential = JsonSerialization.mapper.convertValue(vc, VerifiableCredential.class);
            Map<String, ?> subClaims = credential.getCredentialSubject().getClaims();

            assertNotNull(credential.getIssuanceDate(), "Expected vc.issuanceDate claim");
            assertNotNull(credential.getExpirationDate(), "Expected vc.expirationDate claim");
            assertNotNull(credential.getContext(), "Expected vc.@context claim");

            assertEquals(List.of(clientScope.getName()), credential.getType(), "vc.type mapped correctly");
            assertEquals("did:web:test.org", jsonWebToken.getIssuer(), "iss mapped correctly");
            assertEquals(URI.create("did:web:test.org"), credential.getIssuer(), "vc.issuer mapped correctly");
            assertEquals(jsonWebToken.getSubject(), subClaims.get("id"), "vc.credentialSubject.id mapped correctly");
            assertEquals("John", subClaims.get("given_name"), "vc.credentialSubject.given_name mapped correctly");
            assertEquals("john@email.cz", subClaims.get("email"), "vc.credentialSubject.email mapped correctly");
            assertEquals(clientScope.getName(), subClaims.get("scope-name"), "vc.credentialSubject.scope-name mapped correctly");

            assertThat("vc.credentialSubject.address is parent claim for nested claims", subClaims.get("address"), instanceOf(Map.class));

            @SuppressWarnings("unchecked")
            Map<String, ?> nestedAddressClaim = (Map<String, ?>) subClaims.get("address");

            assertThat("vc.credentialSubject.address contains two nested claims", nestedAddressClaim, aMapWithSize(2));
            assertEquals("221B Baker Street", nestedAddressClaim.get("street_address"), "vc.credentialSubject.address.street_address mapped correctly");
            assertEquals("London", nestedAddressClaim.get("locality"), "vc.credentialSubject.address.locality mapped correctly");

            assertFalse(subClaims.containsKey("AnotherCredentialType"), "Unexpected other claim");
        }
    }
}
