package org.keycloak.tests.oid4vc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.keycloak.OID4VCConstants;
import org.keycloak.VCFormat;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.Profile;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.events.EventType;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.oid4vci.CredentialScopeModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oid4vc.issuance.OID4VCAuthorizationDetailsParser;
import org.keycloak.protocol.oid4vc.issuance.TimeProvider;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCGeneratedIdMapper;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCIssuedAtTimeClaimMapper;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.protocol.oid4vc.model.DisplayObject;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.UserConfigBuilder;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.util.oauth.AccessTokenRequest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.AuthorizationDetailsParser;
import org.keycloak.util.JsonSerialization;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_SUBJECT_ID;
import static org.keycloak.OID4VCConstants.OID4VCI_ENABLED_ATTRIBUTE_KEY;
import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;
import static org.keycloak.constants.OID4VCIConstants.CREDENTIAL_OFFER_CREATE;
import static org.keycloak.models.Constants.CREATE_DEFAULT_CLIENT_SCOPES;
import static org.keycloak.models.oid4vci.CredentialScopeModel.VC_FORMAT_DEFAULT;

/**
 * Abstract base class for OID4VCI Testing
 * <p>
 * Server-side debugging: KC_TEST_SERVER=embedded
 */
public abstract class OID4VCIssuerTestBase {

    protected final Logger log = Logger.getLogger(getClass());

    public static final String OID4VCI_CLIENT_ID = "oid4vci-test";
    public static final String OID4VCI_PUBLIC_CLIENT_ID = "oid4vci-test-pub";
    public static final URI ISSUER_DID = URI.create("did:web:test.org");
    public static final String TEST_CREDENTIAL_MAPPERS_FILE = "/oid4vc/test-credential-mappers.json";
    public static final String TEST_USER = "john";
    public static final String TEST_PASSWORD = "password";

    public static final String sdJwtTypeCredentialScopeName = "sd-jwt-credential";
    public static final String sdJwtTypeCredentialConfigurationIdName = "sd-jwt-credential-config-id";
    public static final String sdJwtTypeCredentialVct = "https://credentials.example.com/SD-JWT-Credential";

    public static final String jwtTypeCredentialScopeName = "jwt-credential";
    public static final String jwtTypeCredentialConfigurationIdName = "jwt-credential-config-id";
    public static final String minimalJwtTypeCredentialScopeName = "vc-with-minimal-config";
    public static final String minimalJwtTypeCredentialConfigurationIdName = "vc-with-minimal-config-id";

    @InjectRealm(config = VCTestRealmConfig.class)
    protected ManagedRealm testRealm;

    @InjectClient(ref = "oid4vci-client", config = ConfidentialOID4VCIClient.class)
    protected ManagedClient managedClient;

    @InjectClient(ref = OID4VCI_PUBLIC_CLIENT_ID, config = PublicOID4VCIClient.class)
    ManagedClient managedPublicClient;

    @InjectOAuthClient
    protected OAuthClient oauth;

    @InjectTimeOffSet
    protected TimeOffSet timeOffSet;

    @InjectEvents
    protected Events events;

    @InjectWebDriver
    protected ManagedWebDriver driver;

    @InjectAdminClient
    protected Keycloak keycloak;

    @InjectKeycloakUrls
    protected KeycloakUrls keycloakUrls;

    protected CredentialScopeRepresentation minimalJwtTypeCredentialScope;
    protected CredentialScopeRepresentation jwtTypeCredentialScope;
    protected CredentialScopeRepresentation sdJwtTypeCredentialScope;

    protected String clientId = "test-app";
    protected ClientRepresentation client;
    protected ClientRepresentation pubClient;
    protected OID4VCBasicWallet wallet;

    @TestSetup
    public void configureTestRealm() {
        RealmResource realmResource = testRealm.admin();
        UPConfig upConfig = realmResource.users().userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ADMIN_EDIT);
        realmResource.users().userProfile().update(upConfig);

