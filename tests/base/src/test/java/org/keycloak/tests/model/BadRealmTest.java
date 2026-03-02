package org.keycloak.tests.model;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.utils.ReservedCharValidator;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;

import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest
public class BadRealmTest {

    @InjectRealm(attachTo = "master")
    ManagedRealm realm;

    private String name = "MyRealm";
    private String id = "MyId";
    private String script = "<script>alert(4)</script>";

    @TestOnServer
    public void testBadRealmName(KeycloakSession session) {
        RealmManager manager = new RealmManager(session);
        assertThrows(ReservedCharValidator.ReservedCharException.class, () ->
            manager.createRealm(id, name + script)
        );
    }

    @TestOnServer
    public void testBaseRealmWithAnsiControlCharacter(KeycloakSession session) {
        RealmManager manager = new RealmManager(session);
        ReservedCharValidator.ReservedCharException ex = assertThrows(ReservedCharValidator.ReservedCharException.class, () ->
                manager.createRealm(id, name + "\u001B")
        );
        MatcherAssert.assertThat(ex.getMessage(), Matchers.containsString("0x1b"));
    }

    @TestOnServer
    public void testBadRealmId(KeycloakSession session) {
        RealmManager manager = new RealmManager(session);
        assertThrows(ReservedCharValidator.ReservedCharException.class, () ->
            manager.createRealm(id + script, name)
        );
    }
}
