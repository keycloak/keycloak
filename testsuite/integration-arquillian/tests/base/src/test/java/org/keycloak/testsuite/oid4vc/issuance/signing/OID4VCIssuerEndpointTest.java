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

package org.keycloak.testsuite.oid4vc.issuance.signing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.TokenVerifier;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.VerificationException;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.constants.Oid4VciConstants;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jwe.JWEHeader;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.RSAPublicJWK;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCAuthorizationDetailsResponse;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerWellKnownProviderFactory;
import org.keycloak.protocol.oid4vc.issuance.TimeProvider;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.CredentialBuilder;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.JwtCredentialBuilder;
import org.keycloak.protocol.oid4vc.issuance.credentialbuilder.SdJwtCredentialBuilder;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.DisplayObject;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oidc.utils.OAuth2Code;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.runonserver.RunOnServerException;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.logging.Logger;
import org.junit.Before;

import static org.keycloak.jose.jwe.JWEConstants.A256GCM;
import static org.keycloak.jose.jwe.JWEConstants.RSA_OAEP;
import static org.keycloak.jose.jwe.JWEConstants.RSA_OAEP_256;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint.CREDENTIAL_OFFER_URI_CODE_SCOPE;
import static org.keycloak.protocol.oid4vc.model.ProofType.JWT;
import static org.keycloak.testsuite.forms.PassThroughClientAuthenticator.clientId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Moved test to subclass. so we can reuse initialization code.
 */
public abstract class OID4VCIssuerEndpointTest extends OID4VCTest {

    protected static final TimeProvider TIME_PROVIDER = new OID4VCTest.StaticTimeProvider(1000);
    protected static final String sdJwtCredentialVct = "https://credentials.example.com/SD-JWT-Credential";

    private static final Logger LOGGER = Logger.getLogger(OID4VCIssuerEndpointTest.class);

    protected static ClientScopeRepresentation sdJwtTypeNaturalPersonClientScope;
    protected static ClientScopeRepresentation sdJwtTypeCredentialClientScope;
    protected static ClientScopeRepresentation jwtTypeCredentialClientScope;
    protected static ClientScopeRepresentation minimalJwtTypeCredentialClientScope;

    protected CloseableHttpClient httpClient;
    protected ClientRepresentation client;

    protected boolean shouldEnableOid4vci() {
        return true;
    }

    protected static String prepareSessionCode(KeycloakSession session, AppAuthManager.BearerTokenAuthenticator authenticator, String note) {
        AuthenticationManager.AuthResult authResult = authenticator.authenticate();
        UserSessionModel userSessionModel = authResult.session();
        AuthenticatedClientSessionModel authenticatedClientSessionModel = userSessionModel.getAuthenticatedClientSessionByClient(
                authResult.client().getId());
        String codeId = SecretGenerator.getInstance().randomString();
        String nonce = SecretGenerator.getInstance().randomString();
        OAuth2Code oAuth2Code = new OAuth2Code(codeId,
                Time.currentTime() + 6000,
                nonce,
                CREDENTIAL_OFFER_URI_CODE_SCOPE,
                null,
                null,
                null,
                null,
                authenticatedClientSessionModel.getUserSession().getId());

        String oauthCode = OAuth2CodeParser.persistCode(session, authenticatedClientSessionModel, oAuth2Code);

        authenticatedClientSessionModel.setNote(oauthCode, note);
        return oauthCode;
    }

    protected static OID4VCIssuerEndpoint prepareIssuerEndpoint(KeycloakSession session,
                                                                AppAuthManager.BearerTokenAuthenticator authenticator) {
        JwtCredentialBuilder jwtCredentialBuilder = new JwtCredentialBuilder(
                new StaticTimeProvider(1000));
        SdJwtCredentialBuilder sdJwtCredentialBuilder = new SdJwtCredentialBuilder();

        return prepareIssuerEndpoint(
                session,
                authenticator,
                Map.of(jwtCredentialBuilder.getSupportedFormat(), jwtCredentialBuilder,
                        sdJwtCredentialBuilder.getSupportedFormat(), sdJwtCredentialBuilder)
        );
    }

