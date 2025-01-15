package org.keycloak.test.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.runonserver.InjectRunOnServer;
import org.keycloak.testframework.runonserver.RunOnServerClient;

@KeycloakIntegrationTest
public class RunOnServerTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void testRunOnServer1() {
        String realmName = realm.getName();
        String groupName = "default-group";

        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            GroupModel g = realm.createGroup(groupName);
            realm.removeGroup(g);
        });

        Assertions.assertTrue(realm.admin().groups().groups().isEmpty());
    }

    @Test
    public void testFetchOnServer() {
        String realmName = realm.getName();
        String groupName = "default-group";

        final String id = runOnServer.fetch(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            GroupModel g = realm.createGroup(groupName);
            return g.getId();
        }, String.class);

        Assertions.assertEquals(groupName, realm.admin().groups().group(id).toRepresentation().getName());
    }

    @Test
    public void testRunOnServer2() {
        runOnServer.run(session -> {
            EventStoreProvider provider = session.getProvider(EventStoreProvider.class);
            Assertions.assertEquals(1, provider.createQuery().realm(realm.getId()).getResultStream().count());
        });
    }
}
