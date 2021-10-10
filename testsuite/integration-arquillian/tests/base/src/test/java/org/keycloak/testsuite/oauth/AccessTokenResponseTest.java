package org.keycloak.testsuite.oauth;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.UserBuilder;

public class AccessTokenResponseTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Before
    public void clientConfiguration() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").directAccessGrant(true);
        /*
         * Configure the default client ID. Seems like OAuthClient is keeping the state of clientID
         * For example: If some test case configure oauth.clientId("sample-public-client"), other tests
         * will fail and the clientID will always be "sample-public-client
         * @see AccessTokenTest#testAuthorizationNegotiateHeaderIgnored()
         */
        oauth.clientId("test-app");
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);

        UserBuilder user = UserBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .username("no-permissions")
                .addRoles("user")
                .password("password");
        realm.getUsers().add(user.build());

        ProtocolMapperRepresentation customClaimHardcodedMapper = new ProtocolMapperRepresentation();
        customClaimHardcodedMapper.setName("custom-claim-hardcoded-mapper");
        customClaimHardcodedMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        customClaimHardcodedMapper.setProtocolMapper(HardcodedClaim.PROVIDER_ID);
        Map<String, String> config = new HashMap<>();
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "custom_hardcoded_claim");
        config.put(HardcodedClaim.CLAIM_VALUE, "custom_claim");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN_RESPONSE, "true");
        customClaimHardcodedMapper.setConfig(config);

        realm.getClients().stream().filter(clientRepresentation -> "test-app".equals(clientRepresentation.getClientId()))
                .forEach(clientRepresentation -> {
                    clientRepresentation.setProtocolMappers(Collections.singletonList(customClaimHardcodedMapper));
                    clientRepresentation.setFullScopeAllowed(false);
                });

        testRealms.add(realm);
    }

    @Test
    public void accessTokenRequest() {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, "password");

        assertEquals(200, response.getStatusCode());

        assertEquals("custom_claim", response.getOtherClaims().get("custom_hardcoded_claim"));
    }
}
