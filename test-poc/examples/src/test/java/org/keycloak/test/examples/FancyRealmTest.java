package org.keycloak.test.examples;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.test.framework.annotations.InjectClient;
import org.keycloak.test.framework.annotations.InjectRealm;
import org.keycloak.test.framework.annotations.InjectUser;
import org.keycloak.test.framework.annotations.KeycloakIntegrationTest;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.realm.ClientConfig;
import org.keycloak.test.framework.realm.ManagedClient;
import org.keycloak.test.framework.realm.ManagedRealm;
import org.keycloak.test.framework.realm.ManagedUser;
import org.keycloak.test.framework.realm.RealmConfig;
import org.keycloak.test.framework.realm.UserConfig;

@KeycloakIntegrationTest
public class FancyRealmTest {

    @InjectRealm(lifecycle = LifeCycle.CLASS, config = MyRealm.class)
    ManagedRealm realm;

    @InjectClient(config = MyClient.class)
    ManagedClient client;

    @InjectUser(config = MyUser.class)
    ManagedUser user;

    @Test
    public void testCreatedRealm() {
        Assertions.assertEquals("default", realm.getName());

        Assertions.assertNotNull(realm.admin().roles().get("role-1").toRepresentation().getName());
    }

    @Test
    public void testCreatedClient() {
        Assertions.assertEquals("the-client", client.getClientId());
        Assertions.assertEquals("the-client", realm.admin().clients().get(client.getId()).toRepresentation().getClientId());
    }

    @Test
    public void testCreatedUser() {
        Assertions.assertEquals("bobthemob", user.getUsername());
        Assertions.assertEquals("bobthemob", realm.admin().users().get(user.getId()).toRepresentation().getUsername());
    }

    static class MyRealm implements RealmConfig {

        @Override
        public RealmRepresentation getRepresentation() {
            return builder()
                    .roles("role-1", "role-2")
                    .groups("group-1", "group-2")
                    .build();
        }
    }

    static class MyClient implements ClientConfig {

        @Override
        public ClientRepresentation getRepresentation() {
            return builder()
                    .clientId("the-client")
                    .redirectUris("http://127.0.0.1", "http://test")
                    .build();
        }
    }

    static class MyUser implements UserConfig {

        @Override
        public UserRepresentation getRepresentation() {
            return builder()
                    .username("bobthemob")
                    .name("Bob", "Mob")
                    .email("bob@mob")
                    .password("password")
                    .roles("role-1", "role-2") // TODO Adding role mappings when creating user is not supported!
                    .groups("/group-1")
                    .build();
        }
    }

}
