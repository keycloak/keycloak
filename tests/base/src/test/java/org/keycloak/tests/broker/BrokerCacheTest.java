package org.keycloak.tests.broker;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@KeycloakIntegrationTest
public class BrokerCacheTest {

    private static final String IDP_ALIAS = "oidc";
    private static final String USERNAME = "test-user";
    private static final String BROKER_USER_ID = "broker-user-id";

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void reLoginAfterIdpDeletedAndRecreatedWithSameAlias() {
        String realmName = managedRealm.getName();

        createIdentityProvider();

        UserRepresentation user = new UserRepresentation();
        user.setUsername(USERNAME);
        String userId;
        try (Response response = managedRealm.admin().users().create(user)) {
            userId = ApiUtil.getCreatedId(response);
        }

        FederatedIdentityRepresentation link = new FederatedIdentityRepresentation();
        link.setIdentityProvider(IDP_ALIAS);
        link.setUserId(BROKER_USER_ID);
        link.setUserName(USERNAME);
        try (Response response = managedRealm.admin().users().get(userId).addFederatedIdentity(IDP_ALIAS, link)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // Prime the users cache with the reverse getUserByFederatedIdentity lookup in its own transaction.
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);

            FederatedIdentityModel socialLink = new FederatedIdentityModel(IDP_ALIAS, BROKER_USER_ID, USERNAME);
            UserModel found = session.users().getUserByFederatedIdentity(realm, socialLink);
            assertNotNull(found, "user should be found by its federated identity");
            assertEquals(USERNAME, found.getUsername());
        });

        managedRealm.admin().identityProviders().get(IDP_ALIAS).remove();

        createIdentityProvider();

        // The cached reverse lookup must not survive the identity provider removal.
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);

            FederatedIdentityModel socialLink = new FederatedIdentityModel(IDP_ALIAS, BROKER_USER_ID, USERNAME);
            UserModel staleUser = session.users().getUserByFederatedIdentity(realm, socialLink);
            assertNull(staleUser);
        });
    }

    @Test
    public void testFederatedIdentityCacheDelimiterCollision() {
        String realmName = managedRealm.getName();

        createIdentityProvider("corp.saml");
        createIdentityProvider("corp");

        UserRepresentation user1 = new UserRepresentation();
        user1.setUsername("alice");
        String user1Id;
        try (Response response = managedRealm.admin().users().create(user1)) {
            user1Id = ApiUtil.getCreatedId(response);
        }

        FederatedIdentityRepresentation link1 = new FederatedIdentityRepresentation();
        link1.setIdentityProvider("corp.saml");
        link1.setUserId("alice");
        link1.setUserName("alice");
        try (Response response = managedRealm.admin().users().get(user1Id).addFederatedIdentity("corp.saml", link1)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // Prime cache with (corp.saml, alice)
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);

            FederatedIdentityModel socialLink = new FederatedIdentityModel("corp.saml", "alice", "alice");
            UserModel found = session.users().getUserByFederatedIdentity(realm, socialLink);
            assertNotNull(found);
            assertEquals("alice", found.getUsername());
        });

        // Lookup (corp, saml.alice) must NOT return alice due to delimiter collision
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);

            FederatedIdentityModel socialLink = new FederatedIdentityModel("corp", "saml.alice", "saml.alice");
            UserModel found = session.users().getUserByFederatedIdentity(realm, socialLink);
            assertNull(found, "Lookup for (corp, saml.alice) should not collide with (corp.saml, alice)");
        });
    }

    private void createIdentityProvider() {
        createIdentityProvider(IDP_ALIAS);
    }

    private void createIdentityProvider(String alias) {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setAlias(alias);
        idp.setProviderId("oidc");
        idp.setEnabled(true);
        managedRealm.admin().identityProviders().create(idp).close();
    }
}
