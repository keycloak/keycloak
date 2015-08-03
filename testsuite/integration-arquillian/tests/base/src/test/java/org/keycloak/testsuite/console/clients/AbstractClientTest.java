package org.keycloak.testsuite.console.clients;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.console.AbstractConsoleTest;
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
    
}
