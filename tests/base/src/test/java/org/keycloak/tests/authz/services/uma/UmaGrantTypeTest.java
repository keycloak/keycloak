package org.keycloak.tests.authz.services.uma;

import java.security.KeyPair;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.KeyUtils;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
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
import org.keycloak.tests.authz.services.Authz;
import org.keycloak.tests.authz.services.Permission;
import org.keycloak.tests.authz.services.config.DefaultAuthzServicesServerConfig;
import org.keycloak.tests.authz.services.config.DefaultResourceServerConfig;
import org.keycloak.tests.common.BasicUserConfig;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.PermissionGrantRequest;
import org.keycloak.util.DPoPGenerator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    public void testBearerAuthorization() {
        AccessTokenResponse tokens = oauth.client(rs1.getClientId(), rs1.getSecret())
                .doPasswordGrantRequest(user.getUsername(), user.getPassword());
        assertEquals(200, tokens.getStatusCode(), tokens.getErrorDescription());
        Permission.create(rs1).resource("resource").grant();

        PermissionGrantRequest request = oauth.permissionGrantRequest();
        request.bearerToken(tokens.getAccessToken());
        AccessTokenResponse response = request.send();
        assertEquals(200, response.getStatusCode(), response.getErrorDescription());
        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    public void testDPoPAuthorization() {
        KeyPair key = KeyUtils.generateRsaKeyPair(2048);
        String endpoint = oauth.getEndpoints().getToken();
        AccessTokenResponse tokens = oauth.client(rs1.getClientId(), rs1.getSecret())
                .passwordGrantRequest(user.getUsername(), user.getPassword())
                .dpopProof(DPoPGenerator.generateRsaSignedDPoPProof(key, "POST", endpoint, null)).send();
        assertEquals(200, tokens.getStatusCode(), tokens.getErrorDescription());
        String token = tokens.getAccessToken();
        Permission.create(rs1).resource("resource").grant();

        PermissionGrantRequest wrongScheme = oauth.permissionGrantRequest();
        wrongScheme.bearerToken(token);
        wrongScheme.header("DPoP", DPoPGenerator.generateRsaSignedDPoPProof(key, "POST", endpoint, token));
        assertEquals(400, wrongScheme.send().getStatusCode());
        assertEquals(400, permissionRequest(token, null).getStatusCode());
        assertEquals(400, permissionRequest(token, DPoPGenerator.generateRsaSignedDPoPProof(key, "POST", endpoint, null)).getStatusCode());
        assertEquals(400, permissionRequest(token, DPoPGenerator.generateRsaSignedDPoPProof(key, "POST", endpoint, "another-token")).getStatusCode());
        assertEquals(400, permissionRequest(token, DPoPGenerator.generateRsaSignedDPoPProof(KeyUtils.generateRsaKeyPair(2048), "POST", endpoint, token)).getStatusCode());

        String proof = DPoPGenerator.generateRsaSignedDPoPProof(key, "POST", endpoint, token);
        AccessTokenResponse response = permissionRequest(token, proof);
        assertEquals(200, response.getStatusCode(), response.getErrorDescription());
        assertEquals("DPoP", response.getTokenType());
        assertEquals(oauth.verifyToken(token).getConfirmation().getKeyThumbprint(),
                oauth.verifyToken(response.getAccessToken()).getConfirmation().getKeyThumbprint());
        assertEquals(400, permissionRequest(token, proof).getStatusCode());
    }

    @Test
    public void testPreviousDPoPBoundRpt() {
        Permission.create(rs1).resource("resource").grant();
        String token = oauth.client(rs1.getClientId(), rs1.getSecret())
                .doPasswordGrantRequest(user.getUsername(), user.getPassword()).getAccessToken();
        KeyPair key = KeyUtils.generateRsaKeyPair(2048);
        String endpoint = oauth.getEndpoints().getToken();
        PermissionGrantRequest issue = oauth.permissionGrantRequest();
        issue.bearerToken(token);
        issue.header("DPoP", DPoPGenerator.generateRsaSignedDPoPProof(key, "POST", endpoint, null));
        AccessTokenResponse issued = issue.send();
        assertEquals(200, issued.getStatusCode(), issued.getErrorDescription());
        assertEquals("DPoP", issued.getTokenType());
        String rpt = issued.getAccessToken();

        PermissionGrantRequest missingProof = oauth.permissionGrantRequest().previousRpt(rpt);
        missingProof.bearerToken(token);
        AccessTokenResponse denied = missingProof.send();
        assertEquals(403, denied.getStatusCode());
        assertEquals("invalid_rpt", denied.getError());

        PermissionGrantRequest upgrade = oauth.permissionGrantRequest().previousRpt(rpt);
        upgrade.bearerToken(token);
        upgrade.header("DPoP", DPoPGenerator.generateRsaSignedDPoPProof(key, "POST", endpoint, rpt));
        AccessTokenResponse upgraded = upgrade.send();
        assertEquals(200, upgraded.getStatusCode(), upgraded.getErrorDescription());
        assertEquals("DPoP", upgraded.getTokenType());

        PermissionGrantRequest sameCredential = oauth.permissionGrantRequest().previousRpt(rpt);
        sameCredential.dpopToken(rpt);
        sameCredential.header("DPoP", DPoPGenerator.generateRsaSignedDPoPProof(key, "POST", endpoint, rpt));
        assertEquals(200, sameCredential.send().getStatusCode());
    }

    @Test
    public void testDPoPClaimToken() throws Exception {
        KeyPair key = KeyUtils.generateRsaKeyPair(2048);
        String endpoint = oauth.getEndpoints().getToken();
        AccessTokenResponse tokens = oauth.client(rs1.getClientId(), rs1.getSecret())
                .passwordGrantRequest(user.getUsername(), user.getPassword())
                .dpopProof(DPoPGenerator.generateRsaSignedDPoPProof(key, "POST", endpoint, null)).send();
        assertEquals(200, tokens.getStatusCode(), tokens.getErrorDescription());
        Permission.create(rs1).resource("resource").grant();

        AccessTokenResponse response = oauth.permissionGrantRequest().claimToken(tokens.getAccessToken()).send();
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_claim_token", response.getError());

        PermissionGrantRequest request = oauth.permissionGrantRequest().claimToken(tokens.getAccessToken());
        request.header("DPoP", DPoPGenerator.generateRsaSignedDPoPProof(key, "POST", endpoint, tokens.getAccessToken()));
        response = request.send();
        assertEquals(200, response.getStatusCode(), response.getErrorDescription());
        assertEquals("DPoP", response.getTokenType());

        SimpleHttp http = SimpleHttp.create(oauth.httpClient().get());
        assertEquals(400, http.doPost(endpoint).authBasic(rs1.getClientId(), rs1.getSecret())
                .param("grant_type", "urn:ietf:params:oauth:grant-type:uma-ticket")
                .param("audience", rs1.getClientId()).param("subject_token", tokens.getAccessToken()).asStatus());
        try (SimpleHttpResponse formResponse = http.doPost(endpoint).authBasic(rs1.getClientId(), rs1.getSecret())
                .param("grant_type", "urn:ietf:params:oauth:grant-type:uma-ticket")
                .param("audience", rs1.getClientId()).param("subject_token", tokens.getAccessToken())
                .header("DPoP", DPoPGenerator.generateRsaSignedDPoPProof(key, "POST", endpoint, tokens.getAccessToken())).asResponse()) {
            assertEquals(200, formResponse.getStatus(), formResponse.asString());
            assertEquals("DPoP", formResponse.asJson().get("token_type").asText());
        }
    }

    @Test
    public void testUnboundClaimTokens() {
        AccessTokenResponse tokens = oauth.client(rs1.getClientId(), rs1.getSecret())
                .doPasswordGrantRequest(user.getUsername(), user.getPassword());
        assertEquals(200, tokens.getStatusCode(), tokens.getErrorDescription());
        Permission.create(rs1).resource("resource").grant();

        for (String token : new String[] {tokens.getIdToken(), tokens.getAccessToken()}) {
            AccessTokenResponse response = oauth.permissionGrantRequest().claimToken(token).send();
            assertEquals(200, response.getStatusCode(), response.getErrorDescription());
            assertEquals("Bearer", response.getTokenType());
        }
    }

    @Test
    public void testServiceAccountAccessTokenAsClaimToken() {
        rs1.updateWithCleanup(c -> c.serviceAccountsEnabled(true));
        Permission.create(rs1).resource("resource").grant();
        AccessTokenResponse tokens = oauth.client(rs1.getClientId(), rs1.getSecret()).doClientCredentialsGrantAccessTokenRequest();
        assertEquals(200, tokens.getStatusCode(), tokens.getErrorDescription());
        assertNull(oauth.verifyToken(tokens.getAccessToken()).getSessionId());

        AccessTokenResponse response = oauth.permissionGrantRequest().claimToken(tokens.getAccessToken()).send();
        assertEquals(200, response.getStatusCode(), response.getErrorDescription());
        assertEquals(oauth.verifyToken(tokens.getAccessToken()).getSubject(), oauth.verifyToken(response.getAccessToken()).getSubject());
    }

    private AccessTokenResponse permissionRequest(String token, String proof) {
        PermissionGrantRequest request = oauth.permissionGrantRequest();
        request.dpopToken(token);
        request.header("DPoP", proof);
        return request.send();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testClientCredentialsAuthorization(boolean dpop) {
        KeyPair key = KeyUtils.generateRsaKeyPair(2048);
        rs1.updateWithCleanup(c -> c.serviceAccountsEnabled(true));
        Permission.create(rs1).resource("resource").grant();

        PermissionGrantRequest request = oauth.client(rs1.getClientId(), rs1.getSecret()).permissionGrantRequest();
        if (dpop) {
            request.header("DPoP", DPoPGenerator.generateRsaSignedDPoPProof(key, "POST", oauth.getEndpoints().getToken(), null));
        }
        AccessTokenResponse response = request.send();
        assertEquals(200, response.getStatusCode(), response.getErrorDescription());
        assertEquals(dpop ? "DPoP" : "Bearer", response.getTokenType());
        if (dpop) {
            assertNotNull(oauth.verifyToken(response.getAccessToken()).getConfirmation().getKeyThumbprint());
        } else {
            assertNull(oauth.verifyToken(response.getAccessToken()).getConfirmation());
        }
    }

    @Test
    public void testIdTokenClaimUsesCurrentRoles() {
        RoleRepresentation role = new RoleRepresentation("resource-reader", null, false);
        managedRealm.admin().roles().create(role);
        role = managedRealm.admin().roles().get(role.getName()).toRepresentation();
        rs1.admin().getScopeMappings().realmLevel().add(List.of(role));

        ResourceRepresentation resource = Authz.create(rs1, ResourceRepresentation.create().name("role-protected").build());
        RolePolicyRepresentation policy = new RolePolicyRepresentation();
        policy.setName("reader policy");
        policy.addRole(role.getId());
        try (Response response = rs1.admin().authorization().policies().role().create(policy)) {
            assertEquals(201, response.getStatus());
        }
        Authz.create(rs1, ResourcePermissionRepresentation.create().name("read resource")
                .resources(Set.of(resource.getId())).policies(Set.of(policy.getName())).build());

        AccessTokenResponse login = oauth.client(rs1.getClientId(), rs1.getSecret()).doPasswordGrantRequest(user.getUsername(), user.getPassword());
        assertNotNull(login.getIdToken());
        user.admin().roles().realmLevel().add(List.of(role));
        AccessTokenResponse grant = oauth.permissionGrantRequest().claimToken(login.getIdToken()).send();
        assertEquals(200, grant.getStatusCode());
        AccessToken rpt = oauth.verifyToken(grant.getAccessToken(), AccessToken.class);
        assertTrue(rpt.getAuthorization().getPermissions().stream().anyMatch(permission -> resource.getId().equals(permission.getResourceId())));
    }

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
