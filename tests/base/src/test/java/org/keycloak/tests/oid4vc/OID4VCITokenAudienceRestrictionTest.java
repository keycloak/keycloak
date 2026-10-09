package org.keycloak.tests.oid4vc;

import java.io.IOException;
import java.util.List;

import jakarta.ws.rs.NotAuthorizedException;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;
import org.keycloak.testsuite.util.oauth.UserInfoResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that an OID4VCI access token – whose audience is restricted to the
 * credential endpoint – is rejected by the other Keycloak endpoints (UserInfo, Account REST, Admin REST etc)
 */
@KeycloakIntegrationTest(config = OID4VCIssuerTestBase.VCTestServerConfig.class)
public class OID4VCITokenAudienceRestrictionTest extends OID4VCIssuerTestBase {

    @InjectUser(config = OID4VCActionTest.OID4VCTestUserConfig.class)
    ManagedUser user;

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    OID4VCTestContext ctx;

    @BeforeEach
    void beforeEach() {
        ctx = new OID4VCTestContext(client, minimalJwtTypeCredentialScope);
        user.admin().logout();
    }

    /**
     * Helper – obtain an OID4VCI access token via authorization_code flow
     *
     * @param oid4vci true if access token with OID4VCI client scope should be obtained. False if token without OID4VCI scope should be obtained
     */
    private String obtainAccessToken(boolean oid4vci) {
        user.admin().logout();

        OID4VCBasicWallet.AuthorizationEndpointRequest request = wallet.authorizationRequest();
        AuthorizationEndpointResponse authResponse;
        if (oid4vci) {
            request.scope(OAuth2Constants.SCOPE_OPENID, ctx.getScope());
        }

        authResponse = request.send(user.getUsername(), TEST_PASSWORD);

        String code = authResponse.getCode();
        assertNotNull(code, "Authorization code must not be null");

        AccessTokenResponse tokenResponse = wallet.accessTokenRequest(ctx, code).send();
        assertTrue(tokenResponse.isSuccess(), "Token exchange must succeed");

        String accessToken = oid4vci
                ? wallet.validateHolderAccessToken(ctx, tokenResponse)
                : tokenResponse.getAccessToken();
        assertNotNull(accessToken, "Access token must not be null");
        return accessToken;
    }

    // ------------------------------------------------------------------
    // UserInfo endpoint
    // ------------------------------------------------------------------

    @Test
    public void testUserInfoEndpointRejectsOid4vciToken() {
        // UserInfo should work with normal token
        String normalAccessToken = obtainAccessToken(false);
        UserInfoResponse userInfoResponse = oauth.doUserInfoRequest(normalAccessToken);
        assertTrue(userInfoResponse.isSuccess(),
                "UserInfo endpoint must work with normal access token");

        // UserInfo should fail with oid4vci token
        String oid4vciAccessToken = obtainAccessToken(true);
        userInfoResponse = oauth.doUserInfoRequest(oid4vciAccessToken);
        assertFalse(userInfoResponse.isSuccess(),
                "UserInfo endpoint must reject an OID4VCI-scoped access token");
        assertEquals(OAuthErrorException.INVALID_TOKEN, userInfoResponse.getError());
    }

    // ------------------------------------------------------------------
    // Token introspection endpoint
    // ------------------------------------------------------------------

    /**
     * The OID4VCI access token is a valid issued token, but unable to call introspection endpoint
     */
    @Test
    public void testIntrospectionEndpoint() throws IOException {
        CredentialIssuer issuerMetadata = wallet.getIssuerMetadata(ctx);
        String credentialEndpoint = issuerMetadata.getCredentialEndpoint();
        assertNotNull(credentialEndpoint, "Credential endpoint must be available in issuer metadata");

        ProtocolMapperRepresentation audienceMapper = ModelToRepresentation.toRepresentation(
                AudienceProtocolMapper.createClaimMapper(
                        "audience-mapper-confidential",
                        OID4VCI_CLIENT_ID,
                        null,
                        true,
                        false,
                        false
                )
        );
        managedClient.admin().getProtocolMappers().createMapper(audienceMapper);

        String normalAccessToken = obtainAccessToken(false);
        invokeIntrospectionEndpoint(normalAccessToken, true);

        String oid4vciAccessToken = obtainAccessToken(true);
        invokeIntrospectionEndpoint(oid4vciAccessToken, false);
    }

