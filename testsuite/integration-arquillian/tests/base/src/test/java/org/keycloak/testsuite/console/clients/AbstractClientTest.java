package org.keycloak.testsuite.console.clients;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.keycloak.representations.idm.ClientRepresentation;
import static org.keycloak.testsuite.auth.page.login.OIDCLogin.OIDC;
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
        configure().clients();
    }

    public void createClient(ClientRepresentation client) {
        assertCurrentUrl(clients);
        clients.table().createClient();
        createClient.form().setValues(client);
        createClient.form().save();
    }

    public void deleteClientViaTable(String clientId) {
        assertCurrentUrl(clients);
        clients.deleteClient(clientId);
    }

    public void deleteClientViaPage(String clientId) {
        assertCurrentUrl(clients);
        clients.table().search(clientId);
        clients.table().clickClient(clientId);
        client.delete();
    }

    public static ClientRepresentation createClientRepresentation(String clientId, String... redirectUris) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        client.setEnabled(true);
        client.setConsentRequired(false);
        client.setDirectGrantsOnly(false);
        
        client.setProtocol(OIDC);
        
        client.setBearerOnly(false);
        client.setPublicClient(false);
        client.setServiceAccountsEnabled(false);
        
        List<String> redirectUrisList = new ArrayList();
        redirectUrisList.addAll(Arrays.asList(redirectUris));
        client.setRedirectUris(redirectUrisList);
        
        return client;
    }

}
