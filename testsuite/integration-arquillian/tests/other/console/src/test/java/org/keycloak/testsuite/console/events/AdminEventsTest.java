package org.keycloak.testsuite.console.events;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.clients.AbstractClientTest;
import org.keycloak.testsuite.console.page.clients.Clients;
import org.keycloak.testsuite.console.page.events.AdminEvents;
import org.keycloak.testsuite.console.page.events.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import javax.ws.rs.core.Response;
import java.util.List;

import static org.junit.Assert.assertEquals;
import org.keycloak.admin.client.resource.ClientsResource;
import static org.keycloak.testsuite.console.page.clients.CreateClientForm.OidcAccessType.CONFIDENTIAL;


/**
 * @author mhajas
 */
public class AdminEventsTest extends AbstractConsoleTest {

    @Page
    private AdminEvents adminEventsPage;

    @Page
    private Config configPage;

    @Page
    private Clients clientsPage;

    private ClientRepresentation newClient;

    @Before
    public void beforeAdminEventsTest() {
        RealmRepresentation realm = testRealmResource().toRepresentation();

        realm.setAdminEventsEnabled(true);
        realm.setAdminEventsDetailsEnabled(true);

        testRealmResource().update(realm);
    }

    @Test
    public void clientsAdminEventsTest() {
        newClient = AbstractClientTest.createOidcClientRep(CONFIDENTIAL, "test_client", "http://example.test/test_client/*");
        Response response = clientsResource().create(newClient);
        String id = ApiUtil.getCreatedId(response);
        response.close();
        newClient.setClientId("test_client2");
        clientsResource().get(id).update(newClient);
        clientsResource().get(id).remove();

        adminEventsPage.navigateTo();
        adminEventsPage.table().filter();
        adminEventsPage.table().filterForm().addOperationType("CREATE");
        adminEventsPage.table().update();

        List<WebElement> resultList = adminEventsPage.table().rows();
        assertEquals(1, resultList.size());
        resultList.get(0).findElement(By.xpath("//td[text()='CREATE']"));
        resultList.get(0).findElement(By.xpath("//td[text()='clients/" + id + "']"));

        adminEventsPage.table().reset();
        adminEventsPage.table().filterForm().addOperationType("UPDATE");
        adminEventsPage.table().update();

        resultList = adminEventsPage.table().rows();
        assertEquals(1, resultList.size());
        resultList.get(0).findElement(By.xpath("//td[text()='UPDATE']"));
        resultList.get(0).findElement(By.xpath("//td[text()='clients/" + id + "']"));

        adminEventsPage.table().reset();
        adminEventsPage.table().filterForm().addOperationType("DELETE");
        adminEventsPage.table().update();

        resultList = adminEventsPage.table().rows();
        assertEquals(1, resultList.size());
        resultList.get(0).findElement(By.xpath("//td[text()='DELETE']"));
        resultList.get(0).findElement(By.xpath("//td[text()='clients/" + id + "']"));
    }
    
    public ClientsResource clientsResource() {
        return testRealmResource().clients();
    }
}