    protected static OID4VCIssuerEndpoint prepareIssuerEndpoint(
            KeycloakSession session,
            AppAuthManager.BearerTokenAuthenticator authenticator,
            Map<String, CredentialBuilder> credentialBuilders
    ) {
        return new OID4VCIssuerEndpoint(
                session,
                credentialBuilders,
                authenticator,
                TIME_PROVIDER,
                30);
    }

    @Before
    public void setup() {
        CryptoIntegration.init(this.getClass().getClassLoader());
        httpClient = HttpClientBuilder.create().build();
        client = testRealm().clients().findByClientId(clientId).get(0);

        // Lookup the pre-installed oid4vc_natural_person client scope
        sdJwtTypeNaturalPersonClientScope = requireExistingClientScope(sdJwtTypeNaturalPersonScopeName);

        // Register the optional client scopes
        sdJwtTypeCredentialClientScope = registerOptionalClientScope(sdJwtTypeCredentialScopeName,
                null,
                sdJwtTypeCredentialConfigurationIdName,
                sdJwtTypeCredentialScopeName,
                sdJwtCredentialVct,
                Format.SD_JWT_VC,
                null);
        jwtTypeCredentialClientScope = registerOptionalClientScope(jwtTypeCredentialScopeName,
                TEST_DID.toString(),
                jwtTypeCredentialConfigurationIdName,
                jwtTypeCredentialScopeName,
                null,
                Format.JWT_VC,
                TEST_CREDENTIAL_MAPPERS_FILE);
        minimalJwtTypeCredentialClientScope = registerOptionalClientScope("vc-with-minimal-config",
                null,
                null,
                null,
                null,
                null,
                null);

        // Assign the registered optional client scopes to the client
        assignOptionalClientScopeToClient(sdJwtTypeCredentialClientScope.getId(), client.getClientId());
        assignOptionalClientScopeToClient(jwtTypeCredentialClientScope.getId(), client.getClientId());
        assignOptionalClientScopeToClient(minimalJwtTypeCredentialClientScope.getId(), client.getClientId());

        // Enable OID4VCI for the client by default, but allow tests to override
        setClientOid4vciEnabled(clientId, shouldEnableOid4vci());
    }

    private ClientResource findClientByClientId(RealmResource realm, String clientId) {
        for (ClientRepresentation c : realm.clients().findAll()) {
            if (clientId.equals(c.getClientId())) {
                return realm.clients().get(c.getId());
            }
        }
        return null;
    }

