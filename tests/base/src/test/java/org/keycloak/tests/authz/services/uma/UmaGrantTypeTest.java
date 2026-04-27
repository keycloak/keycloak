package org.keycloak.tests.authz.services.uma;

import java.time.Duration;

import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.tests.authz.services.Permission;
import org.keycloak.tests.authz.services.config.DefaultAuthzServicesServerConfig;
import org.keycloak.tests.authz.services.config.DefaultResourceServerConfig;
import org.keycloak.tests.common.BasicUserConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@KeycloakIntegrationTest(config = DefaultAuthzServicesServerConfig.class)
public class UmaGrantTypeTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectUser(config = BasicUserConfig.class)
    ManagedUser user;

    @InjectClient(config = DefaultResourceServerConfig.class, ref = "resource-server-foo")
    ManagedClient rs1;

    @InjectClient(config = DefaultResourceServerConfig.class, ref = "resource-server-bar")
    ManagedClient rs2;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @Test
    public void testIdTokenClaimIssuedDifferentClient() {
        AccessTokenResponse tokenResponse = oauth.client(rs1.getClientId(), rs1.getSecret()).doPasswordGrantRequest(user.getUsername(), user.getPassword());
        String idToken = tokenResponse.getIdToken();
        assertNotNull(idToken);

        Permission.create(rs2)
                .resource("resource")
                .grant();

        tokenResponse = oauth.client(rs2.getClientId(), rs2.getSecret())
                .permissionGrantRequest()
                .claimToken(idToken)
                .send();
        String accessToken = tokenResponse.getAccessToken();
        assertNull(accessToken);
        assertEquals("invalid_claim_token", tokenResponse.getError());
        assertEquals("Token issued to a different client", tokenResponse.getErrorDescription());
    }

    @Test
    public void testExpiredIdTokenClaim() {
        AccessTokenResponse tokenResponse = oauth.client(rs1.getClientId(), rs1.getSecret()).doPasswordGrantRequest(user.getUsername(), user.getPassword());
        String idToken = tokenResponse.getIdToken();
        assertNotNull(idToken);

        Permission.create(rs1)
                .resource("resource")
                .grant();

        timeOffSet.set(Duration.ofDays(1));
        tokenResponse = oauth.client(rs1.getClientId(), rs1.getSecret())
                .permissionGrantRequest()
                .claimToken(idToken)
                .send();
        String accessToken = tokenResponse.getAccessToken();
        assertNull(accessToken);
        assertEquals("invalid_claim_token", tokenResponse.getError());
        assertEquals("Expired token", tokenResponse.getErrorDescription());
    }
}
