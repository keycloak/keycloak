/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.keycloak.testsuite.broker;

import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.common.util.StringPropertyReplacer;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.adapter.AbstractServletsAdapterTest;
import org.keycloak.testsuite.adapter.page.SalesPostServlet;
import org.keycloak.testsuite.adapter.servlet.SendUsernameServlet;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.UpdateAccountInformationPage;
import org.keycloak.testsuite.util.IOUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.keycloak.testsuite.broker.BrokerTestConstants.*;
import static org.hamcrest.Matchers.*;

/**
 *
 * @author hmlnarik
 */
public class KcSamlIdPInitiatedSsoTest extends AbstractKeycloakTest {

    private static final String PROVIDER_REALM_USER_NAME = "test";
    private static final String PROVIDER_REALM_USER_PASSWORD = "test";

    @Page
    protected LoginPage accountLoginPage;

    @Page
    protected UpdateAccountInformationPage updateAccountInformationPage;

    protected String getAuthRoot() {
        return suiteContext.getAuthServerInfo().getContextRoot().toString();
    }

    private RealmRepresentation loadFromClasspath(String fileName, Properties properties) {
        InputStream is = KcSamlIdPInitiatedSsoTest.class.getResourceAsStream(fileName);
        try {
            String template = StreamUtil.readString(is);
            String realmString = StringPropertyReplacer.replaceProperties(template, properties);
            return IOUtil.loadRealm(new ByteArrayInputStream(realmString.getBytes("UTF-8")));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        Properties p = new Properties();
        p.put("name.realm.provider", REALM_PROV_NAME);
        p.put("name.realm.consumer", REALM_CONS_NAME);
        p.put("url.realm.provider", getAuthRoot() + "/auth/realms/" + REALM_PROV_NAME);
        p.put("url.realm.consumer", getAuthRoot() + "/auth/realms/" + REALM_CONS_NAME);
        
        testRealms.add(loadFromClasspath("kc3731-provider-realm.json", p));
        testRealms.add(loadFromClasspath("kc3731-broker-realm.json", p));
    }

    @Test
    public void testProviderIdpInitiatedLogin() {
        driver.navigate().to(getSamlIdpInitiatedUrl(REALM_PROV_NAME, "samlbroker"));

        waitForPage("log in to");

        Assert.assertThat("Driver should be on the provider realm page right now",
                driver.getCurrentUrl(), containsString("/auth/realms/" + REALM_PROV_NAME + "/"));

        log.debug("Logging in");
        accountLoginPage.login(PROVIDER_REALM_USER_NAME, PROVIDER_REALM_USER_PASSWORD);

        waitForPage("update account information");

        Assert.assertTrue(updateAccountInformationPage.isCurrent());
        Assert.assertThat("We must be on consumer realm right now",
                driver.getCurrentUrl(), containsString("/auth/realms/" + REALM_CONS_NAME + "/"));

        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation("mytest", "test@localhost", "Firstname", "Lastname");

        UsersResource consumerUsers = adminClient.realm(REALM_CONS_NAME).users();

        int userCount = consumerUsers.count();
        Assert.assertTrue("There must be at least one user", userCount > 0);

        List<UserRepresentation> users = consumerUsers.search("", 0, userCount);

        boolean isUserFound = users.stream().anyMatch(user -> user.getUsername().equals("mytest") && user.getEmail().equals("test@localhost"));
        Assert.assertTrue("There must be user " + "mytest" + " in realm " + REALM_CONS_NAME, isUserFound);

        Assert.assertThat(driver.findElement(org.openqa.selenium.By.tagName("form")).getAttribute("action"), containsString("http://localhost:18080/sales-post-enc/"));
    }

    private String getSamlIdpInitiatedUrl(String realmName, String samlIdpInitiatedSsoUrlName) {
        return getAuthRoot() + "/auth/realms/" + realmName + "/protocol/saml/clients/" + samlIdpInitiatedSsoUrlName;
    }

    private void waitForPage(final String title) {
        WebDriverWait wait = new WebDriverWait(driver, 5);

        ExpectedCondition<Boolean> condition = (WebDriver input) -> input.getTitle().toLowerCase().contains(title);

        wait.until(condition);
    }

}
