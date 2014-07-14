package org.keycloak.testsuite.admin;

import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.representations.idm.OAuthClientRepresentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OAuthClientTest extends AbstractClientTest {

    @Test
    public void getOAuthClients() {
        assertTrue(realm.oAuthClients().findAll().isEmpty());
    }

    @Test
    public void createOAuthClient() {
        OAuthClientRepresentation rep = new OAuthClientRepresentation();
        rep.setName("my-client");
        rep.setEnabled(true);
        realm.oAuthClients().create(rep);

        assertNames(realm.oAuthClients().findAll(), "my-client");
    }

    @Test
    @Ignore
    // TODO For some reason clients are retrieved using id, not client-id
    public void removeOAuthClient() {
        createOAuthClient();

        realm.oAuthClients().get("my-client").remove();
    }

    @Test
    @Ignore
    // TODO For some reason clients are retrieved using id, not client-id
    public void getOAuthClientRepresentation() {
        createOAuthClient();

        OAuthClientRepresentation rep = realm.oAuthClients().get("my-client").toRepresentation();
        assertEquals("my-client", rep.getName());
        assertTrue(rep.isEnabled());
    }

}
