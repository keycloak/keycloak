package org.keycloak.testframework.tests;

import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectDependency;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
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
import org.keycloak.testframework.server.KeycloakUrls;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class ConfigWithInjectionTest {

    @InjectRealm(config = MyRealm.class)
    ManagedRealm realm;

    @InjectClient(config = MyClient.class)
    ManagedClient client;

    @InjectUser(config = MyUser.class)
    ManagedUser user;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    public void hello() {
        Assertions.assertEquals(keycloakUrls.getBase(), realm.admin().toRepresentation().getDisplayName());
        Assertions.assertEquals(keycloakUrls.getAdmin(), client.admin().toRepresentation().getRedirectUris().get(0));

        UserRepresentation user = this.user.admin().toRepresentation();
        Assertions.assertEquals("realm-" + realm.getName(), user.getFirstName());
    }

    public static class MyRealm implements RealmConfig {

        @InjectDependency
        KeycloakUrls keycloakUrls;

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            return realm.displayName(keycloakUrls.getBase());
        }
    }

    public static class MyClient implements ClientConfig {

        @InjectDependency
        KeycloakUrls keycloakUrls;

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return client.redirectUris(keycloakUrls.getAdmin());
        }
    }

    public static class MyUser implements UserConfig {

        @InjectDependency
        ManagedRealm managedRealm;

        @Override
        public UserConfigBuilder configure(UserConfigBuilder user) {
            return user.firstName("realm-" + managedRealm.getName());
        }
    }

}
