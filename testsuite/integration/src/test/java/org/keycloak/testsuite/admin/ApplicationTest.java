package org.keycloak.testsuite.admin;

import org.junit.Test;
import org.keycloak.representations.idm.ApplicationRepresentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ApplicationTest extends AbstractClientTest {

    @Test
    public void getApplications() {
        assertNames(realm.applications().findAll(), "account", "realm-management", "security-admin-console");
    }

    @Test
    public void createApplication() {
        ApplicationRepresentation rep = new ApplicationRepresentation();
        rep.setName("my-app");
        rep.setEnabled(true);
        realm.applications().create(rep);

        assertNames(realm.applications().findAll(), "account", "realm-management", "security-admin-console", "my-app");
    }

    @Test
    public void removeApplication() {
        createApplication();

        realm.applications().get("my-app").remove();
    }

    @Test
    public void getApplicationRepresentation() {
        createApplication();

        ApplicationRepresentation rep = realm.applications().get("my-app").toRepresentation();
        assertEquals("my-app", rep.getName());
        assertTrue(rep.isEnabled());
    }



}
