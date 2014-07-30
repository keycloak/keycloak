package org.keycloak.testsuite.admin;

import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.RealmManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RealmTest extends AbstractClientTest {

    @Test
    public void getRealms() {
        assertNames(keycloak.realms().findAll(), "master", "test", REALM_NAME);
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
    public void getRealmRepresentation() {
        RealmRepresentation rep = realm.toRepresentation();
        assertEquals(REALM_NAME, rep.getRealm());
        assertTrue(rep.isEnabled());
    }

}
