package org.keycloak.testsuite.console.clients;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import static org.keycloak.testsuite.auth.page.login.OIDCLogin.OIDC;
import static org.keycloak.testsuite.auth.page.login.OIDCLogin.SAML;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.clients.Client;
import org.keycloak.testsuite.console.page.clients.Clients;
import org.keycloak.testsuite.console.page.clients.CreateClient;
import static org.keycloak.testsuite.util.AttributesAssert.assertEqualsBooleanAttributes;
import static org.keycloak.testsuite.util.AttributesAssert.assertEqualsListAttributes;
import static org.keycloak.testsuite.util.AttributesAssert.assertEqualsStringAttributes;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractClientTest extends AbstractConsoleTest {

    public final String TEST_CLIENT_ID = "test-client";
    public final String TEST_REDIRECT_URIS = "http://example.test/app/*";
    
    @Page
    protected Clients clientsPage;
    @Page
    protected Client clientPage; // note: cannot call navigateTo() unless client id is set
    @Page
    protected CreateClient createClientPage;

    @Before
    public void beforeClientTest() {
//        configure().clients();
        clientsPage.navigateTo();
    }

    public void createClient(ClientRepresentation client) {
        assertCurrentUrlEquals(clientsPage);
        clientsPage.table().createClient();
        createClientPage.form().setValues(client);
        if (SAML.equals(client.getProtocol())) {
            createClientPage.form().samlForm().setValues(client);
        }
        createClientPage.form().save();
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
        
        //set expected web origins to newClient
        List<String> webOrigins = new ArrayList<>();
        for (String redirectUri : redirectUris) {
            //parse webOrigin from redirectUri: take substring from index 0 to 
            //first occurence of "/", excluded "http://" by starting search on index 7
            webOrigins.add(redirectUri.substring(0, redirectUri.indexOf("/", 7)));
        }
        client.setWebOrigins(webOrigins);
        return client;
    }
    
    public ClientRepresentation findClientRepByClientId(String clientId) {
        ClientRepresentation found = null;
        for (ClientRepresentation clientRepresentation : testRealmResource().clients().findAll()) {
            if (clientRepresentation.getClientId().equals(clientId)) {
                found = clientRepresentation;
                break;
            }
        }
        return found;
    }
    
    public void assertClientSettingsEqual(ClientRepresentation c1, ClientRepresentation c2) {
        assertEqualsStringAttributes(c1.getClientId(), c2.getClientId());
        assertEqualsStringAttributes(c1.getName(), c2.getName());
        assertEqualsBooleanAttributes(c1.isEnabled(), c2.isEnabled());
        assertEqualsBooleanAttributes(c1.isConsentRequired(), c2.isConsentRequired());
        assertEqualsBooleanAttributes(c1.isDirectGrantsOnly(), c2.isDirectGrantsOnly());
        assertEqualsStringAttributes(c1.getProtocol(), c2.getProtocol());

        assertEqualsBooleanAttributes(c1.isBearerOnly(), c2.isBearerOnly());
        assertEqualsBooleanAttributes(c1.isPublicClient(), c2.isPublicClient());
        assertEqualsBooleanAttributes(c1.isSurrogateAuthRequired(), c2.isSurrogateAuthRequired());

        assertEqualsBooleanAttributes(c1.isFrontchannelLogout(), c2.isFrontchannelLogout());

        assertEqualsBooleanAttributes(c1.isServiceAccountsEnabled(), c2.isServiceAccountsEnabled());
        assertEqualsListAttributes(c1.getRedirectUris(), c2.getRedirectUris());
        assertEqualsStringAttributes(c1.getBaseUrl(), c2.getBaseUrl());
        assertEqualsStringAttributes(c1.getAdminUrl(), c2.getAdminUrl());
        assertEqualsListAttributes(c1.getWebOrigins(), c2.getWebOrigins());
    }

}
