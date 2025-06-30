package org.keycloak.testsuite.model;

import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;
import org.keycloak.utils.ReservedCharValidator;

import java.util.List;

import static org.junit.Assert.fail;

public class BadRealmTest extends AbstractKeycloakTest {
    private String name = "MyRealm";
    private String id = "MyId";
    private String script = "<script>alert(4)</script>";

    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    @Test
    @ModelTest
    public void testBadRealmName(KeycloakSession session) {
        RealmManager manager = new RealmManager(session);
        try {
            manager.createRealm(id, name + script);
            fail();
        } catch (ReservedCharValidator.ReservedCharException ex) {}
    }

    @Test
    @ModelTest
    public void testBadRealmId(KeycloakSession session) {
        RealmManager manager = new RealmManager(session);
        try {
            manager.createRealm(id + script, name);
            fail();
        } catch (ReservedCharValidator.ReservedCharException ex) {}
    }
}
