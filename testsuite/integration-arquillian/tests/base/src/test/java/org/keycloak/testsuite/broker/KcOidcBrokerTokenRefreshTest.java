package org.keycloak.testsuite.broker;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.util.JsonSerialization;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_ALIAS;

public class KcOidcBrokerTokenRefreshTest extends AbstractInitializedBaseBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerConfiguration.INSTANCE;
    }

    @Test
    public void testTokenRefreshUpdatesDatabase() throws Exception {
        // Ensure store token is enabled
        RealmResource consumerRealm = realmsResouce().realm(bc.consumerRealmName());
        IdentityProviderResource identityProviderResource = consumerRealm.identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation representation = identityProviderResource.toRepresentation();
        representation.setStoreToken(true);
        identityProviderResource.update(representation);

        // 1. Login to IDP to get initial token
        logInAsUserInIDPForFirstTimeAndAssertSuccess();

        // Setup permissions and password for the federated user
        testingClient.server(BrokerTestConstants.REALM_CONS_NAME).run(KcOidcBrokerTokenRefreshTest::setupUserForApiAccess);

        // 2. Get DB Token
        String initialTokenJson = getFederatedIdentityToken(bc.consumerRealmName(), bc.getUserLogin(), bc.getIDPAlias());
        
        // 3. Time Travel
        // Expire the access token from the provider
        Integer accessTokenLifespan = adminClient.realm(bc.providerRealmName()).toRepresentation().getAccessTokenLifespan();
        int offset = (accessTokenLifespan != null ? accessTokenLifespan : 60) + 10;
        setTimeOffset(offset);

        // 4. Retrieve Token via API (Triggers Broker Refresh)
        // Login to Consumer to get access token for the user
        oauth.realm(bc.consumerRealmName());
        oauth.client("broker-app", "broker-app-secret");
        org.keycloak.testsuite.util.oauth.AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(bc.getUserLogin(), bc.getUserPassword());
        assertThat(tokenResponse.getAccessToken(), not(equalTo(null)));

        try (Client client = AdminClientUtil.createResteasyClient()) {
             Response response = client.target(OAuthClient.AUTH_SERVER_ROOT)
                .path("/realms/" + bc.consumerRealmName() + "/broker/" + bc.getIDPAlias() + "/token")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResponse.getAccessToken())
                .get();

             if (response.getStatus() != 200) {
                 String body = response.readEntity(String.class);
                 throw new RuntimeException("Retrieve Token failed with " + response.getStatus() + ": " + body);
             }
             assertThat(response.getStatus(), equalTo(200));
        }

        // 5. Get DB Token again
        String newTokenJson = getFederatedIdentityToken(bc.consumerRealmName(), bc.getUserLogin(), bc.getIDPAlias());

        // 6. Assert
        assertThat(newTokenJson, not(equalTo(initialTokenJson)));
    }

    private String getFederatedIdentityToken(String realmName, String username, String idpAlias) {
        return testingClient.server(realmName).fetch(session -> {
            RealmModel realm = session.getContext().getRealm();
            if (realm == null) {
                realm = session.realms().getRealmByName(realmName);
            }
            UserModel user = session.users().getUserByUsername(realm, username);
            if (user == null) {
                throw new RuntimeException("User not found: " + username);
            }
            FederatedIdentityModel identity = session.users().getFederatedIdentity(realm, user, idpAlias);
            if (identity == null) {
                throw new RuntimeException("Federated Identity not found for user: " + username + ", idp: " + idpAlias);
            }
            return identity.getToken();
        }, String.class);
    }

    private static void setupUserForApiAccess(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        if (realm == null) {
             realm = session.realms().getRealmByName("consumer"); 
        }
        if (realm == null) {
             throw new RuntimeException("Realm not found: consumer");
        }

        ClientModel brokerClient = realm.getClientByClientId("broker"); 
        if (brokerClient == null) {
            throw new RuntimeException("Client 'broker' not found");
        }
        RoleModel readTokenRole = brokerClient.getRole("read-token"); 
        if (readTokenRole == null) {
            throw new RuntimeException("Role 'read-token' not found in broker client");
        }
        
        // Find user
        UserModel user = session.users().getUserByUsername(realm, "testuser");
        if (user == null) {
             throw new RuntimeException("User 'testuser' not found in realm " + realm.getName());
        }
        
        user.grantRole(readTokenRole);
        user.credentialManager().updateCredential(UserCredentialModel.password("password"));
    }
}
