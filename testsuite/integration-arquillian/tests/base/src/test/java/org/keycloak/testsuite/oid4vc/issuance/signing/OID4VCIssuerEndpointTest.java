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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.OAuth2Constants;
import org.keycloak.OID4VCConstants.KeyAttestationResistanceLevels;
import org.keycloak.TokenVerifier;
import org.keycloak.VCFormat;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.VerificationException;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwe.JWE;
import org.keycloak.jose.jwe.JWEException;
import org.keycloak.jose.jwe.JWEHeader;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.RSAPublicJWK;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.protocol.oid4vc.issuance.JWTVCIssuerWellKnownProviderFactory;
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
import org.keycloak.protocol.oid4vc.model.SupportedCredentialConfiguration;
import org.keycloak.protocol.oid4vc.model.VerifiableCredential;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.protocol.oidc.utils.OAuth2Code;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.runonserver.RunOnServerException;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.oauth.OpenIDProviderConfigurationResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialIssuerMetadataResponse;
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
import org.junit.Before;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_VC;
import static org.keycloak.OID4VCConstants.OID4VCI_ENABLED_ATTRIBUTE_KEY;
import static org.keycloak.constants.OID4VCIConstants.CREDENTIAL_OFFER_CREATE;
import static org.keycloak.jose.jwe.JWEConstants.A256GCM;
import static org.keycloak.jose.jwe.JWEConstants.RSA_OAEP;
import static org.keycloak.jose.jwe.JWEConstants.RSA_OAEP_256;
import static org.keycloak.models.Constants.CREATE_DEFAULT_CLIENT_SCOPES;
import static org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint.CREDENTIAL_OFFER_URI_CODE_SCOPE;
import static org.keycloak.protocol.oid4vc.model.ProofType.JWT;
import static org.keycloak.userprofile.DeclarativeUserProfileProvider.UP_COMPONENT_CONFIG_KEY;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_ADMIN;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_USER;
import static org.keycloak.util.JsonSerialization.valueAsPrettyString;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Moved test to subclass. so we can reuse initialization code.
 */
public abstract class OID4VCIssuerEndpointTest extends OID4VCTest {

    private static final Logger LOGGER = Logger.getLogger(OID4VCIssuerEndpointTest.class);

    protected static final TimeProvider TIME_PROVIDER = new OID4VCTest.StaticTimeProvider(1000);
    protected static final String sdJwtCredentialVct = "https://credentials.example.com/SD-JWT-Credential";

    protected ClientScopeRepresentation sdJwtTypeCredentialClientScope;
    protected ClientScopeRepresentation jwtTypeCredentialClientScope;
    protected ClientScopeRepresentation minimalJwtTypeCredentialClientScope;

    protected CloseableHttpClient httpClient;
    protected ClientRepresentation client;

    record OAuth2CodeEntry(String key, OAuth2Code code) {}

    protected boolean shouldEnableOid4vci(RealmRepresentation testRealm) {
        return true;
    }

