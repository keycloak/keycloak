package org.keycloak.test.examples;

import java.util.LinkedList;

import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class CustomConfigBuilderTest {

    @InjectRealm(config = CustomRealmConfig.class)
    ManagedRealm realm;

    @InjectClient(config = CustomClientConfig.class)
    ManagedClient client;

    @InjectUser(config = CustomUserConfig.class)
    ManagedUser user;

    @Test
    public void testRealm() {
        Assertions.assertEquals(1, realm.admin().groups().query("mygroup").size());
    }

    @Test
    public void testClient() {
        Assertions.assertTrue(client.admin().toRepresentation().isBearerOnly());
    }

    @Test
    public void testUser() {
        Assertions.assertFalse(user.admin().toRepresentation().isEnabled());
    }

    public static class CustomRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            return realm.update(r -> {
                if (r.getGroups() == null) {
                    r.setGroups(new LinkedList<>());
                }
                GroupRepresentation group = new GroupRepresentation();
                group.setName("mygroup");
                group.setPath("/mygroup");
                r.getGroups().add(group);
            });
        }
    }

    public static class CustomClientConfig implements ClientConfig {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.update(u -> u.setBearerOnly(true));
        }
    }

    public static class CustomUserConfig implements UserConfig {

        @Override
        public UserConfigBuilder configure(UserConfigBuilder user) {
            return user.update(u -> u.setEnabled(false));
        }
    }

}