    private ClientScopeRepresentation registerOptionalClientScope(String scopeName,
                                                                  String issuerDid,
                                                                  String credentialConfigurationId,
                                                                  String credentialIdentifier,
                                                                  String vct,
                                                                  String format,
                                                                  String protocolMapperReferenceFile) {
        // Check if the client scope already exists
        List<ClientScopeRepresentation> existingScopes = testRealm().clientScopes().findAll();
        for (ClientScopeRepresentation existingScope : existingScopes) {
            if (existingScope.getName().equals(scopeName)) {
                return existingScope; // Reuse existing scope
            }
        }

        // Create a new ClientScope if not found
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName(scopeName);
        clientScope.setProtocol(Oid4VciConstants.OID4VC_PROTOCOL);
        Map<String, String> attributes =
                new HashMap<>(Map.of(ClientScopeModel.INCLUDE_IN_TOKEN_SCOPE, "true",
                        CredentialScopeModel.EXPIRY_IN_SECONDS, "15"));
        BiConsumer<String, String> addAttribute = (attributeName, value) -> {
            if (value != null) {
                attributes.put(attributeName, value);
            }
        };
        addAttribute.accept(CredentialScopeModel.ISSUER_DID, issuerDid);
        addAttribute.accept(CredentialScopeModel.CONFIGURATION_ID, credentialConfigurationId);
        addAttribute.accept(CredentialScopeModel.CREDENTIAL_IDENTIFIER, credentialIdentifier);
        addAttribute.accept(CredentialScopeModel.FORMAT, format);
        addAttribute.accept(CredentialScopeModel.VCT, Optional.ofNullable(vct).orElse(credentialIdentifier));
        if (credentialConfigurationId != null) {
            String vcDisplay;
            try {
                vcDisplay = JsonSerialization.writeValueAsString(List.of(new DisplayObject().setName(credentialConfigurationId)
                                .setLocale("en-EN"),
                        new DisplayObject().setName(credentialConfigurationId)
                                .setLocale("de-DE")));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            addAttribute.accept(CredentialScopeModel.VC_DISPLAY, vcDisplay);
        }
        clientScope.setAttributes(attributes);

        Response res = testRealm().clientScopes().create(clientScope);
        String scopeId = ApiUtil.getCreatedId(res);
        getCleanup().addClientScopeId(scopeId); // Automatically removed when a test method is finished.
        res.close();

        clientScope.setId(scopeId);

        // Add protocol mappers to the ClientScope
        List<ProtocolMapperRepresentation> protocolMappers;
        if (protocolMapperReferenceFile == null) {
            protocolMappers = getProtocolMappers(scopeName);
            addProtocolMappersToClientScope(clientScope, protocolMappers);
        } else {
            protocolMappers = resolveProtocolMappers(protocolMapperReferenceFile);
            protocolMappers.add(getStaticClaimMapper(scopeName));
            addProtocolMappersToClientScope(clientScope, protocolMappers);
        }
        clientScope.setProtocolMappers(protocolMappers);
        return clientScope;
    }

    private ClientScopeRepresentation requireExistingClientScope(String scopeName) {

        // Check if the client scope already exists
        List<ClientScopeRepresentation> existingScopes = testRealm().clientScopes().findAll();
        for (ClientScopeRepresentation existingScope : existingScopes) {
            if (existingScope.getName().equals(scopeName)) {
                return existingScope; // Reuse existing scope
            }
        }
        fail("No such client scope: " + scopeName);
        return null;
    }

    private List<ProtocolMapperRepresentation> resolveProtocolMappers(String protocolMapperReferenceFile) {
        if (protocolMapperReferenceFile == null) {
            return null;
        }
        try (InputStream inputStream = getClass().getResourceAsStream(protocolMapperReferenceFile)) {
            return JsonSerialization.mapper.readValue(inputStream,
                    ClientScopeRepresentation.class).getProtocolMappers();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void assignOptionalClientScopeToClient(String scopeId, String clientId) {
        ClientResource clientResource = findClientByClientId(testRealm(), clientId);
        clientResource.addOptionalClientScope(scopeId);
    }

    private void logoutUser(String clientId, String username) {
        UserResource user = ApiUtil.findUserByUsernameId(adminClient.realm(TEST_REALM_NAME), username);
        user.logout();
    }

    public static JWK generateRsaJwk() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        String modulus = Base64Url.encode(publicKey.getModulus().toByteArray());
        String exponent = Base64Url.encode(publicKey.getPublicExponent().toByteArray());

        RSAPublicJWK jwk = new RSAPublicJWK();
        jwk.setKeyType("RSA");
        jwk.setPublicKeyUse("enc");
        jwk.setAlgorithm("RSA-OAEP");
        jwk.setModulus(modulus);
        jwk.setPublicExponent(exponent);

        return jwk;
    }

    public static Map<String, Object> generateRsaJwkWithPrivateKey() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        String modulus = Base64Url.encode(publicKey.getModulus().toByteArray());
        String exponent = Base64Url.encode(publicKey.getPublicExponent().toByteArray());

        RSAPublicJWK jwk = new RSAPublicJWK();
        jwk.setKeyType("RSA");
        jwk.setPublicKeyUse("enc");
        jwk.setAlgorithm("RSA-OAEP");
        jwk.setModulus(modulus);
        jwk.setPublicExponent(exponent);

        Map<String, Object> result = new HashMap<>();
        result.put("jwk", jwk);
        result.put("privateKey", privateKey);
        return result;
    }

    public static CredentialResponse decryptJweResponse(String encryptedResponse, PrivateKey privateKey) throws IOException, JWEException {
        assertNotNull("Encrypted response should not be null", encryptedResponse);
        assertEquals("Response should be a JWE", 5, encryptedResponse.split("\\.").length);

        JWE jwe = new JWE(encryptedResponse);
        jwe.getKeyStorage().setDecryptionKey(privateKey);
        jwe.verifyAndDecodeJwe();
        byte[] decryptedContent = jwe.getContent();
        return JsonSerialization.readValue(decryptedContent, CredentialResponse.class);
    }

    public static String createEncryptedCredentialRequest(String payload, KeyWrapper encryptionKey) throws Exception {
        byte[] content = payload.getBytes(StandardCharsets.UTF_8);

        JWEHeader header = new JWEHeader.JWEHeaderBuilder()
                .keyId(encryptionKey.getKid())
                .algorithm(encryptionKey.getAlgorithm())
                .encryptionAlgorithm(A256GCM)
                .type(JWT)
                .build();

        JWE jwe = new JWE()
                .header(header)
                .content(content);
        jwe.getKeyStorage().setEncryptionKey(encryptionKey.getPublicKey());
        return jwe.encodeJwe();
    }

    public static String createEncryptedCredentialRequestWithCompression(String payload, KeyWrapper encryptionKey) throws Exception {
        byte[] content = compressPayload(payload.getBytes(StandardCharsets.UTF_8));
        LOGGER.debugf("Compressed payload size: %d bytes", content.length);

        JWEHeader header = new JWEHeader.JWEHeaderBuilder()
                .keyId(encryptionKey.getKid())
                .algorithm(encryptionKey.getAlgorithm())
                .encryptionAlgorithm(A256GCM)
                .compressionAlgorithm("DEF")
                .type(JWT)
                .build();

        JWE jwe = new JWE()
                .header(header)
                .content(content);
        jwe.getKeyStorage().setEncryptionKey(encryptionKey.getPublicKey());
        return jwe.encodeJwe();
    }

    public static byte[] compressPayload(byte[] payload) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(out, new Deflater(Deflater.DEFAULT_COMPRESSION, true))) {
            deflater.write(payload);
        }
        return out.toByteArray();
    }

    void setClientOid4vciEnabled(String clientId, boolean enabled) {
        ClientRepresentation clientRepresentation = adminClient.realm(TEST_REALM_NAME).clients().findByClientId(clientId).get(0);
        ClientResource clientResource = adminClient.realm(TEST_REALM_NAME).clients().get(clientRepresentation.getId());

        Map<String, String> attributes = new HashMap<>(clientRepresentation.getAttributes() != null ? clientRepresentation.getAttributes() : Map.of());
        attributes.put("oid4vci.enabled", String.valueOf(enabled));
        clientRepresentation.setAttributes(attributes);

        clientResource.update(clientRepresentation);
    }

    // Tests the AuthZCode complete flow without scope from
    // 1. Get authorization code without scope specified by wallet
    // 2. Using the code to get access token
    // 3. Get the credential configuration id from issuer metadata at .wellKnown
    // 4. With the access token, get the credential
    protected void testCredentialIssuanceWithAuthZCodeFlow(ClientScopeRepresentation clientScope,
                                                           BiFunction<String, String, String> f,
                                                           Consumer<Map<String, Object>> c) {
        String testClientId = client.getClientId();
        String testScope = clientScope.getName();
        String testFormat = clientScope.getAttributes().get(CredentialScopeModel.FORMAT);
        String testCredentialConfigurationId = clientScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);

        try (Client client = AdminClientUtil.createResteasyClient()) {
            UriBuilder builder = UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT);
            URI oid4vciDiscoveryUri = RealmsResource.wellKnownProviderUrl(builder)
                    .build(TEST_REALM_NAME,
                            OID4VCIssuerWellKnownProviderFactory.PROVIDER_ID);
            WebTarget oid4vciDiscoveryTarget = client.target(oid4vciDiscoveryUri);

            // 1. Get authoriZation code without scope specified by wallet
            // 2. Using the code to get accesstoken
            String token = f.apply(testClientId, testScope);

            // 3. Get the credential configuration id from issuer metadata at .wellKnown
            try (Response discoveryResponse = oid4vciDiscoveryTarget.request().get()) {
                CredentialIssuer oid4vciIssuerConfig = JsonSerialization.readValue(discoveryResponse.readEntity(String.class),
                        CredentialIssuer.class);
                assertEquals(200, discoveryResponse.getStatus());
                assertEquals(getRealmPath(TEST_REALM_NAME), oid4vciIssuerConfig.getCredentialIssuer());
                assertEquals(getBasePath(TEST_REALM_NAME) + "credential", oid4vciIssuerConfig.getCredentialEndpoint());

                // 4. With the access token, get the credential
                try (Client clientForCredentialRequest = AdminClientUtil.createResteasyClient()) {
                    UriBuilder credentialUriBuilder = UriBuilder.fromUri(oid4vciIssuerConfig.getCredentialEndpoint());
                    URI credentialUri = credentialUriBuilder.build();
                    WebTarget credentialTarget = clientForCredentialRequest.target(credentialUri);

                    CredentialRequest request = new CredentialRequest();
                    request.setCredentialConfigurationId(oid4vciIssuerConfig.getCredentialsSupported()
                            .get(testCredentialConfigurationId)
                            .getId());

                    assertEquals(testFormat,
                            oid4vciIssuerConfig.getCredentialsSupported()
                                    .get(testCredentialConfigurationId)
                                    .getFormat());
                    assertEquals(testCredentialConfigurationId,
                            oid4vciIssuerConfig.getCredentialsSupported()
                                    .get(testCredentialConfigurationId)
                                    .getId());

                    c.accept(Map.of(
                            "accessToken", token,
                            "credentialTarget", credentialTarget,
                            "credentialRequest", request
                    ));
                }
            }
        } catch (IOException e) {
            Assert.fail();
        }
    }

    protected String getBasePath(String realm) {
        return getRealmPath(realm) + "/protocol/oid4vc/";
    }

    protected String getRealmPath(String realm) {
        return suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/" + realm;
    }

    protected void requestCredential(String token,
                                     String credentialEndpoint,
                                     SupportedCredentialConfiguration offeredCredential,
                                     CredentialResponseHandler responseHandler,
                                     ClientScopeRepresentation expectedClientScope) throws IOException, VerificationException {
        CredentialRequest request = new CredentialRequest();
        request.setCredentialConfigurationId(offeredCredential.getId());

        StringEntity stringEntity = new StringEntity(JsonSerialization.writeValueAsString(request),
                ContentType.APPLICATION_JSON);

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
        final String endpoint = getRealmPath(TEST_REALM_NAME) + "/.well-known/openid-credential-issuer";
        HttpGet getMetadataRequest = new HttpGet(endpoint);
        try (CloseableHttpResponse metadataResponse = httpClient.execute(getMetadataRequest)) {
            assertEquals(HttpStatus.SC_OK, metadataResponse.getStatusLine().getStatusCode());
            String s = IOUtils.toString(metadataResponse.getEntity().getContent(), StandardCharsets.UTF_8);
            return JsonSerialization.readValue(s, CredentialIssuer.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        if (testRealm.getComponents() == null) {
            testRealm.setComponents(new MultivaluedHashMap<>());
        }

        testRealm.getComponents().add("org.keycloak.keys.KeyProvider", getKeyProvider());

        testRealm.getComponents().add("org.keycloak.keys.KeyProvider",
                getRsaEncKeyProvider(RSA_OAEP_256, "enc-key-oaep256", 100));
        testRealm.getComponents().add("org.keycloak.keys.KeyProvider",
                getRsaEncKeyProvider(RSA_OAEP, "enc-key-oaep", 101));

        // Find existing client representation
        ClientRepresentation existingClient = testRealm.getClients().stream()
                .filter(client -> client.getClientId().equals(clientId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Client with ID " + clientId + " not found in realm"));

        // Add a role to an existing client
        if (testRealm.getRoles() != null) {
            Map<String, List<RoleRepresentation>> clientRoles = testRealm.getRoles().getClient();
            clientRoles.merge(
                    existingClient.getClientId(),
                    List.of(getRoleRepresentation("testRole", existingClient.getClientId())),
                    (existingRoles, newRoles) -> {
                        List<RoleRepresentation> mergedRoles = new ArrayList<>(existingRoles);
                        mergedRoles.addAll(newRoles);
                        return mergedRoles;
                    }
            );
        } else {
            testRealm.getRoles()
                    .setClient(Map.of(existingClient.getClientId(),
                            List.of(getRoleRepresentation("testRole", existingClient.getClientId()))));
        }

        List<UserRepresentation> realmUsers = Optional.ofNullable(testRealm.getUsers()).map(ArrayList::new)
                .orElse(new ArrayList<>());
        realmUsers.add(getUserRepresentation(Map.of(existingClient.getClientId(), List.of("testRole"))));
        testRealm.setUsers(realmUsers);
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

    protected ComponentExportRepresentation getKeyProvider() {
        return getRsaKeyProvider(RSA_KEY);
    }

    protected static class CredentialResponseHandler {
        protected void handleCredentialResponse(CredentialResponse credentialResponse,
                                                ClientScopeRepresentation clientScope) throws VerificationException {
            assertNotNull("The credentials array should be present in the response.", credentialResponse.getCredentials());
            assertFalse("The credentials array should not be empty.", credentialResponse.getCredentials().isEmpty());

            // Get the first credential from the array (maintaining compatibility with single credential tests)
            CredentialResponse.Credential credentialObj = credentialResponse.getCredentials().get(0);
            assertNotNull("The first credential in the array should not be null.", credentialObj);

            JsonWebToken jsonWebToken = TokenVerifier.create((String) credentialObj.getCredential(),
                    JsonWebToken.class).getToken();
            assertEquals("did:web:test.org", jsonWebToken.getIssuer());
            VerifiableCredential credential = JsonSerialization.mapper.convertValue(jsonWebToken.getOtherClaims().get(
                    "vc"), VerifiableCredential.class);
            assertEquals(List.of(clientScope.getName()), credential.getType());
            assertEquals(URI.create("did:web:test.org"), credential.getIssuer());
            assertEquals("john@email.cz", credential.getCredentialSubject().getClaims().get("email"));
            assertTrue("The static claim should be set.",
                    credential.getCredentialSubject().getClaims().containsKey("scope-name"));
            assertEquals("The static claim should be set.",
                    clientScope.getName(),
                    credential.getCredentialSubject().getClaims().get("scope-name"));
            assertFalse("Only mappers supported for the requested type should have been evaluated.",
                    credential.getCredentialSubject().getClaims().containsKey("AnotherCredentialType"));
        }
    }

    protected List<OID4VCAuthorizationDetailsResponse> parseAuthorizationDetails(String responseBody) throws IOException {
        Map<String, Object> responseMap = JsonSerialization.readValue(responseBody, new TypeReference<Map<String, Object>>() {
        });
        Object authDetailsObj = responseMap.get("authorization_details");
        assertNotNull("authorization_details should be present in the response", authDetailsObj);
        return JsonSerialization.readValue(
                JsonSerialization.writeValueAsString(authDetailsObj),
                new TypeReference<List<OID4VCAuthorizationDetailsResponse>>() {
                }
        );
    }

    protected String getAccessToken(String responseBody) throws IOException {
        Map<String, Object> responseMap = JsonSerialization.readValue(responseBody, new TypeReference<Map<String, Object>>() {
        });
        String token = (String) responseMap.get("access_token");
        assertNotNull("Access token should be present", token);
        return token;
    }
}
