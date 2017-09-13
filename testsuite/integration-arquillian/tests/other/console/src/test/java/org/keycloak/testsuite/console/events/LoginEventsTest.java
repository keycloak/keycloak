package org.keycloak.testsuite.console.events;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.admin.Users;
import org.keycloak.testsuite.console.AbstractConsoleTest;
import org.keycloak.testsuite.console.page.events.Config;
import org.keycloak.testsuite.console.page.events.LoginEvents;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;

/**
 * @author mhajas
 */
public class LoginEventsTest extends AbstractConsoleTest {
    @Page
    private LoginEvents loginEventsPage;

    @Page
    private Config configPage;

    @Before
    public void beforeLoginEventsTest() {
        RealmRepresentation realm = testRealmResource().toRepresentation();

        realm.setEventsEnabled(true);
        realm.setEnabledEventTypes(Arrays.asList("LOGIN", "LOGIN_ERROR", "LOGOUT"));

        testRealmResource().update(realm);
    }

    @Test
    public void userAccessEventsTest() {
        deleteAllCookiesForTestRealm();
        testRealmAdminConsolePage.navigateTo();
        Users.setPasswordFor(testUser, "Wrong_password");
        testRealmLoginPage.form().login(testUser);
        Users.setPasswordFor(testUser, PASSWORD);
        testRealmLoginPage.form().login(testUser);
        testRealmAdminConsolePage.logOut();

        loginEventsPage.navigateTo();
        loginEventsPage.table().filter();

        List<WebElement> resultList = loginEventsPage.table().rows();
        assertEquals(3, resultList.size());

        loginEventsPage.table().filterForm().addEventType("LOGIN");
        loginEventsPage.table().update();

        resultList = loginEventsPage.table().rows();

        assertEquals(1, resultList.size());
        resultList.get(0).findElement(By.xpath("//td[text()='LOGIN']"));
        resultList.get(0).findElement(By.xpath("//td[text()='User']/../td[text()='" + testUser.getId() + "']"));
        resultList.get(0).findElement(By.xpath("//td[text()='Client']/../td[text()='security-admin-console']"));
        resultList.get(0).findElement(By.xpath("//td[text()='IP Address']/../td[text()='127.0.0.1' or text()='0:0:0:0:0:0:0:1']"));

        loginEventsPage.table().reset();
        loginEventsPage.table().filterForm().addEventType("LOGOUT");
        loginEventsPage.table().update();

        resultList = loginEventsPage.table().rows();

        assertEquals(1, resultList.size());
        resultList.get(0).findElement(By.xpath("//td[text()='LOGOUT']"));
        resultList.get(0).findElement(By.xpath("//td[text()='User']/../td[text()='" + testUser.getId() + "']"));
        resultList.get(0).findElement(By.xpath("//td[text()='IP Address']/../td[text()='127.0.0.1' or text()='0:0:0:0:0:0:0:1']"));

        loginEventsPage.table().reset();
        loginEventsPage.table().filterForm().addEventType("LOGIN_ERROR");
        loginEventsPage.table().update();

        resultList = loginEventsPage.table().rows();

        assertEquals(1, resultList.size());
        resultList.get(0).findElement(By.xpath("//td[text()='LOGIN_ERROR']"));
        resultList.get(0).findElement(By.xpath("//td[text()='User']/../td[text()='" + testUser.getId() + "']"));
        resultList.get(0).findElement(By.xpath("//td[text()='Client']/../td[text()='security-admin-console']"));
        resultList.get(0).findElement(By.xpath("//td[text()='Error']/../td[text()='invalid_user_credentials']"));
        resultList.get(0).findElement(By.xpath("//td[text()='IP Address']/../td[text()='127.0.0.1' or text()='0:0:0:0:0:0:0:1']"));


    }
}