    protected boolean shouldEnableOid4vci(ClientRepresentation testClient) {
        return true;
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        CryptoIntegration.init(this.getClass().getClassLoader());

        testRealm.setVerifiableCredentialsEnabled(shouldEnableOid4vci(testRealm));

        if (testRealm.getComponents() == null) {
            testRealm.setComponents(new MultivaluedHashMap<>());
        }

        // Add key providers
        testRealm.getComponents().add("org.keycloak.keys.KeyProvider",
                getKeyProvider());
        testRealm.getComponents().add("org.keycloak.keys.KeyProvider",
                getRsaEncKeyProvider(RSA_OAEP_256, "enc-key-oaep256", 100));
        testRealm.getComponents().add("org.keycloak.keys.KeyProvider",
                getRsaEncKeyProvider(RSA_OAEP, "enc-key-oaep", 101));

        // Add Did attribute to the user profile
        testRealm.getComponents().add("org.keycloak.userprofile.UserProfileProvider",
                getUserProfileProvider());

        // Add a role representations
        //
        RolesRepresentation realmRoles = testRealm.getRoles();
        realmRoles.getRealm().add(CREDENTIAL_OFFER_CREATE);
        realmRoles.getClient().get(clientId).add(getRoleRepresentation("testRole", clientId));

        // Add user representations
        //
        Map<String, List<String>> clientRoles = Map.of(clientId, List.of("testRole"));
        List<UserRepresentation> realmUsers = Optional.ofNullable(testRealm.getUsers()).map(ArrayList::new).orElse(new ArrayList<>());
        realmUsers.add(getUserRepresentation("John Doe", Map.of("did", "did:key:1234"), List.of(CREDENTIAL_OFFER_CREATE.getName()), clientRoles));
        realmUsers.add(getUserRepresentation("Alice Wonderland", Map.of("did", "did:key:5678"), List.of(), Map.of()));
        testRealm.setUsers(realmUsers);

        // Allow the default client scopes to be added as well
        Map<String, String> realmAttributes = Optional.ofNullable(testRealm.getAttributes()).orElse(new HashMap<>());
        realmAttributes.put(CREATE_DEFAULT_CLIENT_SCOPES, String.valueOf(true));
        testRealm.setAttributes(realmAttributes);

        // Add additional client scopes
        //
        List<ClientScopeRepresentation> clientScopes = Optional.ofNullable(testRealm.getClientScopes()).orElse(new ArrayList<>());
        clientScopes.add(createOptionalClientScope(sdJwtTypeCredentialScopeName,
                null,
                sdJwtTypeCredentialConfigurationIdName,
                sdJwtTypeCredentialScopeName,
                sdJwtCredentialVct,
                VCFormat.SD_JWT_VC,
                null,
                List.of(KeyAttestationResistanceLevels.HIGH, KeyAttestationResistanceLevels.MODERATE))
        );
        clientScopes.add(createOptionalClientScope(jwtTypeCredentialScopeName,
                TEST_DID.toString(),
                jwtTypeCredentialConfigurationIdName,
                jwtTypeCredentialScopeName,
                null,
                VCFormat.JWT_VC,
                TEST_CREDENTIAL_MAPPERS_FILE,
                Collections.emptyList())
        );
        clientScopes.add(createOptionalClientScope(minimalJwtTypeCredentialScopeName,
                null,
                null,
                null,
                null,
                null,
                null, null)
        );
        testRealm.setClientScopes(clientScopes);

        // Enable oid4vci in test clients
        for (String cid : List.of(clientId)) {

            ClientRepresentation testClient = testRealm.getClients().stream()
                    .filter(c -> c.getClientId().equals(cid))
                    .findFirst().orElseThrow(() -> new IllegalStateException("Client with clientId=" + cid + " not found in realm"));

            // Enable oid4vci on the client
            Map<String, String> attributes = Optional.ofNullable(testClient.getAttributes()).orElse(new HashMap<>());
            attributes.put(OID4VCI_ENABLED_ATTRIBUTE_KEY, String.valueOf(shouldEnableOid4vci(testClient)));
            testClient.setAttributes(attributes);

            // Assign default client scopes
            List<String> defaultClientScopes = new ArrayList<>(Optional.ofNullable(testClient.getDefaultClientScopes()).orElse(List.of()));
            defaultClientScopes.addAll(List.of("web-origins", "acr", "roles", "profile", "basic", "email"));
            testClient.setDefaultClientScopes(defaultClientScopes);

            // Assign optional client scopes
            List<String> optionalClientScopes = new ArrayList<>(Optional.ofNullable(testClient.getOptionalClientScopes()).orElse(List.of()));
            // Realm import does not assign the default optional scopes
            // optionalClientScopes.addAll(List.of("address", "phone", "offline_access", "organization", "microprofile-jwt"));
            optionalClientScopes.addAll(clientScopes.stream().map(ClientScopeRepresentation::getName).toList());
            optionalClientScopes.addAll(List.of(jwtTypeNaturalPersonScopeName, sdJwtTypeNaturalPersonScopeName));
            testClient.setOptionalClientScopes(optionalClientScopes);
        }
    }

    @Before
    public void setup() {
        httpClient = HttpClientBuilder.create().build();
        client = requireExistingClient(clientId);

        // Lookup additional client scopes
        sdJwtTypeCredentialClientScope = requireExistingClientScope(sdJwtTypeCredentialScopeName);
        jwtTypeCredentialClientScope = requireExistingClientScope(jwtTypeCredentialScopeName);
        minimalJwtTypeCredentialClientScope = requireExistingClientScope(minimalJwtTypeCredentialScopeName);
    }

