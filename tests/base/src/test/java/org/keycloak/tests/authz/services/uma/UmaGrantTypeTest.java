package org.keycloak.tests.authz.services.uma;

import java.security.KeyPair;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.https.InjectCertificates;
import org.keycloak.testframework.https.ManagedCertificates;
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
import org.keycloak.tests.oauth.HolderOfKeyTokenTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.PermissionGrantRequest;
import org.keycloak.util.DPoPGenerator;
import org.keycloak.util.JWKSUtils;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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

    @InjectCertificates(config = HolderOfKeyTokenTest.Certificates.class)
    ManagedCertificates certificates;

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
    public void testCertificateBoundRptForAnotherResourceServer() throws Exception {
        rs1.updateWithCleanup(c -> c.attribute(OIDCConfigAttributes.USE_MTLS_HOK_TOKEN, "true"));
        for (String audience : List.of(rs2.getClientId(), "resource-api")) {
            try (Response response = rs2.admin().getProtocolMappers().createMapper(ModelToRepresentation.toRepresentation(
                    AudienceProtocolMapper.createClaimMapper(audience, null, audience, true, false, false)))) {
                assertEquals(201, response.getStatus());
            }
        }
        Permission.create(rs2).resource("first").grant();
        Permission.create(rs2).resource("second").grant();
        AccessTokenResponse login = oauth.client(rs1.getClientId(), rs1.getSecret())
                .doPasswordGrantRequest(user.getUsername(), user.getPassword());
        assertEquals(200, login.getStatusCode(), login.getErrorDescription());
        byte[] certificate = certificates.getClientKeyStore().getCertificate("client-key").getEncoded();
        String thumbprint = Base64Url.encode(MessageDigest.getInstance("SHA-256").digest(certificate));
        assertEquals(thumbprint, oauth.verifyToken(login.getAccessToken()).getConfirmation().getCertThumbprint());

        PermissionGrantRequest issue = oauth.client(rs2.getClientId(), rs2.getSecret()).permissionGrantRequest().permission("first");
        issue.bearerToken(login.getAccessToken());
        AccessTokenResponse issued = issue.send();
        assertEquals(200, issued.getStatusCode(), issued.getErrorDescription());

        PermissionGrantRequest upgrade = oauth.permissionGrantRequest().previousRpt(issued.getAccessToken()).permission("second");
        upgrade.bearerToken(login.getAccessToken());
        AccessTokenResponse upgraded = upgrade.send();
        assertEquals(200, upgraded.getStatusCode(), upgraded.getErrorDescription());
        assertCertificateBoundRpt(issued, thumbprint, Set.of("first"));
        assertCertificateBoundRpt(upgraded, thumbprint, Set.of("first", "second"));
        String refreshToken = upgraded.getRefreshToken();
        AccessToken.Confirmation confirmation = oauth.parseRefreshToken(refreshToken).getConfirmation();
        assertNotNull(confirmation);
        assertEquals(thumbprint, confirmation.getCertThumbprint());

        try (CloseableHttpClient missing = HttpClients.custom().setSSLContext(certificates.createClientSSLContext(null, "keycloak-truststore.bcfks", false)).build();
             CloseableHttpClient different = HttpClients.custom().setSSLContext(certificates.createClientSSLContext("keycloak.bcfks", "keycloak-truststore.bcfks", true)).build()) {
            // The ID token identifies the user independently of the certificate needed for RPT issuance.
            try (SimpleHttpResponse response = SimpleHttp.create(missing).doPost(oauth.getEndpoints().getToken())
                    .authBasic(rs1.getClientId(), rs1.getSecret()).param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.UMA_GRANT_TYPE)
                    .param("audience", rs2.getClientId()).param("claim_token", login.getIdToken()).param("permission", "first").asResponse()) {
                assertEquals(400, response.getStatus());
                assertEquals("invalid_request", response.asJson().get("error").asText());
            }
            for (CloseableHttpClient client : List.of(missing, different)) {
                try (SimpleHttpResponse response = SimpleHttp.create(client).doPost(oauth.getEndpoints().getToken())
                        .authBasic(rs1.getClientId(), rs1.getSecret()).param("grant_type", OAuth2Constants.UMA_GRANT_TYPE)
                        .param("audience", rs2.getClientId()).param("claim_token", login.getIdToken())
                        .param("rpt", issued.getAccessToken()).asResponse()) {
                    assertEquals(403, response.getStatus());
                    assertEquals("invalid_rpt", response.asJson().get("error").asText());
                }
                try (SimpleHttpResponse response = SimpleHttp.create(client).doPost(oauth.getEndpoints().getToken())
                        .authBasic(rs1.getClientId(), rs1.getSecret()).param(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN)
                        .param(OAuth2Constants.REFRESH_TOKEN, refreshToken).asResponse()) {
                    assertEquals(401, response.getStatus());
                    assertEquals("unauthorized_client", response.asJson().get("error").asText());
                }
            }
        }
        AccessTokenResponse refreshed = oauth.client(rs1.getClientId(), rs1.getSecret()).doRefreshTokenRequest(refreshToken);
        assertEquals(200, refreshed.getStatusCode(), refreshed.getErrorDescription());
        assertEquals(thumbprint, oauth.parseRefreshToken(refreshed.getRefreshToken()).getConfirmation().getCertThumbprint());
    }

    @Test
    public void testResourceServerCertificateSettingDoesNotBindRequestingClient() {
        rs2.updateWithCleanup(c -> c.attribute(OIDCConfigAttributes.USE_MTLS_HOK_TOKEN, "true"));
        Permission.create(rs2).resource("first").grant();
        AccessTokenResponse login = oauth.client(rs1.getClientId(), rs1.getSecret()).doPasswordGrantRequest(user.getUsername(), user.getPassword());
        assertEquals(200, login.getStatusCode(), login.getErrorDescription());
        PermissionGrantRequest request = oauth.client(rs2.getClientId(), rs2.getSecret()).permissionGrantRequest().permission("first");
        request.bearerToken(login.getAccessToken());
        AccessTokenResponse issued = request.send();
        assertEquals(200, issued.getStatusCode(), issued.getErrorDescription());
        AccessToken rpt = oauth.verifyToken(issued.getAccessToken());
        assertEquals(rs1.getClientId(), rpt.getIssuedFor());
        assertTrue(rpt.hasAudience(rs2.getClientId()));
        assertNull(rpt.getConfirmation());
    }

    private void assertCertificateBoundRpt(AccessTokenResponse response, String thumbprint, Set<String> resources) {
        AccessToken rpt = oauth.verifyToken(response.getAccessToken());
        assertEquals(rs1.getClientId(), rpt.getIssuedFor());
        assertTrue(rpt.hasAudience(rs2.getClientId()));
        assertTrue(rpt.hasAudience("resource-api"));
        assertEquals(thumbprint, rpt.getConfirmation().getCertThumbprint());
        assertEquals(resources, rpt.getAuthorization().getPermissions().stream().map(p -> p.getResourceName()).collect(Collectors.toSet()));
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
        assertNull(oauth.parseRefreshToken(issued.getRefreshToken()).getConfirmation());
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
    public void testPublicClientDPoPRefreshTokenForAnotherResourceServer() {
        rs1.updateWithCleanup(c -> c.authorizationServicesEnabled(false).publicClient());
        AccessTokenResponse login = oauth.client(rs1.getClientId()).doPasswordGrantRequest(user.getUsername(), user.getPassword());
        assertEquals(200, login.getStatusCode(), login.getErrorDescription());
        Permission.create(rs2).resource("resource").grant();

        String endpoint = oauth.getEndpoints().getToken();
        KeyPair key = KeyUtils.generateRsaKeyPair(2048);
        PermissionGrantRequest issue = oauth.client(rs2.getClientId(), rs2.getSecret()).permissionGrantRequest().permission("resource");
        issue.bearerToken(login.getAccessToken());
        issue.header("DPoP", DPoPGenerator.generateRsaSignedDPoPProof(key, "POST", endpoint, null));
        AccessTokenResponse issued = issue.send();
        assertEquals(200, issued.getStatusCode(), issued.getErrorDescription());
        String thumbprint = JWKSUtils.computeThumbprint(DPoPGenerator.createRsaJwk(key.getPublic()));
        assertEquals(thumbprint, oauth.verifyToken(issued.getAccessToken()).getConfirmation().getKeyThumbprint());
        String refreshToken = issued.getRefreshToken();
        AccessToken.Confirmation confirmation = oauth.parseRefreshToken(refreshToken).getConfirmation();
        assertNotNull(confirmation);
        assertEquals(thumbprint, confirmation.getKeyThumbprint());

        oauth.client(rs1.getClientId());
        AccessTokenResponse missing = oauth.doRefreshTokenRequest(refreshToken);
        assertEquals(400, missing.getStatusCode());
        assertEquals("invalid_grant", missing.getError());
        AccessTokenResponse different = oauth.refreshRequest(refreshToken)
                .dpopProof(DPoPGenerator.generateRsaSignedDPoPProof(KeyUtils.generateRsaKeyPair(2048), "POST", endpoint, null)).send();
        assertEquals(400, different.getStatusCode());
        assertEquals("invalid_grant", different.getError());
        AccessTokenResponse refreshed = oauth.refreshRequest(refreshToken)
                .dpopProof(DPoPGenerator.generateRsaSignedDPoPProof(key, "POST", endpoint, null)).send();
        assertEquals(200, refreshed.getStatusCode(), refreshed.getErrorDescription());
        assertEquals(thumbprint, oauth.parseRefreshToken(refreshed.getRefreshToken()).getConfirmation().getKeyThumbprint());
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
    public void testClientCredentialsAuthorization(boolean dpopRequired) {
        rs1.updateWithCleanup(c -> c.serviceAccountsEnabled(true)
                .attribute(OIDCConfigAttributes.DPOP_BOUND_ACCESS_TOKENS, Boolean.toString(dpopRequired)));
        oauth.client(rs1.getClientId(), rs1.getSecret());
        if (dpopRequired) {
            AccessTokenResponse denied = oauth.permissionGrantRequest().send();
            assertEquals(400, denied.getStatusCode());
            assertEquals("DPoP proof is missing", denied.getErrorDescription());
        }

        Permission.create(rs1).resource("resource").grant();
        if (!dpopRequired) {
            AccessTokenResponse bearer = oauth.permissionGrantRequest().send();
            assertEquals(200, bearer.getStatusCode(), bearer.getErrorDescription());
            assertEquals("Bearer", bearer.getTokenType());
            assertNull(oauth.verifyToken(bearer.getAccessToken()).getConfirmation());
        }

        KeyPair key = KeyUtils.generateRsaKeyPair(2048);
        PermissionGrantRequest request = oauth.permissionGrantRequest();
        request.header("DPoP", DPoPGenerator.generateRsaSignedDPoPProof(key, "POST", oauth.getEndpoints().getToken(), null));
        AccessTokenResponse response = request.send();
        assertEquals(200, response.getStatusCode(), response.getErrorDescription());
        assertEquals("DPoP", response.getTokenType());
        assertEquals(JWKSUtils.computeThumbprint(DPoPGenerator.createRsaJwk(key.getPublic())),
                oauth.verifyToken(response.getAccessToken()).getConfirmation().getKeyThumbprint());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testOidcClaimUsesCurrentRoles(boolean useIdToken) {
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
        String claim = useIdToken ? login.getIdToken() : login.getAccessToken();
        assertNotNull(claim);
        user.admin().roles().realmLevel().add(List.of(role));
        AccessTokenResponse grant = oauth.permissionGrantRequest().claimToken(claim).send();
        assertEquals(200, grant.getStatusCode());
        AccessToken rpt = oauth.verifyToken(grant.getAccessToken(), AccessToken.class);
        assertTrue(rpt.getAuthorization().getPermissions().stream().anyMatch(permission -> resource.getId().equals(permission.getResourceId())));

        AccessTokenResponse withRole = oauth.doPasswordGrantRequest(user.getUsername(), user.getPassword());
        String claimWithRole = useIdToken ? withRole.getIdToken() : withRole.getAccessToken();
        assertEquals(200, oauth.permissionGrantRequest().claimToken(claimWithRole).send().getStatusCode());
        user.admin().roles().realmLevel().remove(List.of(role));
        assertEquals(403, oauth.permissionGrantRequest().claimToken(claimWithRole).send().getStatusCode());
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
