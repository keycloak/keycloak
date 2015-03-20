package org.keycloak.testsuite.admin;

import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.services.managers.RealmManager;

import javax.ws.rs.NotFoundException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RealmTest extends AbstractClientTest {

    @Test
    public void getRealms() {
        List<RealmRepresentation> realms = keycloak.realms().findAll();
        assertNames(realms, "master", "test", REALM_NAME);

        for (RealmRepresentation rep : realms) {
            assertNull(rep.getPrivateKey());
            assertNull(rep.getCodeSecret());
            assertNotNull(rep.getPublicKey());
            assertNotNull(rep.getCertificate());
        }
    }

    @Test
    public void createRealm() {
        try {
            RealmRepresentation rep = new RealmRepresentation();
            rep.setRealm("new-realm");

            keycloak.realms().create(rep);

            assertNames(keycloak.realms().findAll(), "master", "test", REALM_NAME, "new-realm");
        } finally {
            KeycloakSession session = keycloakRule.startSession();
            RealmManager manager = new RealmManager(session);
            RealmModel newRealm = manager.getRealmByName("new-realm");
            if (newRealm != null) {
                manager.removeRealm(newRealm);
            }
            keycloakRule.stopSession(session, true);
        }
    }

    @Test
    public void removeRealm() {
        realm.remove();

        assertNames(keycloak.realms().findAll(), "master", "test");
    }

    @Test
    public void updateRealm() {
        // first change
        RealmRepresentation rep = realm.toRepresentation();
        rep.setSsoSessionIdleTimeout(123);
        rep.setSsoSessionMaxLifespan(12);
        rep.setAccessCodeLifespanLogin(1234);
        rep.setRegistrationAllowed(true);
        rep.setRegistrationEmailAsUsername(true);

        realm.update(rep);

        rep = realm.toRepresentation();

        assertEquals(123, rep.getSsoSessionIdleTimeout().intValue());
        assertEquals(12, rep.getSsoSessionMaxLifespan().intValue());
        assertEquals(1234, rep.getAccessCodeLifespanLogin().intValue());
        assertEquals(Boolean.TRUE, rep.isRegistrationAllowed());
        assertEquals(Boolean.TRUE, rep.isRegistrationEmailAsUsername());

        // second change
        rep.setRegistrationAllowed(false);
        rep.setRegistrationEmailAsUsername(false);

        realm.update(rep);

        rep = realm.toRepresentation();
        assertEquals(Boolean.FALSE, rep.isRegistrationAllowed());
        assertEquals(Boolean.FALSE, rep.isRegistrationEmailAsUsername());
    }

    @Test
    public void getRealmRepresentation() {
        RealmRepresentation rep = realm.toRepresentation();
        assertEquals(REALM_NAME, rep.getRealm());
        assertTrue(rep.isEnabled());

        assertNull(rep.getPrivateKey());
        assertNull(rep.getCodeSecret());
        assertNotNull(rep.getPublicKey());
        assertNotNull(rep.getCertificate());
    }

    @Test
    // KEYCLOAK-1110
    public void deleteDefaultRole() {
        RoleRepresentation role = new RoleRepresentation("test", "test");
        realm.roles().create(role);

        assertNotNull(realm.roles().get("test").toRepresentation());

        RealmRepresentation rep = realm.toRepresentation();
        rep.setDefaultRoles(new LinkedList<String>());
        rep.getDefaultRoles().add("test");

        realm.update(rep);

        realm.roles().deleteRole("test");

        try {
            realm.roles().get("testsadfsadf").toRepresentation();
            fail("Expected NotFoundException");
        } catch (NotFoundException e) {
        }
    }

}
