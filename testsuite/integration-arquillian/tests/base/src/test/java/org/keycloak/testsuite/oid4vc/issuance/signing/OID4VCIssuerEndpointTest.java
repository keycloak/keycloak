package org.keycloak.testsuite.oid4vc.issuance.signing;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.keycloak.TokenVerifier;
import org.keycloak.adapters.saml.config.Key;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.VerificationException;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.crypto.Algorithm;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.TimeProvider;
import org.keycloak.protocol.oid4vc.issuance.signing.JwtSigningService;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.ErrorResponse;
import org.keycloak.protocol.oid4vc.model.ErrorType;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedGrant;
import org.keycloak.protocol.oid4vc.model.SupportedCredential;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.protocol.oidc.utils.OAuth2Code;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.testsuite.runonserver.RunOnServerException;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.TokenUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OID4VCIssuerEndpointTest extends OID4VCTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TimeProvider TIME_PROVIDER = new OID4VCTest.StaticTimeProvider(1000);
    private CloseableHttpClient httpClient;


    @Before
    public void setup() {
        CryptoIntegration.init(this.getClass().getClassLoader());
        httpClient = HttpClientBuilder.create().build();
    }


    // ----- getCredentialOfferUri

    @Test(expected = BadRequestException.class)
    public void testGetCredentialOfferUriUnsupportedCredential() throws Throwable {
        String token = getBearerToken(oauth);
        withCausePropagation(() -> testingClient.server(TEST_REALM_NAME)
                .run((session -> {
                    AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                    authenticator.setTokenString(token);

                    OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                    oid4VCIssuerEndpoint.getCredentialOfferURI("inexistent-id");
                })));

    }

    @Test(expected = BadRequestException.class)
    public void testGetCredentialOfferUriUnauthorized() throws Throwable {
        withCausePropagation(() -> testingClient.server(TEST_REALM_NAME)
                .run((session -> {
                    AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                    authenticator.setTokenString(null);
                    OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                    oid4VCIssuerEndpoint.getCredentialOfferURI("test-credential");
                })));
    }

    @Test(expected = BadRequestException.class)
    public void testGetCredentialOfferUriInvalidToken() throws Throwable {
        withCausePropagation(() -> testingClient.server(TEST_REALM_NAME)
                .run((session -> {
                    AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                    authenticator.setTokenString("invalid-token");
                    OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                    oid4VCIssuerEndpoint.getCredentialOfferURI("test-credential");
                })));
    }

    @Test
    public void testGetCredentialOfferURI() {
        String token = getBearerToken(oauth);
        testingClient
                .server(TEST_REALM_NAME)
                .run((session) -> {
                    try {
                        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                        authenticator.setTokenString(token);
                        OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, authenticator);

                        Response response = oid4VCIssuerEndpoint.getCredentialOfferURI("test-credential");

                        assertEquals("An offer uri should have been returned.", HttpStatus.SC_OK, response.getStatus());
                        CredentialOfferURI credentialOfferURI = new ObjectMapper().convertValue(response.getEntity(), CredentialOfferURI.class);
                        assertNotNull("A nonce should be included.", credentialOfferURI.getNonce());
                        assertNotNull("The issuer uri should be provided.", credentialOfferURI.getIssuer());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

    }

    private static String getBearerToken(OAuthClient oAuthClient) {
        OAuthClient.AuthorizationEndpointResponse authorizationEndpointResponse = oAuthClient.doLogin("john", "password");
        return oAuthClient.doAccessTokenRequest(authorizationEndpointResponse.getCode(), "password").getAccessToken();
    }

    // ----- getCredentialOffer

    @Test(expected = BadRequestException.class)
    public void testGetCredentialOfferUnauthorized() throws Throwable {
        withCausePropagation(() -> {
            testingClient
                    .server(TEST_REALM_NAME)
                    .run((session) -> {
                        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                        authenticator.setTokenString(null);
                        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                        issuerEndpoint.getCredentialOffer("nonce");
                    });
        });
    }

    @Test(expected = BadRequestException.class)
    public void testGetCredentialOfferWithoutNonce() throws Throwable {
        String token = getBearerToken(oauth);
        withCausePropagation(() -> {
            testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                        authenticator.setTokenString(token);
                        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                        issuerEndpoint.getCredentialOffer(null);
                    }));
        });
    }

    @Test(expected = BadRequestException.class)
    public void testGetCredentialOfferWithoutAPreparedOffer() throws Throwable {
        String token = getBearerToken(oauth);
        withCausePropagation(() -> {
            testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                        authenticator.setTokenString(token);
                        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                        issuerEndpoint.getCredentialOffer("unpreparedNonce");
                    }));
        });
    }

    @Test(expected = BadRequestException.class)
    public void testGetCredentialOfferWithABrokenNote() throws Throwable {
        String token = getBearerToken(oauth);
        withCausePropagation(() -> {
            testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                        authenticator.setTokenString(token);
                        String nonce = prepareNonce(authenticator, "invalidNote");
                        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                        issuerEndpoint.getCredentialOffer(nonce);
                    }));
        });
    }

    @Test
    public void testGetCredentialOffer() {
        String token = getBearerToken(oauth);
        String rootURL = suiteContext.getAuthServerInfo().getContextRoot().toString();
        testingClient
                .server(TEST_REALM_NAME)
                .run((session) -> {
                    AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                    authenticator.setTokenString(token);

                    SupportedCredential supportedCredential = new SupportedCredential()
                            .setId("test-credential")
                            .setScope("VerifiableCredential")
                            .setFormat(Format.JWT_VC);
                    String nonce = prepareNonce(authenticator, OBJECT_MAPPER.writeValueAsString(supportedCredential));

                    OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                    Response credentialOfferResponse = issuerEndpoint.getCredentialOffer(nonce);
                    assertEquals("The offer should have been returned.", HttpStatus.SC_OK, credentialOfferResponse.getStatus());
                    Object credentialOfferEntity = credentialOfferResponse.getEntity();
                    assertNotNull("An actual offer should be in the response.", credentialOfferEntity);

                    CredentialsOffer credentialsOffer = OBJECT_MAPPER.convertValue(credentialOfferEntity, CredentialsOffer.class);
                    assertNotNull("Credentials should have been offered.", credentialsOffer.getCredentials());
                    assertFalse("Credentials should have been offered.", credentialsOffer.getCredentials().isEmpty());
                    List<SupportedCredential> supportedCredentials = credentialsOffer.getCredentials()
                            .stream()
                            .map(co -> OBJECT_MAPPER.convertValue(co, SupportedCredential.class))
                            .collect(Collectors.toList());
                    assertEquals("Exactly one credential should have been returend.", 1, supportedCredentials.size());
                    SupportedCredential offeredCredential = supportedCredentials.get(0);
                    assertEquals("The credential should be as defined in the note.", supportedCredential, offeredCredential);

                    PreAuthorizedGrant grant = credentialsOffer.getGrants();
                    assertNotNull("The grant should be included.", grant);
                    assertNotNull("The grant should contain the pre-authorized code.", grant.getPreAuthorizedCode());
                    assertNotNull("The actual pre-authorized code should be included.", grant
                            .getPreAuthorizedCode()
                            .getPreAuthorizedCode());

                    assertEquals("The correct issuer should be included.", rootURL + "/auth/realms/" + TEST_REALM_NAME, credentialsOffer.getCredentialIssuer());
                });
    }

    // ----- requestCredential

    @Test(expected = BadRequestException.class)
    public void testRequestCredentialUnauthorized() throws Throwable {
        withCausePropagation(() -> {
            testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                        authenticator.setTokenString(null);
                        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                        issuerEndpoint.requestCredential(new CredentialRequest()
                                .setFormat(Format.JWT_VC)
                                .setCredentialIdentifier("test-credential"));
                    }));
        });
    }

    @Test(expected = BadRequestException.class)
    public void testRequestCredentialInvalidToken() throws Throwable {
        withCausePropagation(() -> {
            testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                        authenticator.setTokenString("token");
                        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                        issuerEndpoint.requestCredential(new CredentialRequest()
                                .setFormat(Format.JWT_VC)
                                .setCredentialIdentifier("test-credential"));
                    }));
        });
    }

    @Test(expected = BadRequestException.class)
    public void testRequestCredentialUnsupportedFormat() throws Throwable {
        String token = getBearerToken(oauth);
        withCausePropagation(() -> {
            testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                        authenticator.setTokenString(token);
                        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                        issuerEndpoint.requestCredential(new CredentialRequest()
                                .setFormat(Format.SD_JWT_VC)
                                .setCredentialIdentifier("test-credential"));
                    }));
        });
    }

    @Test(expected = BadRequestException.class)
    public void testRequestCredentialUnsupportedCredential() throws Throwable {
        String token = getBearerToken(oauth);
        withCausePropagation(() -> {
            testingClient
                    .server(TEST_REALM_NAME)
                    .run((session -> {
                        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                        authenticator.setTokenString(token);
                        OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                        issuerEndpoint.requestCredential(new CredentialRequest()
                                .setFormat(Format.JWT_VC)
                                .setCredentialIdentifier("no-such-credential"));
                    }));
        });
    }

    @Test
    public void testRequestCredential() {
        String token = getBearerToken(oauth);
        testingClient
                .server(TEST_REALM_NAME)
                .run((session -> {
                    AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
                    authenticator.setTokenString(token);
                    OID4VCIssuerEndpoint issuerEndpoint = prepareIssuerEndpoint(session, authenticator);
                    CredentialRequest credentialRequest = new CredentialRequest()
                            .setFormat(Format.JWT_VC)
                            .setCredentialIdentifier("test-credential");
                    Response credentialResponse = issuerEndpoint.requestCredential(credentialRequest);
                    assertEquals("The credential request should be answered successfully.", HttpStatus.SC_OK, credentialResponse.getStatus());
                    assertNotNull("A credential should be responded.", credentialResponse.getEntity());
                    CredentialResponse credentialResponseVO = OBJECT_MAPPER.convertValue(credentialResponse.getEntity(), CredentialResponse.class);
                    JsonWebToken jsonWebToken = TokenVerifier.create((String) credentialResponseVO.getCredential(), JsonWebToken.class).getToken();
                    // correct signing and contents are verified in the JwtSigningServiceTest, thus we only check that it is a JWT
                    assertNotNull("A valid credential string should have been responded", jsonWebToken);
                }));
    }


    // Tests the complete flow from
    // 1. Retrieving the credential-offer-uri
    // 2. Using the uri to get the actual credential offer
    // 3. Use the preAuthorized code to retrieve a credential.
    @Test
    public void testCredentialIssuance() throws Exception {

        String token = getBearerToken(oauth);
        SimpleHttp.Response response = SimpleHttp.doGet(getBasePath(TEST_REALM_NAME) + "credential-offer-uri?credentialId=test-credential", httpClient)
                .auth(token)
                .asResponse();

        assertEquals("A valid offer uri should be returned", HttpStatus.SC_OK, response.getStatus());
        CredentialOfferURI credentialOfferURI = new ObjectMapper().convertValue(response.asJson(), CredentialOfferURI.class);

        SimpleHttp.Response offer = SimpleHttp.doGet(getBasePath(TEST_REALM_NAME) + "credential-offer/" + credentialOfferURI.getNonce(), httpClient)
                .auth(token)
                .asResponse();
        assertEquals("A valid offer should be returned", HttpStatus.SC_OK, response.getStatus());
        CredentialsOffer credentialOffer = new ObjectMapper().convertValue(offer.asJson(), CredentialsOffer.class);

        OAuthClient.AccessTokenResponse accessTokenResponse = oauth.doPreauthorizedTokenRequest(credentialOffer.getGrants().getPreAuthorizedCode().getPreAuthorizedCode());

        assertEquals(HttpStatus.SC_OK, accessTokenResponse.getStatusCode());
        String theToken = accessTokenResponse.getAccessToken();

        credentialOffer.getCredentials().stream()
                .map(offeredCredential -> OBJECT_MAPPER.convertValue(offeredCredential, SupportedCredential.class))
                .forEach(supportedCredential -> {
                    try {
                        requestOffer(theToken, supportedCredential);
                    } catch (IOException e) {
                        fail("Was not able to get the credential.");
                    }
                });
    }

    private static String prepareNonce(AppAuthManager.BearerTokenAuthenticator authenticator, String note) {
        String nonce = SecretGenerator.getInstance().randomString();
        AuthenticationManager.AuthResult authResult = authenticator.authenticate();
        UserSessionModel userSessionModel = authResult.getSession();
        userSessionModel.getAuthenticatedClientSessionByClient(authResult.getClient().getId()).setNote(nonce, note);
        return nonce;
    }

    private static OID4VCIssuerEndpoint prepareIssuerEndpoint(KeycloakSession session, AppAuthManager.BearerTokenAuthenticator authenticator) {


        JwtSigningService jwtSigningService = new JwtSigningService(
                session,
                getKeyFromSession(session).getKid(),
                Algorithm.RS256,
                "JWT",
                "did:web:issuer.org",
                TIME_PROVIDER);
        return new OID4VCIssuerEndpoint(
                session,
                "did:web:issuer.org",
                Map.of(Format.JWT_VC, jwtSigningService),
                authenticator,
                new ObjectMapper(),
                TIME_PROVIDER,
                30);
    }

    private String getBasePath(String realm) {
        return suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/" + realm + "/protocol/oid4vc/";
    }

    private void requestOffer(String token, SupportedCredential offeredCredential) throws IOException {
        CredentialRequest request = new CredentialRequest();
        request.setFormat(offeredCredential.getFormat());
        request.setCredentialIdentifier(offeredCredential.getId());

        SimpleHttp.Response credentialResponse = SimpleHttp.doPost(getBasePath(TEST_REALM_NAME) + "credential", httpClient)
                .json(request)
                .auth(token)
                .asResponse();

        assertEquals("The credential should have successfully been responded.", HttpStatus.SC_OK, credentialResponse.getStatus());
        CredentialResponse credentialResponseVO = new ObjectMapper().convertValue(credentialResponse.asJson(), CredentialResponse.class);
        assertEquals("Credential should be in the requested format.", offeredCredential.getFormat(), credentialResponseVO.getFormat());
        assertNotNull("The credential should have been responded.", credentialResponseVO.getCredential());
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        if (testRealm.getComponents() != null) {
            testRealm.getComponents().add("org.keycloak.keys.KeyProvider", getRsaKeyProvider(RSA_KEY));
            testRealm.getComponents().add("org.keycloak.protocol.oid4vc.issuance.signing.VerifiableCredentialsSigningService", getJwtSigningProvider(RSA_KEY));
        } else {
            testRealm.setComponents(new MultivaluedHashMap<>(
                    Map.of("org.keycloak.keys.KeyProvider", List.of(getRsaKeyProvider(RSA_KEY)),
                            "org.keycloak.protocol.oid4vc.issuance.signing.VerifiableCredentialsSigningService", List.of(getJwtSigningProvider(RSA_KEY))
                    )));
        }
        ClientRepresentation clientRepresentation = getTestClient("did:web:test.org");
        if (testRealm.getClients() != null) {
            testRealm.getClients().add(clientRepresentation);
        } else {
            testRealm.setClients(List.of(clientRepresentation));
        }
        if (testRealm.getRoles() != null) {
            testRealm.getRoles().getClient()
                    .put(clientRepresentation.getClientId(), List.of(getRoleRepresentation("testRole", clientRepresentation.getClientId())));
        } else {
            testRealm.getRoles()
                    .setClient(Map.of(clientRepresentation.getClientId(), List.of(getRoleRepresentation("testRole", clientRepresentation.getClientId()))));
        }
        if (testRealm.getUsers() != null) {
            testRealm.getUsers().add(getUserRepresentation(Map.of(clientRepresentation.getClientId(), List.of("testRole"))));
        } else {
            testRealm.setUsers(List.of(getUserRepresentation(Map.of(clientRepresentation.getClientId(), List.of("testRole")))));
        }
        if (testRealm.getAttributes() != null) {
            testRealm.getAttributes().put("issuerDid", TEST_DID.toString());
        } else {
            testRealm.setAttributes(Map.of("issuerDid", TEST_DID.toString()));
        }
    }

    private void withCausePropagation(Runnable r) throws Throwable {
        try {
            r.run();
        } catch (Exception e) {
            if (e instanceof RunOnServerException) {
                throw e.getCause();
            }
            throw e;
        }
    }
}

