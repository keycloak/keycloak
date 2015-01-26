package org.keycloak.testsuite.ui.test.application;

import org.jboss.arquillian.graphene.findby.FindByJQuery;
import org.junit.Test;
import org.keycloak.testsuite.ui.fragment.FlashMessage;
import org.keycloak.testsuite.ui.model.Application;
import org.keycloak.testsuite.ui.page.settings.ApplicationPage;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.keycloak.testsuite.ui.AbstractKeyCloakTest;

/**
 * Created by fkiss.
 */
public class AddNewApplicationTest extends AbstractKeyCloakTest<ApplicationPage> {

    @FindByJQuery(".alert")
    private FlashMessage flashMessage;

    @Test
    public void addNewAppTest() {
        driver.manage().timeouts().setScriptTimeout(10, TimeUnit.SECONDS);
        navigation.applications();
        Application newApp = new Application("testApp1", "http://example.com/*");
        page.addApplication(newApp);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        navigation.applications();

        page.deleteApplication(newApp.getName());
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        assertNull(page.findApplication(newApp.getName()));
    }

    @Test
    public void addNewAppWithBlankNameTest() {
        navigation.applications();
        Application newApp = new Application("", "http://example.com/*");
        page.addApplication(newApp);
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());
        navigation.applications();
    }

    @Test
    public void addNewAppWithBlankUriTest() {
        navigation.applications();
        Application newApp = new Application("testApp2", "");
        page.addApplicationWithoutUri(newApp);
        page.confirmAddApplication();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());

        page.addUri("http://testUri.com/*");
        page.confirmAddApplication();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());

        navigation.applications();
        page.deleteApplication(newApp.getName());
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        assertNull(page.findApplication(newApp.getName()));
    }

    @Test
    public void addNewAppWithTwoUriTest() {
        navigation.applications();
        Application newApp = new Application("testApp3", "");
        page.addApplicationWithoutUri(newApp);
        page.confirmAddApplication();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isDanger());

        page.addUri("http://testUri.com/*");
        page.addUri("http://example.com/*");

        page.confirmAddApplication();
        flashMessage.waitUntilPresent();
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());

        navigation.applications();
        page.deleteApplication(newApp.getName());
        assertTrue(flashMessage.getText(), flashMessage.isSuccess());
        assertNull(page.findApplication(newApp.getName()));
    }

}
