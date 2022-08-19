/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.console.events;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.clients.Clients;
import org.keycloak.testsuite.console.page.events.AdminEvents;
import org.keycloak.testsuite.console.page.events.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import javax.ws.rs.core.Response;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.auth.page.login.Login.OIDC;
import static org.keycloak.testsuite.console.clients.AbstractClientTest.createClientRep;


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
        testRealmResource().clearAdminEvents();
    }

    @Test
    public void clientsAdminEventsTest() {
        newClient = createClientRep("test_client", OIDC);
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
        resultList.get(0).findElement(By.xpath("//td[text()='CLIENT']"));

        adminEventsPage.table().reset();
        adminEventsPage.table().filterForm().addOperationType("UPDATE");
        adminEventsPage.table().update();

        resultList = adminEventsPage.table().rows();
        assertEquals(1, resultList.size());
        resultList.get(0).findElement(By.xpath("//td[text()='UPDATE']"));
        resultList.get(0).findElement(By.xpath("//td[text()='clients/" + id + "']"));
        resultList.get(0).findElement(By.xpath("//td[text()='CLIENT']"));

        adminEventsPage.table().reset();
        adminEventsPage.table().filterForm().addOperationType("DELETE");
        adminEventsPage.table().update();

        resultList = adminEventsPage.table().rows();
        assertEquals(1, resultList.size());
        resultList.get(0).findElement(By.xpath("//td[text()='DELETE']"));
        resultList.get(0).findElement(By.xpath("//td[text()='clients/" + id + "']"));
        resultList.get(0).findElement(By.xpath("//td[text()='CLIENT']"));
    }
    
    public ClientsResource clientsResource() {
        return testRealmResource().clients();
    }
}