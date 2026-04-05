package org.keycloak.tests.model;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.utils.ReservedCharValidator;

import static org.junit.jupiter.api.Assertions.fail;

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
        try {
            manager.createRealm(id, name + script);
            fail();
        } catch (ReservedCharValidator.ReservedCharException ex) {}
    }

    @TestOnServer
    public void testBadRealmId(KeycloakSession session) {
        RealmManager manager = new RealmManager(session);
        try {
            manager.createRealm(id + script, name);
            fail();
        } catch (ReservedCharValidator.ReservedCharException ex) {}
    }
}
