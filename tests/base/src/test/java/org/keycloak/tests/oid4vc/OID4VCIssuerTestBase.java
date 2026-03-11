package org.keycloak.tests.oid4vc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

import org.keycloak.VCFormat;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopesResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.common.Profile;
import org.keycloak.constants.OID4VCIConstants;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oid4vc.issuance.OID4VCAuthorizationDetailsParser;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCGeneratedIdMapper;
import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCIssuedAtTimeClaimMapper;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.protocol.oid4vc.model.DisplayObject;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectDependency;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
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
import org.keycloak.util.AuthorizationDetailsParser;
import org.keycloak.util.JsonSerialization;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;

import static org.keycloak.OID4VCConstants.CLAIM_NAME_SUBJECT_ID;
import static org.keycloak.OID4VCConstants.OID4VCI_ENABLED_ATTRIBUTE_KEY;
import static org.keycloak.OID4VCConstants.OPENID_CREDENTIAL;
import static org.keycloak.constants.OID4VCIConstants.CREDENTIAL_OFFER_CREATE;

/**
 * Abstract base class for OID4VCI Testing
 *
 * [TODO] Can the server runtime mode be configured by the testcase?
 * Server-side debugging: KC_TEST_SERVER=embedded
 */
public abstract class OID4VCIssuerTestBase {

    final Logger log = Logger.getLogger(getClass());

    static final URI ISSUER_DID = URI.create("did:web:test.org");
    static final String TEST_CREDENTIAL_MAPPERS_FILE = "/oid4vc/test-credential-mappers.json";

    static final String jwtTypeCredentialScopeName = "jwt-credential";
    static final String jwtTypeCredentialConfigurationIdName = "jwt-credential-config-id";
    static final String minimalJwtTypeCredentialScopeName = "vc-with-minimal-config";

    CredentialScopeRepresentation jwtTypeCredentialScope;
    CredentialScopeRepresentation minimalJwtTypeCredentialScope;

    @InjectRealm(config = VCTestRealmConfig.class)
    ManagedRealm testRealm;

    @InjectAdminClient
    Keycloak keycloak;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectWebDriver
    ManagedWebDriver driver;

    String clientId = "test-app";
    ClientRepresentation client;

    @TestSetup
    public void configureTestRealm() {

        // Enable OID4VCI on the test realm
        //
        RealmResource realmResource = testRealm.admin();
        RealmRepresentation realm = realmResource.toRepresentation();
        realm.setVerifiableCredentialsEnabled(shouldEnableOid4vci(realm));
        realmResource.update(realm);

        // Add a user representations
        //
        UsersResource usersResource = realmResource.users();
        for (UserRepresentation user : List.of(
                getUserRepresentation("John Doe", Map.of("did", "did:key:1234"), List.of(CREDENTIAL_OFFER_CREATE.getName()), Map.of()),
                getUserRepresentation("Alice Wonderland", Map.of("did", "did:key:5678"), List.of(), Map.of()),
                getUserRepresentation("Bob Baumeister", Map.of("did", "did:key:789"), List.of(), Map.of()))) {

            try (Response res = usersResource.create(user)) {

                String userId = CreatedResponseUtil.getCreatedId(res);
                UserResource userResource = usersResource.get(userId);

                List<RoleRepresentation> userRealmRoles = testRealm.admin().roles().list().stream()
                        .filter(it -> user.getRealmRoles().contains(it.getName()))
                        .toList();

                userResource.roles().realmLevel().add(userRealmRoles);
            }
        }

        // Register Credential Scopes
        //
        ClientScopesResource clientScopesResource = realmResource.clientScopes();
        clientScopesResource.create(createCredentialScope(jwtTypeCredentialScopeName,
                ISSUER_DID.toString(),
                jwtTypeCredentialConfigurationIdName,
                jwtTypeCredentialScopeName,
                null,
                VCFormat.JWT_VC,
                TEST_CREDENTIAL_MAPPERS_FILE,
                Collections.emptyList())
        ).close();
        clientScopesResource.create(createCredentialScope(minimalJwtTypeCredentialScopeName,
                null,
                null,
                null,
                null,
                null,
                null,
                null)
        ).close();

        jwtTypeCredentialScope = requireExistingCredentialScope(jwtTypeCredentialScopeName);
        minimalJwtTypeCredentialScope = requireExistingCredentialScope(minimalJwtTypeCredentialScopeName);

        // Update the test clients
        //
        ClientsResource clientsResource = realmResource.clients();
        for (String cid : List.of(clientId)) {
            ClientRepresentation client = clientsResource.findByClientId(cid).get(0);
            ClientResource clientResource = clientsResource.get(client.getId());

            // Enable OID4VCI
            setOid4vciEnabled(client, shouldEnableOid4vci(client));
            clientResource.update(client);

            // Assign optional client scopes
            clientResource.addOptionalClientScope(jwtTypeCredentialScope.getId());
            clientResource.addOptionalClientScope(minimalJwtTypeCredentialScope.getId());
        }

        // Fetch the test client
        client = clientsResource.findByClientId(clientId).get(0);

        AuthorizationDetailsParser.registerParser(OPENID_CREDENTIAL, new OID4VCAuthorizationDetailsParser());
    }