    protected static OAuth2CodeEntry prepareSessionCode(KeycloakSession session, AppAuthManager.BearerTokenAuthenticator authenticator, String note) {
        AuthenticationManager.AuthResult authResult = authenticator.authenticate();
        UserSessionModel userSessionModel = authResult.session();
        AuthenticatedClientSessionModel authenticatedClientSessionModel = userSessionModel.getAuthenticatedClientSessionByClient(
                authResult.client().getId());
        OAuth2Code oauth2Code = new OAuth2Code(
                SecretGenerator.getInstance().randomString(),
                Time.currentTime() + 6000,
                SecretGenerator.getInstance().randomString(),
                CREDENTIAL_OFFER_URI_CODE_SCOPE,
                authenticatedClientSessionModel.getUserSession().getId());

        String nonce = OAuth2CodeParser.persistCode(session, authenticatedClientSessionModel, oauth2Code);
        authenticatedClientSessionModel.setNote(nonce, note);
        return new OAuth2CodeEntry(nonce, oauth2Code);
    }

    protected static OID4VCIssuerEndpoint prepareIssuerEndpoint(KeycloakSession session,
                                                                AppAuthManager.BearerTokenAuthenticator authenticator) {
        JwtCredentialBuilder jwtCredentialBuilder = new JwtCredentialBuilder(
                new StaticTimeProvider(1000),
                session);
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
            Map<String, CredentialBuilder> credentialBuilders
    ) {
        return new OID4VCIssuerEndpoint(
                session,
                credentialBuilders,
                authenticator,
                TIME_PROVIDER,
                30);
    }

