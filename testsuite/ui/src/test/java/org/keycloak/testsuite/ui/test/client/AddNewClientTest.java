package org.keycloak.testsuite.ui.test.client;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.junit.Test;
import org.keycloak.testsuite.ui.fragment.FlashMessage;
import org.keycloak.testsuite.ui.model.Client;
import org.keycloak.testsuite.ui.page.settings.ClientPage;


import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.keycloak.testsuite.ui.AbstractKeyCloakTest;

/**
 * Created by fkiss.
 */
public class AddNewClientTest extends AbstractKeyCloakTest<ClientPage> {

    @FindByJQuery(".alert")
    private FlashMessage flashMessage;
	
	@Before
	public void beforeClientTest() {
		navigation.clients();
		page.goToCreateClient();
	}

    @Test
    public void addNewClientTest() {
        Client newClient = new Client("testClient1", "http://example.com/*");
        page.addClient(newClient);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        navigation.clients();

        page.deleteClient(newClient.getName());
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        assertNull(page.findClient(newClient.getName()));
    }

    @Test
    public void addNewClientWithBlankNameTest() {
        Client newClient = new Client("", "http://example.com/*");
        page.addClient(newClient);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
    }

    @Test
    public void addNewClientWithBlankUriTest() {
        Client newClient = new Client("testClient2", "");
        page.addClient(newClient);
        page.confirmAddClient();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());

        page.addUri("http://testUri.com/*");
        page.confirmAddClient();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());

        navigation.clients();
        page.deleteClient(newClient.getName());
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        assertNull(page.findClient(newClient.getName()));
    }

    @Test
    public void addNewClientWithTwoUriTest() {
        Client newClient = new Client("testClient3", "");
        page.addClient(newClient);
        page.confirmAddClient();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());

        page.addUri("http://testUri.com/*");
        page.addUri("http://example.com/*");

        page.confirmAddClient();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());

        navigation.clients();
        page.deleteClient(newClient.getName());
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        assertNull(page.findClient(newClient.getName()));
    }

}