    @BeforeEach
    void beforeEachInternal() {
        client = testRealm.admin().clients().findByClientId(clientId).get(0);
        jwtTypeCredentialScope = requireExistingCredentialScope(jwtTypeCredentialScopeName);
        minimalJwtTypeCredentialScope = requireExistingCredentialScope(minimalJwtTypeCredentialScopeName);
    }

    protected boolean shouldEnableOid4vci(RealmRepresentation realm) {
        return true;
    }

    protected boolean shouldEnableOid4vci(ClientRepresentation client) {
        return true;
    }

    boolean isOid4vciEnabled(ClientRepresentation client) {
        Map<String, String> attributes = Optional.ofNullable(client.getAttributes()).orElse(new HashMap<>());
        return Boolean.parseBoolean(attributes.get(OID4VCI_ENABLED_ATTRIBUTE_KEY));
    }

    void setOid4vciEnabled(ClientRepresentation client, boolean enable) {
        Map<String, String> attributes = Optional.ofNullable(client.getAttributes()).orElse(new HashMap<>());
        attributes.put(OID4VCI_ENABLED_ATTRIBUTE_KEY, String.valueOf(enable));
        client.setAttributes(attributes);
    }

    CredentialScopeRepresentation createCredentialScope(
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
                    new DisplayObject().setName(credentialConfigurationId).setLocale("de-DE"));
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
            cs.setProtocolMappers(getProtocolMappers(scopeName));
        } else {
            List<ProtocolMapperRepresentation> protocolMappers = resolveProtocolMappers(protocolMapperReferenceFile);
            protocolMappers.add(getStaticClaimMapper(scopeName));
            cs.setProtocolMappers(protocolMappers);
        }

        return cs;
    }

    CredentialScopeRepresentation getExistingCredentialScope(String scopeName) {
        return testRealm.admin().clientScopes().findAll().stream()
                .filter(it -> scopeName.equals(it.getName()))
                .map(CredentialScopeRepresentation::new)
                .findFirst().orElse(null);
    }

    CredentialScopeRepresentation requireExistingCredentialScope(String scopeName) {
        return Optional.ofNullable(getExistingCredentialScope(scopeName))
                .orElseThrow(() -> new IllegalStateException("No such credential scope: " + scopeName));
    }

    ProtocolMapperRepresentation getIssuedAtTimeMapper(String subjectProperty, String truncateToTimeUnit, String valueSource) {
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

    ProtocolMapperRepresentation getJtiGeneratedIdMapper() {
        ProtocolMapperRepresentation protocolMapperRepresentation = new ProtocolMapperRepresentation();
        protocolMapperRepresentation.setName("generated-id-mapper");
        protocolMapperRepresentation.setProtocol(OID4VCIConstants.OID4VC_PROTOCOL);
        protocolMapperRepresentation.setId(UUID.randomUUID().toString());
        protocolMapperRepresentation.setProtocolMapper("oid4vc-generated-id-mapper");
        protocolMapperRepresentation.setConfig(Map.of(OID4VCGeneratedIdMapper.CLAIM_NAME, "jti"));
        return protocolMapperRepresentation;
    }

    List<ProtocolMapperRepresentation> getProtocolMappers(String scopeName) {
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

    ProtocolMapperRepresentation getSubjectIdMapper(String subjectProperty, String attributeName) {
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

    ProtocolMapperRepresentation getStaticClaimMapper(String scopeName) {
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

    ProtocolMapperRepresentation getUserAttributeMapper(String subjectProperty, String attributeName) {
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

    UserRepresentation getUserRepresentation(
            String fullName,
            Map<String, String> attributes,
            List<String> realmRoles,
            Map<String, List<String>> clientRoles
    ) {
        String[] nameToks = fullName.split("\\s");
        String firstName = nameToks[0];
        String lastName = nameToks[1];
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
        //
        //  1. It looks up all effective realm roles and all effective client roles assigned to the user.
        //  2. The token includes only those roles that the user actually has.
        //
        realmRoles.forEach(userBuilder::roles);
        clientRoles.forEach((cid, roles) -> roles.forEach(role -> userBuilder.roles(cid, role)));
        return userBuilder.build();
    }

    List<ProtocolMapperRepresentation> resolveProtocolMappers(String protocolMapperReferenceFile) {
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

    // Config ----------------------------------------------------------------------------------------------------------

    static class VCTestServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.OID4VC_VCI);
        }
    }

    static class VCTestRealmConfig implements RealmConfig {

        public static final String TEST_REALM_NAME = "test";

        @InjectDependency
        KeycloakUrls keycloakUrls;

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.name(TEST_REALM_NAME);
            realm.verifiableCredentialsEnabled(true);
            return realm;
        }
    }
}
