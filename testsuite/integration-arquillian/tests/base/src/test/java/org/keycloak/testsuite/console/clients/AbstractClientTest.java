package org.keycloak.testsuite.console.clients;

import java.util.ArrayList;
import java.util.List;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.clients.Client;
import org.keycloak.testsuite.console.page.clients.Clients;
import org.keycloak.testsuite.console.page.clients.CreateClient;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrl;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractClientTest extends AbstractConsoleTest {

    @Page
    protected Clients clients;
    @Page
    protected Client client; // note: cannot call navigateTo() unless client id is set
    @Page
    protected CreateClient createClient;

    @Before
    public void beforeClientTest() {
        testAdminConsoleRealm.configure().clients();
    }

    public void createClient(ClientRepresentation client) {
        assertCurrentUrl(clients);
        clients.createClient();
        createClient.form().setValues(client);
        createClient.form().save();
    }

    public static ClientRepresentation createClientRepresentation(String clientId, String redirectUri) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        if (redirectUri != null) {
            List<String> redirectUris = new ArrayList<>();
            redirectUris.add(redirectUri);
            client.setRedirectUris(redirectUris);
        }
        client.setEnabled(true);
        client.setBearerOnly(false);
        client.setDirectGrantsOnly(false);
        client.setPublicClient(false);
        return client;
    }
    
}