        AuthorizationDetailsParser.registerParser(OPENID_CREDENTIAL, new OID4VCAuthorizationDetailsParser());
    }

    @BeforeEach
    void beforeEachBase() {
        client = managedClient.admin().toRepresentation();
        pubClient = managedPublicClient.admin().toRepresentation();

        jwtTypeCredentialScope = requireExistingCredentialScope(jwtTypeCredentialScopeName);
        minimalJwtTypeCredentialScope = requireExistingCredentialScope(minimalJwtTypeCredentialScopeName);
        sdJwtTypeCredentialScope = requireExistingCredentialScope(sdJwtTypeCredentialScopeName);

        oauth.client(client.getClientId(), client.getSecret());
        enableVerifiableCredentialEvents(testRealm);

        wallet = new OID4VCBasicWallet(keycloak, oauth);
    }

    @AfterEach
    void afterEachBase() {
        wallet.logout();
        driver.cookies().deleteAll();
        driver.open("about:blank");
    }

    protected CredentialScopeRepresentation getExistingCredentialScope(String scopeName) {
        return testRealm.admin().clientScopes().findAll().stream()
                .filter(it -> scopeName.equals(it.getName()))
                .map(CredentialScopeRepresentation::new)
                .findFirst()
                .orElse(null);
    }

    protected UserRepresentation getExistingUser(String username) {
        return testRealm.admin().users().search(username).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No such user: " + username));
    }

    protected KeyWrapper getRsaKey(KeyUse keyUse, String algorithm, String keyName) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            var keyPair = kpg.generateKeyPair();

            KeyWrapper kw = new KeyWrapper();
            kw.setPrivateKey(keyPair.getPrivate());
            kw.setPublicKey(keyPair.getPublic());
            kw.setUse(keyUse);
            kw.setKid(keyName != null ? keyName : KeyUtils.createKeyId(keyPair.getPublic()));
            kw.setType("RSA");
            kw.setAlgorithm(algorithm);

            return kw;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    protected ComponentRepresentation getRsaKeyProvider(KeyWrapper keyWrapper) {
        return createRsaKeyProviderComponent(keyWrapper, "rsa-key-provider", 0);
    }

    protected ComponentRepresentation getRsaEncKeyProvider(String algorithm, String keyName, int priority) {
        KeyWrapper keyWrapper = getRsaKey(KeyUse.ENC, algorithm, keyName);
        return createRsaKeyProviderComponent(keyWrapper, keyName, priority);
    }

    protected KeyWrapper getRsaKey_Default() {
        return getRsaKey(KeyUse.SIG, "RS256", null);
    }

    protected ComponentRepresentation getAesKeyProvider(String algorithm, String keyName, String keyUse, String providerId) {
        // Generate a random AES key (default length: 256 bits)
        byte[] secret = SecretGenerator.getInstance().randomBytes(32); // 32 bytes = 256 bits
        String secretBase64 = Base64.getEncoder().encodeToString(secret);

        ComponentRepresentation component = new ComponentRepresentation();
        component.setProviderType(KeyProvider.class.getName());
        component.setName(keyName);
        component.setId(UUID.randomUUID().toString());
        component.setProviderId(providerId);

        component.setConfig(new MultivaluedHashMap<>(Map.of(
                "secret", List.of(secretBase64),
                "active", List.of("true"),
                "priority", List.of(String.valueOf(100)),
                "enabled", List.of("true"),
                "algorithm", List.of(algorithm),
                "keyUse", List.of(keyUse) // encryption usage
        )));

        return component;
    }

    protected String getBearerToken(OAuthClient oauthClient) {
        return getBearerToken(oauthClient, null);
    }

    protected String getBearerToken(OAuthClient oauthClient, ClientRepresentation client) {
        return getBearerToken(oauthClient, client, null);
    }

    protected String getBearerToken(OAuthClient oauthClient, ClientRepresentation client, String scope) {
        return getBearerToken(oauthClient, client, "john", scope);
    }

    protected String getBearerToken(OAuthClient oauthClient, ClientRepresentation client, String username, String scope) {
        return getBearerTokenCodeFlow(oauthClient, client, username, scope).getAccessToken();
    }

    protected AccessTokenResponse getBearerTokenCodeFlow(OAuthClient oauthClient, ClientRepresentation client, String username, String scope) {
        var authCode = getAuthorizationCode(oauthClient, client, username, scope);
        return oauthClient.accessTokenRequest(authCode).send();
    }

    protected String getAuthorizationCode(OAuthClient oAuthClient, ClientRepresentation client, String username, String scope) {
        var authorizationEndpointResponse = getAuthorizationResponse(oAuthClient, client, username, scope);
        return authorizationEndpointResponse.getCode();
    }

    protected AuthorizationEndpointResponse getAuthorizationResponse(OAuthClient oAuthClient, ClientRepresentation client, String username, String scope) {
        if (client != null) {
            if (client.getSecret() != null) {
                oAuthClient.client(client.getClientId(), client.getSecret());
            } else {
                oAuthClient.client(client.getClientId());
            }
        }
        if (scope != null) {
            oAuthClient.scope(scope);
        }
        var authorizationEndpointResponse = oAuthClient.doLogin(username, "password");
        return authorizationEndpointResponse;
    }

    protected AccessTokenResponse getBearerToken(OAuthClient oauthClient, String authCode, OID4VCAuthorizationDetail... authDetail) {
        AccessTokenRequest accessTokenRequest = oauthClient.accessTokenRequest(authCode);
        if (authDetail != null && authDetail.length > 0) {
            accessTokenRequest.authorizationDetails(Arrays.asList(authDetail));
        }
        AccessTokenResponse tokenResponse = accessTokenRequest.send();
        if (!tokenResponse.isSuccess()) {
            throw new IllegalStateException(tokenResponse.getErrorDescription());
        }
        return tokenResponse;
    }

    protected String getRealmAttribute(String key) {
        RealmRepresentation realm = testRealm.admin().toRepresentation();
        Map<String, String> attributes = realm.getAttributesOrEmpty();
        return attributes.get(key);
    }

    protected void setRealmAttributes(Map<String, String> extraAttributes) {
        RealmResource realmResource = testRealm.admin();
        RealmRepresentation realm = realmResource.toRepresentation();
        Map<String, String> attributes = realm.getAttributesOrEmpty();
        attributes.putAll(extraAttributes);
        realm.setAttributes(attributes);
        realmResource.update(realm);
    }

    protected void setVerifiableCredentialsEnabled(boolean enabled) {
        RealmResource realmResource = testRealm.admin();
        RealmRepresentation realm = realmResource.toRepresentation();
        realm.setVerifiableCredentialsEnabled(enabled);
        realmResource.update(realm);
    }

    protected CredentialScopeRepresentation requireExistingCredentialScope(String scopeName) {
        return Optional.ofNullable(getExistingCredentialScope(scopeName))
                .orElseThrow(() -> new IllegalStateException("No such credential scope: " + scopeName));
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private ComponentRepresentation createRsaKeyProviderComponent(KeyWrapper keyWrapper, String name, int priority) {
        ComponentRepresentation component = new ComponentRepresentation();
        component.setProviderType(KeyProvider.class.getName());
        component.setName(name);
        component.setId(UUID.randomUUID().toString());
        component.setProviderId("rsa");

        Certificate certificate = CertificateUtils.generateV1SelfSignedCertificate(
                new KeyPair((PublicKey) keyWrapper.getPublicKey(), (PrivateKey) keyWrapper.getPrivateKey()), "TestKey");

        component.setConfig(new MultivaluedHashMap<>(Map.of(
                "privateKey", List.of(PemUtils.encodeKey(keyWrapper.getPrivateKey())),
                "certificate", List.of(PemUtils.encodeCertificate(certificate)),
                "active", List.of("true"),
                "priority", List.of(String.valueOf(priority)),
                "enabled", List.of("true"),
                "algorithm", List.of(keyWrapper.getAlgorithm()),
                "keyUse", List.of(keyWrapper.getUse().name())
        )));

        return component;
    }

    private void enableVerifiableCredentialEvents(ManagedRealm realm) {
        RealmEventsConfigRepresentation realmEventsConfig = realm.admin().getRealmEventsConfig();
        List<String> enabledEventTypes = realmEventsConfig.getEnabledEventTypes();
        if (!enabledEventTypes.contains(EventType.VERIFIABLE_CREDENTIAL_NONCE_REQUEST.name())) {
            enabledEventTypes.add(EventType.VERIFIABLE_CREDENTIAL_NONCE_REQUEST.name());
            realm.admin().updateRealmEventsConfig(realmEventsConfig);
        }
    }

    // Static Config and RunOnServer Helpers ---------------------------------------------------------------------------

    public static class VCTestServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.OID4VC_VCI);
        }
    }

    public static class VCTestServerWithPreAuthCodeEnabled implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.OID4VC_VCI, Profile.Feature.OID4VC_VCI_PREAUTH_CODE);
        }
    }

    public static class VCTestRealmConfig implements RealmConfig {

        public static final String TEST_REALM_NAME = "test";

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.name(TEST_REALM_NAME)
                    .eventsEnabled(true);

            realm.eventsListeners("jboss-logging");

            CryptoIntegration.init(this.getClass().getClassLoader());
            realm.verifiableCredentialsEnabled(true);
            realm.addRole(CREDENTIAL_OFFER_CREATE);

            // Allow the default client scopes to be added as well
            realm.attribute(CREATE_DEFAULT_CLIENT_SCOPES, "true");

            // Explicitly enable cryptographic binding + proof types for test credential configurations.
            // The issuer metadata only advertises binding/proofs when it is explicitly configured as required.
            CredentialScopeRepresentation sdJwtScope = createCredentialScope(
                    sdJwtTypeCredentialScopeName,
                    null,
                    sdJwtTypeCredentialConfigurationIdName,
                    sdJwtTypeCredentialScopeName,
                    sdJwtTypeCredentialVct,
                    VCFormat.SD_JWT_VC,
                    null,
                    List.of(OID4VCConstants.KeyAttestationResistanceLevels.HIGH, OID4VCConstants.KeyAttestationResistanceLevels.MODERATE)
            );
            Map<String, String> sdJwtAttrs = Optional.ofNullable(sdJwtScope.getAttributes()).orElseGet(HashMap::new);
            sdJwtAttrs.put(CredentialScopeModel.VC_BINDING_REQUIRED, "true");
            sdJwtAttrs.put(CredentialScopeModel.VC_BINDING_REQUIRED_PROOF_TYPES, "jwt");
            sdJwtAttrs.put(CredentialScopeModel.VC_CRYPTOGRAPHIC_BINDING_METHODS,
                    CredentialScopeModel.CRYPTOGRAPHIC_BINDING_METHODS_DEFAULT);
            sdJwtScope.setAttributes(sdJwtAttrs);
            realm.addClientScope(sdJwtScope);

            CredentialScopeRepresentation jwtVcScope = createCredentialScope(
                    jwtTypeCredentialScopeName,
                    ISSUER_DID.toString(),
                    jwtTypeCredentialConfigurationIdName,
                    jwtTypeCredentialScopeName,
                    null,
                    VCFormat.JWT_VC,
                    TEST_CREDENTIAL_MAPPERS_FILE,
                    Collections.emptyList()
            );
            Map<String, String> jwtVcAttrs = Optional.ofNullable(jwtVcScope.getAttributes()).orElseGet(HashMap::new);
            jwtVcAttrs.put(CredentialScopeModel.VC_BINDING_REQUIRED, "true");
            jwtVcAttrs.put(CredentialScopeModel.VC_BINDING_REQUIRED_PROOF_TYPES, "jwt,attestation");
            jwtVcAttrs.put(CredentialScopeModel.VC_CRYPTOGRAPHIC_BINDING_METHODS,
                    CredentialScopeModel.CRYPTOGRAPHIC_BINDING_METHODS_DEFAULT);
            jwtVcScope.setAttributes(jwtVcAttrs);
            realm.addClientScope(jwtVcScope);

            realm.addClientScope(createCredentialScope(
                    minimalJwtTypeCredentialScopeName,
                    null,
                    minimalJwtTypeCredentialConfigurationIdName,
                    null,
                    minimalJwtTypeCredentialScopeName,
                    VC_FORMAT_DEFAULT,
                    null,
                    null
            ));

            realm.addUser(getUserRepresentation("John Doe", Map.of("did", "did:key:1234"), List.of(CREDENTIAL_OFFER_CREATE.getName()), Collections.emptyMap()));
            realm.addUser(getUserRepresentation("Alice Wonderland", Map.of("did", "did:key:5678"), List.of(), Map.of()));

            return realm;
        }

        private CredentialScopeRepresentation createCredentialScope(
                String scopeName,
                String issuerDid,
                String credentialConfigurationId,
                String credentialIdentifier,
                String vct,
                String format,
                String protocolMapperReferenceFile,
                List<String> acceptedKeyAttestationValues) {

            CredentialScopeRepresentation cs = new CredentialScopeRepresentation(scopeName)
                    .setIncludeInTokenScope(true)
                    .setExpiryInSeconds(15)
                    .setIssuerDid(issuerDid)
                    .setCredentialConfigurationId(credentialConfigurationId)
                    .setCredentialIdentifier(credentialIdentifier)
                    .setFormat(format)
                    .setVct(Optional.ofNullable(vct).orElse(credentialIdentifier));

            if (credentialConfigurationId != null) {
                List<DisplayObject> displayObjects = List.of(
                        new DisplayObject().setName(credentialConfigurationId).setLocale("en-EN"),
                        new DisplayObject().setName(credentialConfigurationId).setLocale("de-DE")
                );
                cs.setDisplay(JsonSerialization.valueAsString(displayObjects));
            }

            if (acceptedKeyAttestationValues != null) {
                cs.setKeyAttestationRequired(true);
                if (!acceptedKeyAttestationValues.isEmpty()) {
                    cs.setRequiredKeyAttestationKeyStorage(acceptedKeyAttestationValues);
                    cs.setRequiredKeyAttestationUserAuthentication(acceptedKeyAttestationValues);
                }
            }

            if (protocolMapperReferenceFile == null) {
                cs.setProtocolMappers(ProtocolMapperUtils.getProtocolMappers(scopeName));
            } else {
                List<ProtocolMapperRepresentation> protocolMappers = new ArrayList<>(resolveProtocolMappers(protocolMapperReferenceFile));
                protocolMappers.add(ProtocolMapperUtils.getStaticClaimMapper(scopeName));
                cs.setProtocolMappers(protocolMappers);
            }

            return cs;
        }

        private UserRepresentation getUserRepresentation(
                String fullName,
                Map<String, String> attributes,
                List<String> realmRoles,
                Map<String, List<String>> clientRoles) {

            String[] nameToks = fullName.split("\\s");
            String firstName = nameToks[0];
            String lastName = nameToks.length > 1 ? nameToks[1] : "";
            String username = firstName.toLowerCase();

            UserConfigBuilder userBuilder = UserConfigBuilder.create()
                    .id(KeycloakModelUtils.generateId())
                    .username(username)
                    .enabled(true)
                    .email(username + "@email.cz")
                    .emailVerified(true)
                    .firstName(firstName)
                    .lastName(lastName)
                    .password("password")
                    .attribute("address_street_address", "221B Baker Street")
                    .attribute("address_locality", "London")
                    .roles("account", "manage-account", "view-profile");

            attributes.forEach(userBuilder::attribute);

            // When Keycloak issues a token for a user and client:
            //  1. It looks up all effective realm roles and all effective client roles assigned to the user.
            //  2. The token includes only those roles that the user actually has.
            realmRoles.forEach(userBuilder::roles);
            clientRoles.forEach((cid, roles) -> roles.forEach(role -> userBuilder.roles(cid, role)));

            return userBuilder.build();
        }

        private List<ProtocolMapperRepresentation> resolveProtocolMappers(String protocolMapperReferenceFile) {
            if (protocolMapperReferenceFile == null) {
                return null;
            }
            try (InputStream inputStream = OID4VCIssuerTestBase.class.getResourceAsStream(protocolMapperReferenceFile)) {
                return JsonSerialization.mapper.readValue(inputStream, ClientScopeRepresentation.class).getProtocolMappers();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class ConfidentialOID4VCIClient implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            client.clientId(OID4VCI_CLIENT_ID)
                    .serviceAccountsEnabled(true)
                    .directAccessGrantsEnabled(true)
                    .defaultClientScopes("basic", "profile", "roles")
                    .optionalClientScopes(jwtTypeCredentialScopeName, minimalJwtTypeCredentialScopeName, sdJwtTypeCredentialScopeName, "email")
                    .attribute(OID4VCI_ENABLED_ATTRIBUTE_KEY, "true")
                    .redirectUris("*")
                    .secret("test-secret");
            return client;
        }
    }

    public static class PublicOID4VCIClient implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            client.clientId(OID4VCI_PUBLIC_CLIENT_ID)
                    .publicClient(true)
                    .defaultClientScopes("basic", "profile", "roles")
                    .optionalClientScopes(jwtTypeCredentialScopeName, minimalJwtTypeCredentialScopeName, sdJwtTypeCredentialScopeName, "email")
                    .redirectUris("http://127.0.0.1:8500/callback/oauth")
                    .attribute(OID4VCI_ENABLED_ATTRIBUTE_KEY, "true")
                    .attribute("pkce.code.challenge.method", "S256");  // require PKCE
            return client;
        }
    }

    public static class ProtocolMapperUtils {

        static ProtocolMapperRepresentation getIssuedAtTimeMapper(String subjectProperty, String truncateToTimeUnit, String valueSource) {
            ProtocolMapperRepresentation protocolMapperRepresentation = new ProtocolMapperRepresentation();
            protocolMapperRepresentation.setName(subjectProperty + "-oid4vc-issued-at-time-claim-mapper");
            protocolMapperRepresentation.setProtocol(OID4VCIConstants.OID4VC_PROTOCOL);
            protocolMapperRepresentation.setId(UUID.randomUUID().toString());
            protocolMapperRepresentation.setProtocolMapper("oid4vc-issued-at-time-claim-mapper");

            Map<String, String> configMap = new HashMap<>();
            Optional.ofNullable(subjectProperty)
                    .ifPresent(value -> configMap.put(OID4VCIssuedAtTimeClaimMapper.CLAIM_NAME, value));
            Optional.ofNullable(truncateToTimeUnit)
                    .ifPresent(value -> configMap.put(OID4VCIssuedAtTimeClaimMapper.TRUNCATE_TO_TIME_UNIT_KEY, value));
            Optional.ofNullable(valueSource)
                    .ifPresent(value -> configMap.put(OID4VCIssuedAtTimeClaimMapper.VALUE_SOURCE, value));

            protocolMapperRepresentation.setConfig(configMap);
            return protocolMapperRepresentation;
        }

        static ProtocolMapperRepresentation getJtiGeneratedIdMapper() {
            ProtocolMapperRepresentation protocolMapperRepresentation = new ProtocolMapperRepresentation();
            protocolMapperRepresentation.setName("generated-id-mapper");
            protocolMapperRepresentation.setProtocol(OID4VCIConstants.OID4VC_PROTOCOL);
            protocolMapperRepresentation.setId(UUID.randomUUID().toString());
            protocolMapperRepresentation.setProtocolMapper("oid4vc-generated-id-mapper");
            protocolMapperRepresentation.setConfig(Map.of(OID4VCGeneratedIdMapper.CLAIM_NAME, "jti"));
            return protocolMapperRepresentation;
        }

        static List<ProtocolMapperRepresentation> getProtocolMappers(String scopeName) {
            return List.of(
                    getSubjectIdMapper(CLAIM_NAME_SUBJECT_ID, UserModel.DID),
                    getUserAttributeMapper("email", "email"),
                    getUserAttributeMapper("firstName", "firstName"),
                    getUserAttributeMapper("lastName", "lastName"),
                    getUserAttributeMapper("address.street_address", "address_street_address"),
                    getUserAttributeMapper("address.locality", "address_locality"),
                    getJtiGeneratedIdMapper(),
                    getStaticClaimMapper(scopeName),
                    getIssuedAtTimeMapper("iat", ChronoUnit.HOURS.name(), "COMPUTE"),
                    getIssuedAtTimeMapper("nbf", null, "COMPUTE"));
        }

        static ProtocolMapperRepresentation getStaticClaimMapper(String scopeName) {
            ProtocolMapperRepresentation protocolMapperRepresentation = new ProtocolMapperRepresentation();
            protocolMapperRepresentation.setName(UUID.randomUUID().toString());
            protocolMapperRepresentation.setProtocol(OID4VCIConstants.OID4VC_PROTOCOL);
            protocolMapperRepresentation.setId(UUID.randomUUID().toString());
            protocolMapperRepresentation.setProtocolMapper("oid4vc-static-claim-mapper");
            protocolMapperRepresentation.setConfig(
                    Map.of("claim.name", "scope-name",
                            "staticValue", scopeName)
            );
            return protocolMapperRepresentation;
        }

        static ProtocolMapperRepresentation getSubjectIdMapper(String subjectProperty, String attributeName) {
            ProtocolMapperRepresentation protocolMapperRepresentation = new ProtocolMapperRepresentation();
            protocolMapperRepresentation.setName(attributeName + "-mapper");
            protocolMapperRepresentation.setProtocol(OID4VCIConstants.OID4VC_PROTOCOL);
            protocolMapperRepresentation.setId(UUID.randomUUID().toString());
            protocolMapperRepresentation.setProtocolMapper("oid4vc-subject-id-mapper");
            protocolMapperRepresentation.setConfig(Map.of(
                    "claim.name", subjectProperty,
                    "userAttribute", attributeName));
            return protocolMapperRepresentation;
        }

        static ProtocolMapperRepresentation getUserAttributeMapper(String subjectProperty, String attributeName) {
            ProtocolMapperRepresentation protocolMapperRepresentation = new ProtocolMapperRepresentation();
            protocolMapperRepresentation.setName(attributeName + "-mapper");
            protocolMapperRepresentation.setProtocol(OID4VCIConstants.OID4VC_PROTOCOL);
            protocolMapperRepresentation.setId(UUID.randomUUID().toString());
            protocolMapperRepresentation.setProtocolMapper("oid4vc-user-attribute-mapper");
            protocolMapperRepresentation.setConfig(
                    Map.of(
                            "claim.name", subjectProperty,
                            "userAttribute", attributeName)
            );
            return protocolMapperRepresentation;
        }
    }

    public static class StaticTimeProvider implements TimeProvider {
        private final int currentTimeInS;

        public StaticTimeProvider(int currentTimeInS) {
            this.currentTimeInS = currentTimeInS;
        }

        @Override
        public int currentTimeSeconds() {
            return currentTimeInS;
        }

        @Override
        public long currentTimeMillis() {
            return currentTimeInS * 1000L;
        }
    }

    protected void setOid4vciEnabled(ClientRepresentation testClient, boolean enabled) {
        ClientResource clientResource = testRealm.admin().clients().get(testClient.getId());
        Map<String, String> attributes = Optional.ofNullable(testClient.getAttributes()).orElse(new HashMap<>());
        attributes.put(OID4VCI_ENABLED_ATTRIBUTE_KEY, String.valueOf(enabled));
        testClient.setAttributes(attributes);
        clientResource.update(testClient);
    }
}
