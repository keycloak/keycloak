package org.keycloak.tests.account;

import java.io.IOException;
import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.Profile;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.IssuedVerifiableCredentialModel;
import org.keycloak.models.UserVerifiableCredentialModel;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.oid4vc.IssuedVerifiableCredentialRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


// should be annotated with @DatabaseTest, but excluded for now to keep test scope minimal
@KeycloakIntegrationTest(config = AccountIssuedVerifiableCredentialTest.VCServerConfig.class)
public class AccountIssuedVerifiableCredentialTest {

    private static final String PASSWORD = "password";
    private static final String CREDENTIAL_TYPE = "test-vc-scope";
    private static final String USER_A = "user-a";
    private static final String USER_B = "user-b";

    @InjectRealm(config = VCAccountRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    private String credentialScopeId;

    @BeforeEach
    void ensureCredentialScope() {
        credentialScopeId = managedRealm.admin().clientScopes().findAll().stream()
                .filter(s -> CREDENTIAL_TYPE.equals(s.getName()))
                .map(ClientScopeRepresentation::getId)
                .findFirst()
                .orElseGet(() -> {
                    CredentialScopeRepresentation scope = new CredentialScopeRepresentation(CREDENTIAL_TYPE);
                    scope.setIncludeInTokenScope(true);
                    scope.setCredentialConfigurationId(CREDENTIAL_TYPE);
                    Response resp = managedRealm.admin().clientScopes().create(scope);
                    resp.close();
                    return managedRealm.admin().clientScopes().findAll().stream()
                            .filter(s -> CREDENTIAL_TYPE.equals(s.getName()))
                            .findFirst().orElseThrow().getId();
                });
    }

    @Test
    public void testDeleteRejectsCredentialNotOwnedByAuthenticatedUser() throws IOException {
        String userBId = getUserId(USER_B);
        String credentialId = createIssuedVcForUser(userBId);

        String tokenA = getAccountToken(USER_A);

        try (SimpleHttpResponse response = simpleHttp
                .doDelete(getAccountUrl("issued-verifiable-credentials/" + credentialId))
                .acceptJson()
                .auth(tokenA)
                .asResponse()) {
            assertEquals(404, response.getStatus(), "User A should not be able to delete User B's credential");
        }

        boolean credentialStillExists = managedRealm.admin().users().get(userBId)
                .verifiableCredentials().getIssuedCredentials().stream()
                .anyMatch(c -> credentialId.equals(c.getId()));
        assertTrue(credentialStillExists, "User B's credential should still exist");
    }

    @Test
    public void testDeleteOwnCredentialsSucceeds() throws IOException {
        String userAId = getUserId(USER_A);
        String credentialId = createIssuedVcForUser(userAId);

        String tokenA = getAccountToken(USER_A);

        try (SimpleHttpResponse response = simpleHttp
                .doDelete(getAccountUrl("issued-verifiable-credentials/" + credentialId))
                .acceptJson()
                .auth(tokenA)
                .asResponse()) {
            assertEquals(204, response.getStatus(), "User A should be able to delete their own credential");
        }

        List<IssuedVerifiableCredentialRepresentation> userACreds = managedRealm.admin().users().get(userAId)
                .verifiableCredentials().getIssuedCredentials();
        assertThat("User A's credential should be deleted", userACreds, hasSize(0));
    }

    @Test
    public void testOnListOnlyShowOwnCredentials() throws IOException {
        String userAId = getUserId(USER_A);
        String userBId = getUserId(USER_B);
        createIssuedVcForUser(userAId);
        createIssuedVcForUser(userBId);

        String tokenA = getAccountToken(USER_A);

        List<IssuedVerifiableCredentialRepresentation> credentials = simpleHttp
                .doGet(getAccountUrl("issued-verifiable-credentials"))
                .auth(tokenA)
                .asJson(new TypeReference<>() {});

        assertThat(credentials, hasSize(1));
        assertEquals(userAId, credentials.get(0).getUserId());
    }

    public static class VCAccountRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .verifiableCredentialsEnabled(true)
                    .users(
                            UserBuilder.create(USER_A)
                                    .name("User", "A")
                                    .email("usera@test.local")
                                    .password(PASSWORD)
                                    .clientRoles(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID,
                                            AccountRoles.MANAGE_ACCOUNT,
                                            AccountRoles.VIEW_PROFILE),
                            UserBuilder.create(USER_B)
                                    .name("User", "B")
                                    .email("userb@test.local")
                                    .password(PASSWORD)
                                    .clientRoles(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID,
                                            AccountRoles.MANAGE_ACCOUNT,
                                            AccountRoles.VIEW_PROFILE)
                    );
        }
    }

    private String getAccountUrl(String resource) {
        return managedRealm.getBaseUrl() + "/account/" + resource;
    }

    private String getUserId(String username) {
        return managedRealm.admin().users().search(username).get(0).getId();
    }

    private String getAccountToken(String username) {
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(username, PASSWORD);
        assertTrue(tokenResponse.isSuccess(),
                "Token request failed for " + username + ": " + tokenResponse.getErrorDescription());
        return tokenResponse.getAccessToken();
    }

    private String createIssuedVcForUser(String userId) {
        managedRealm.admin().users().get(userId)
                .verifiableCredentials().getIssuedCredentials()
                .forEach(c -> managedRealm.admin().users().get(userId)
                        .verifiableCredentials().revokeIssuedCredential(c.getId()));

        String scopeId = credentialScopeId;
        runOnServer.run(session -> {
            String vcId = session.users().getVerifiableCredentialsByUser(userId)
                    .filter(vc -> scopeId.equals(vc.getClientScopeId()))
                    .map(UserVerifiableCredentialModel::getId)
                    .findFirst()
                    .orElseGet(() -> {
                        UserVerifiableCredentialModel vcModel = new UserVerifiableCredentialModel(null, scopeId);
                        vcModel.setRevision("rev-001");
                        return session.users().addVerifiableCredential(userId, vcModel).getId();
                    });
            IssuedVerifiableCredentialModel issuedModel = new IssuedVerifiableCredentialModel(userId, vcId, null);
            issuedModel.setRevision("rev-001");
            session.users().addIssuedVerifiableCredential(issuedModel);
        });

        return managedRealm.admin().users().get(userId)
                .verifiableCredentials().getIssuedCredentials()
                .get(0).getId();
    }

    public static class VCServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.OID4VC_VCI);
        }
    }
}
