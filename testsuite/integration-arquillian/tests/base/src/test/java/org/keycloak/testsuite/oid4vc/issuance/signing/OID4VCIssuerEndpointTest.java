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
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.crypto.Algorithm;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oid4vc.issuance.OID4VCIssuerEndpoint;
import org.keycloak.protocol.oid4vc.issuance.TimeProvider;
import org.keycloak.protocol.oid4vc.issuance.signing.JwtSigningService;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialRequest;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.Format;
import org.keycloak.protocol.oid4vc.model.SupportedCredential;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.testsuite.runonserver.RunOnServerException;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.TokenUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class OID4VCIssuerEndpointTest extends OID4VCTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Rule
    public TokenUtil tokenUtil = new TokenUtil();

    private CloseableHttpClient httpClient;

    @Before
    public void setup() {
        CryptoIntegration.init(this.getClass().getClassLoader());
        httpClient = HttpClientBuilder.create().build();
    }

    @Test(expected = BadRequestException.class)
    public void testGetCredentialOfferUriUnsupportedCredential() throws Throwable {
        String token = tokenUtil.getToken();
        try {
            testingClient.server(TEST_REALM_NAME)
                    .run((session -> {
                        OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, token);
                        oid4VCIssuerEndpoint.getCredentialOfferURI("inexistent-id");

                    }));
        } catch (Exception e) {
            if (e instanceof RunOnServerException) {
                throw e.getCause();
            }
            throw e;
        }
    }

    @Test(expected = BadRequestException.class)
    public void testGetCredentialOfferUriUnauthorized() throws Throwable {
        try {
            testingClient.server(TEST_REALM_NAME)
                    .run((session -> {
                        OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, null);
                        oid4VCIssuerEndpoint.getCredentialOfferURI("inexistent-id");
                    }));
        } catch (Exception e) {
            if (e instanceof RunOnServerException) {
                throw e.getCause();
            }
            throw e;
        }
    }

    @Test
    public void testGetCredentialOfferURI() {
        String token = tokenUtil.getToken();
        testingClient
                .server(TEST_REALM_NAME)
                .run((session) -> {
                    try {
                        OID4VCIssuerEndpoint oid4VCIssuerEndpoint = prepareIssuerEndpoint(session, token);

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

    private static OID4VCIssuerEndpoint prepareIssuerEndpoint(KeycloakSession session, String token) {
        AppAuthManager.BearerTokenAuthenticator authenticator = new AppAuthManager.BearerTokenAuthenticator(session);
        authenticator.setTokenString(token);

        TimeProvider timeProvider = new OID4VCTest.StaticTimeProvider(1000);
        JwtSigningService jwtSigningService = new JwtSigningService(
                session,
                getKeyFromSession(session).getKid(),
                Algorithm.RS256,
                "JWT",
                "did:web:issuer.org",
                timeProvider);
        return new OID4VCIssuerEndpoint(
                session,
                "did:web:issuer.org",
                Map.of(Format.JWT_VC, jwtSigningService),
                authenticator,
                new ObjectMapper(),
                timeProvider);
    }

    private String getBasePath(String realm) {
        return suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/" + realm + "/protocol/oid4vc/";
    }

    @Test
    public void testCredentialIssuance() throws Exception {

        SimpleHttp.Response response = SimpleHttp.doGet(getBasePath(TEST_REALM_NAME) + "credential-offer-uri?credentialId=test-credential", httpClient)
                .auth(tokenUtil.getToken())
                .asResponse();

        assertEquals("A valid offer uri should be returned", HttpStatus.SC_OK, response.getStatus());
        CredentialOfferURI credentialOfferURI = new ObjectMapper().convertValue(response.asJson(), CredentialOfferURI.class);

        SimpleHttp.Response offer = SimpleHttp.doGet(getBasePath(TEST_REALM_NAME) + "credential-offer/" + credentialOfferURI.getNonce(), httpClient)
                .auth(tokenUtil.getToken())
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

}