    @Test
    public void testIntrospectionEndpoint_withoutRefreshTokenUsed() throws IOException {
        managedClient.updateWithCleanup(clientUpdate -> clientUpdate.attribute(OIDCConfigAttributes.USE_REFRESH_TOKEN, "false"));
        testIntrospectionEndpoint();
    }

    private void invokeIntrospectionEndpoint(String accessToken, boolean expectSuccess) throws IOException {
        IntrospectionResponse introspectionResponse = oauth.doIntrospectionAccessTokenRequest(accessToken);

        assertEquals(200, introspectionResponse.getStatusCode(),
                "Introspection must return HTTP 200");

        // Parse the metadata once and reuse it
        var tokenMetadata = introspectionResponse.asTokenMetadata();

        // Check if token should be reported as active or not
        assertEquals(expectSuccess, tokenMetadata.isActive());
    }

    // ------------------------------------------------------------------
    // Account REST API
    // ------------------------------------------------------------------

    /**
     * The OID4VCI access token must not grant access to the Account REST API
     * (user profile endpoint).  A 401 response is expected.
     */
    @Test
    public void testAccountRestApiRejectsOid4vciToken() throws IOException {
        // Successful call to account REST with the regular token
        String normalAccessToken = obtainAccessToken(false);
        assertEquals(200, invokeAccountRestApi(normalAccessToken));

        // Failed call to account REST with the oid4vci access token
        String oid4vciAccessToken = obtainAccessToken(true);
        assertEquals(401, invokeAccountRestApi(oid4vciAccessToken));
    }

    private int invokeAccountRestApi(String accessToken) throws IOException {
        String accountUrl = testRealm.getBaseUrl() + "/account";

        try (SimpleHttpResponse response = simpleHttp.doGet(accountUrl)
                .header("Accept", "application/json")
                .auth(accessToken)
                .asResponse()) {

            return response.getStatus();
        }
    }

    // ------------------------------------------------------------------
    // Admin REST API
    // ------------------------------------------------------------------

    /**
     * The OID4VCI access token must not grant access to the Admin REST API.
     * Attempting to read the realm representation must throw a
     * {@link NotAuthorizedException}.
     */
    @Test
    public void testAdminRestApiRejectsOid4vciToken() {
        // Grant admin role to the user
        ClientRepresentation mgmtClient = testRealm.admin().clients().findByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID).get(0);
        ClientResource mgmtClientApi = testRealm.admin().clients().get(mgmtClient.getId());
        RoleRepresentation role = mgmtClientApi.roles().get(AdminRoles.VIEW_REALM).toRepresentation();
        user.admin().roles().clientLevel(mgmtClient.getId()).add(List.of(role));

        // Successful admin REST API call when OID4VCI scope not used
        String normalAccessToken = obtainAccessToken(false);
        RealmRepresentation realmRep = invokeAdminRest(normalAccessToken);
        assertEquals(testRealm.getName(), realmRep.getRealm());

        // Should fail admin REST API call when OID4VCI scope is used
        String oid4vciAccessToken = obtainAccessToken(true);
        assertThrows(NotAuthorizedException.class,
                () -> invokeAdminRest(oid4vciAccessToken),
                "Admin REST API must reject an OID4VCI-scoped access token");
    }

    @Test
    public void testAdminRestApiRejectsOid4vciToken_withoutRefreshTokenUsed() {
        managedClient.updateWithCleanup(clientUpdate -> clientUpdate.attribute(OIDCConfigAttributes.USE_REFRESH_TOKEN, "false"));
        testAdminRestApiRejectsOid4vciToken();
    }

    private RealmRepresentation invokeAdminRest(String accessToken) {
        try (Keycloak adminClient = adminClientFactory.create()
                .realm(testRealm.getName())
                .clientId(Constants.ADMIN_CLI_CLIENT_ID)
                .authorization(accessToken)
                .build()) {
            return adminClient.realm(testRealm.getName()).toRepresentation();
        }
    }

}
