package org.keycloak.tests.oid4vc;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.resourceindicators.ResourceIndicatorConstants;
import org.keycloak.representations.AccessToken;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The resource parameter of Resource Indicators together with a credential scope of OID4VCI, which restricts the
 * audience of the access token to the credential endpoint.
 */
@KeycloakIntegrationTest(config = OID4VCResourceIndicatorsTest.ServerConfig.class)
public class OID4VCResourceIndicatorsTest extends OID4VCIssuerTestBase {

    private static final String RESOURCE_1 = "https://api-1.example.com";
    private static final String RESOURCE_2 = "https://api-2.example.com";

    @AfterEach
    void logoutUser() {
        // the login form is not sent by the wallet, so the wallet does not log the user out
        wallet.logout(TEST_USER);
    }

    @Test
    public void testResourcesOtherThanCredentialEndpointRejected() {
        // without the credential scope, both the resources could be resolved from the audience of the access token
        addAudienceMapper(RESOURCE_1);
        addAudienceMapper(RESOURCE_2);

        AuthorizationEndpointResponse authResponse = oauth.loginForm().scope(jwtTypeCredentialScopeName)
                .resource(RESOURCE_1).resource(RESOURCE_2).doLogin(TEST_USER, TEST_PASSWORD);
        assertNotNull(authResponse.getCode());

        // the access token must not be issued with an audience that was not requested
        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(authResponse.getCode()).send();
        assertFalse(tokenResponse.isSuccess());
        assertEquals(OAuthErrorException.INVALID_TARGET, tokenResponse.getError());
        assertEquals(ResourceIndicatorConstants.ERROR_INVALID_RESOURCE, tokenResponse.getErrorDescription());
    }

    @Test
    public void testCredentialEndpointAsResource() {
        String credentialEndpoint = oauth.oid4vc().doIssuerMetadataRequest().getMetadata().getCredentialEndpoint();

        AuthorizationEndpointResponse authResponse = oauth.loginForm().scope(jwtTypeCredentialScopeName)
                .resource(credentialEndpoint).doLogin(TEST_USER, TEST_PASSWORD);
        assertNotNull(authResponse.getCode());

        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(authResponse.getCode()).resource(credentialEndpoint).send();
        assertTrue(tokenResponse.isSuccess());
        AccessToken accessToken = oauth.parseToken(tokenResponse.getAccessToken(), AccessToken.class);
        assertArrayEquals(new String[] { credentialEndpoint }, accessToken.getAudience());
    }

    private void addAudienceMapper(String audience) {
        String mapperId = ApiUtil.getCreatedId(managedClient.admin().getProtocolMappers().createMapper(ModelToRepresentation.toRepresentation(
                AudienceProtocolMapper.createClaimMapper("audience-" + audience, null, audience, true, false, false))));
        managedClient.cleanup().add(c -> c.getProtocolMappers().delete(mapperId));
    }

    public static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.OID4VC_VCI, Profile.Feature.RESOURCE_INDICATORS);
        }
    }
}