    protected ClientScopeRepresentation createOptionalClientScope(String scopeName,
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
        if (acceptedKeyAttestationValues != null) {
            attributes.put(CredentialScopeModel.KEY_ATTESTATION_REQUIRED, "true");
            if (!acceptedKeyAttestationValues.isEmpty()) {
                attributes.put(CredentialScopeModel.KEY_ATTESTATION_REQUIRED_KEY_STORAGE,
                        String.join(",", acceptedKeyAttestationValues));
                attributes.put(CredentialScopeModel.KEY_ATTESTATION_REQUIRED_USER_AUTH,
                        String.join(",", acceptedKeyAttestationValues));
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
        try (Response res = testRealm().clientScopes().create(clientScope)) {
            String scopeId = ApiUtil.getCreatedId(res);
            getCleanup().addClientScopeId(scopeId);
            clientScope.setId(scopeId);
        }
        return clientScope;
    }

    protected ClientRepresentation requireExistingClient(String clientId) {
        List<ClientRepresentation> clientRepresentations = testRealm().clients().findByClientId(clientId);
        assertFalse("No such client", clientRepresentations.isEmpty());
        return clientRepresentations.get(0);
    }

    protected ClientScopeRepresentation requireExistingClientScope(String scopeName) {

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

    protected void assignOptionalClientScope(ClientRepresentation testClient, String scopeName) {
        ClientScopeRepresentation clientScope = requireExistingClientScope(scopeName);
        assignOptionalClientScope(testClient, clientScope);
    }

    protected void assignOptionalClientScope(ClientRepresentation client, ClientScopeRepresentation clientScope) {
        ClientResource clientResource = testRealm().clients().get(client.getId());
        clientResource.addOptionalClientScope(clientScope.getId());
    }

    protected void logoutUser(String username) {
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

    protected void setOid4vciEnabled(ClientRepresentation testClient, boolean enabled) {
        ClientResource clientResource = testRealm().clients().get(testClient.getId());

        Map<String, String> attributes = Optional.ofNullable(testClient.getAttributes()).orElse(new HashMap<>());
        attributes.put(OID4VCI_ENABLED_ATTRIBUTE_KEY, String.valueOf(enabled));
        testClient.setAttributes(attributes);

        clientResource.update(testClient);
    }

    // Tests the AuthZCode complete flow without scope from
    // 1. Get authorization code without scope specified by wallet
    // 2. Using the code to get access token
    // 3. Get the credential configuration id from issuer metadata at .wellKnown
    // 4. With the access token, get the credential
    protected void testCredentialIssuanceWithAuthZCodeFlow(ClientScopeRepresentation clientScope,
                                                           BiFunction<String, String, String> f,
                                                           Consumer<Map<String, Object>> c) {
        String testScope = clientScope.getName();
        String testFormat = clientScope.getAttributes().get(CredentialScopeModel.FORMAT);
        String testCredentialConfigurationId = clientScope.getAttributes().get(CredentialScopeModel.CONFIGURATION_ID);

        try (Client client = AdminClientUtil.createResteasyClient()) {
            String metadataUrl = getRealmMetadataPath(TEST_REALM_NAME);
            WebTarget oid4vciDiscoveryTarget = client.target(metadataUrl);

            // 1. Get authoriZation code without scope specified by wallet
            // 2. Using the code to get the AccessToken
            String token = f.apply(clientId, testScope);

            // Extract credential_identifier from the token (client-side parsing)
            String credentialIdentifier = null;
            try {
                JsonWebToken jwt = new JWSInput(token).readJsonContent(JsonWebToken.class);
                Object authDetails = jwt.getOtherClaims().get(OAuth2Constants.AUTHORIZATION_DETAILS);
                if (authDetails != null) {
                    List<OID4VCAuthorizationDetail> authDetailsResponse = JsonSerialization.readValue(
                            JsonSerialization.writeValueAsString(authDetails),
                            new TypeReference<List<OID4VCAuthorizationDetail>>() {
                            }
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
                    // Use credential_identifier if available, otherwise use configuration_id for error testing
                    if (credentialIdentifier != null) {
                        request.setCredentialIdentifier(credentialIdentifier);
                    } else {
                        request.setCredentialConfigurationId(testCredentialConfigurationId);
                    }

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
        return suiteContext.getAuthServerInfo().getContextRoot() + "/auth/realms/" + realm;
    }

    protected String getRealmMetadataPath(String realm) {
        var contextRoot = suiteContext.getAuthServerInfo().getContextRoot();
        // [TODO] This should be contextRoot/.well-known/openid-credential-issuer/auth/realms/...
        return contextRoot + "/auth/.well-known/openid-credential-issuer/realms/" + realm;
    }

    protected String getSpecCompliantRealmMetadataPath(String realm) {
        var contextRoot = suiteContext.getAuthServerInfo().getContextRoot();
        // [TODO] This should be contextRoot/.well-known/jwt-vc-issuer/auth/realms/...
        return contextRoot + "/auth/.well-known/" + JWTVCIssuerWellKnownProviderFactory.PROVIDER_ID + "/realms/" + realm;
    }

    protected String getLegacyJwtVcRealmMetadataPath(String realm) {
        var contextRoot = suiteContext.getAuthServerInfo().getContextRoot();
        return contextRoot + "/auth/realms/" + realm + "/.well-known/" + JWTVCIssuerWellKnownProviderFactory.PROVIDER_ID;
    }

    protected String getCredentialOfferUrl(String nonce) {
        return getBasePath("test") + "credential-offer/" + nonce;
    }

    protected void requestCredentialWithIdentifier(String token,
                                                   String credentialEndpoint,
                                                   String credentialIdentifier,
                                                   CredentialResponseHandler responseHandler,
                                                   ClientScopeRepresentation expectedClientScope) throws IOException, VerificationException {
        CredentialRequest request = new CredentialRequest();
        request.setCredentialIdentifier(credentialIdentifier);

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
        CredentialIssuerMetadataResponse metadataResponse = oauth.oid4vc()
                .doIssuerMetadataRequest();
        return metadataResponse.getMetadata();
    }

    public OIDCConfigurationRepresentation getAuthorizationMetadata(String authServerUrl) {
        OpenIDProviderConfigurationResponse response = oauth.wellknownRequest()
                .url(authServerUrl)
                .send();
        assertEquals(HttpStatus.SC_OK, response.getStatusCode());
        return response.getOidcConfiguration();
    }

    public SupportedCredentialConfiguration getSupportedCredentialConfigurationByScope(CredentialIssuer metadata, String scope) {
        SupportedCredentialConfiguration result = metadata.getCredentialsSupported().values().stream()
                .filter(it -> it.getScope().equals(scope))
                .findFirst().orElse(null);
        return result;
    }

    private ComponentExportRepresentation getUserProfileProvider() {

        // Add the User DID attribute, with the same logic as in DeclarativeUserProfileProviderFactory
        //
        UPConfig profileConfig = UPConfigUtils.parseSystemDefaultConfig();
        if (profileConfig.getAttribute(UserModel.DID) == null) {
            UPAttribute attr = new UPAttribute(UserModel.DID);
            attr.setDisplayName("${did}");
            attr.setPermissions(new UPAttributePermissions(Set.of(ROLE_ADMIN, ROLE_USER), Set.of(ROLE_ADMIN, ROLE_USER)));
            attr.setValidations(Map.of(PatternValidator.ID, Map.of(
                    "pattern", "^did:.+:.+$",
                    "error-message", "Value must start with 'did:scheme:'")));
            profileConfig.addOrReplaceAttribute(attr);
        }

        ComponentExportRepresentation componentExportRepresentation = new ComponentExportRepresentation();
        componentExportRepresentation.setProviderId("declarative-user-profile");
        componentExportRepresentation.setConfig(new MultivaluedHashMap<>(
                Map.of(UP_COMPONENT_CONFIG_KEY, List.of(JsonSerialization.valueAsString(profileConfig)))));

        return componentExportRepresentation;
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
        final Logger log = Logger.getLogger(OID4VCIssuerEndpointTest.class);

        protected void handleCredentialResponse(CredentialResponse credentialResponse,
                                                ClientScopeRepresentation clientScope) throws VerificationException {

            assertNotNull("The credentials array should be present in the response.", credentialResponse.getCredentials());
            assertFalse("The credentials array should not be empty.", credentialResponse.getCredentials().isEmpty());

            // Get the first credential from the array (maintaining compatibility with single credential tests)
            CredentialResponse.Credential credentialObj = credentialResponse.getCredentials().get(0);
            assertNotNull("The first credential in the array should not be null.", credentialObj);

            JsonWebToken jsonWebToken = TokenVerifier.create((String) credentialObj.getCredential(), JsonWebToken.class).getToken();
            Map<String, Object> otherClaims = jsonWebToken.getOtherClaims();

            log.infof("JsonWebToken: %s", valueAsPrettyString(jsonWebToken));
            assertNotNull("Expected jti claim", jsonWebToken.getId());
            assertNotNull("Expected exp claim", jsonWebToken.getExp());
            assertNotNull("Expected nbf claim", jsonWebToken.getNbf());
            assertNotNull("Expected iss claim", jsonWebToken.getIssuer());
            assertNotNull("Expected sub claim", jsonWebToken.getSubject());

            assertNull("Unexpected aud claim", jsonWebToken.getAudience());
            assertNull("Unexpected iat claim", jsonWebToken.getIat());

            assertEquals("did:web:test.org", jsonWebToken.getIssuer());
            assertEquals(Set.of(CLAIM_NAME_VC), otherClaims.keySet());

            @SuppressWarnings("unchecked") Map<String, ?> vc = (Map<String, ?>) otherClaims.get("vc");
            VerifiableCredential credential = JsonSerialization.mapper.convertValue(vc, VerifiableCredential.class);
            Map<String, ?> subClaims = credential.getCredentialSubject().getClaims();

            assertNotNull("Expected vc.issuanceDate claim", credential.getIssuanceDate());
            assertNotNull("Expected vc.expirationDate claim", credential.getExpirationDate());
            assertNotNull("Expected vc.@context claim", credential.getContext());

            assertEquals("vc.type mapped correctly", List.of(clientScope.getName()), credential.getType());
            assertEquals("iss mapped correctly", "did:web:test.org", jsonWebToken.getIssuer());
            assertEquals("vc.issuer mapped correctly", URI.create("did:web:test.org"), credential.getIssuer());
            assertEquals("vc.credentialSubject.id mapped correctly", jsonWebToken.getSubject(), subClaims.get("id"));
            assertEquals("vc.credentialSubject.given_name mapped correctly", "John", subClaims.get("given_name"));
            assertEquals("vc.credentialSubject.email mapped correctly", "john@email.cz", subClaims.get("email"));
            assertEquals("vc.credentialSubject.scope-name mapped correctly", clientScope.getName(), subClaims.get("scope-name"));
            assertThat("vc.credentialSubject.address is parent claim for nested claims", subClaims.get("address"), instanceOf(Map.class));
            Map<String, ?> nestedAddressClaim = (Map<String, ?>) subClaims.get("address");
            assertThat("vc.credentialSubject.address contains two nested claims", nestedAddressClaim, aMapWithSize(2));
            assertEquals("vc.credentialSubject.address.street_address mapped correctly", "221B Baker Street", nestedAddressClaim.get("street_address"));
            assertEquals("vc.credentialSubject.address.locality mapped correctly", "London", nestedAddressClaim.get("locality"));

            assertFalse("Unexpected other claim", subClaims.containsKey("AnotherCredentialType"));
        }
    }

    protected List<OID4VCAuthorizationDetail> parseAuthorizationDetails(String responseBody) throws IOException {
        Map<String, Object> responseMap = JsonSerialization.readValue(responseBody, new TypeReference<Map<String, Object>>() {
        });
        Object authDetailsObj = responseMap.get("authorization_details");
        assertNotNull("authorization_details should be present in the response", authDetailsObj);
        return JsonSerialization.readValue(
                JsonSerialization.writeValueAsString(authDetailsObj),
                new TypeReference<List<OID4VCAuthorizationDetail>>() {
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
