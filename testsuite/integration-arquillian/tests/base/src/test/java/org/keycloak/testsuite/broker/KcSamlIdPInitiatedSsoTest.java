package org.keycloak.testsuite.broker;

import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.common.util.StringPropertyReplacer;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.UpdateAccountInformationPage;
import org.keycloak.testsuite.util.IOUtil;

import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.keycloak.testsuite.broker.BrokerTestConstants.*;
import static org.junit.Assert.assertThat;

/**
 *
 * @author hmlnarik
 */
public class KcSamlIdPInitiatedSsoTest extends AbstractKeycloakTest {

    private static final String PROVIDER_REALM_USER_NAME = "test";
    private static final String PROVIDER_REALM_USER_PASSWORD = "test";

    private static final String CONSUMER_CHOSEN_USERNAME = "mytest";

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

    @Before
    public void cleanupTestUserInConsumerRealm() {
        final UsersResource users = adminClient.realm(REALM_CONS_NAME).users();
        users.search(CONSUMER_CHOSEN_USERNAME).stream()
          .map(UserRepresentation::getId)
          .map(users::delete)
          .forEach(Response::close);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        Properties p = new Properties();
        p.put("name.realm.provider", REALM_PROV_NAME);
        p.put("name.realm.consumer", REALM_CONS_NAME);
        p.put("url.realm.provider", getAuthRoot() + "/auth/realms/" + REALM_PROV_NAME);
        p.put("url.realm.consumer", getAuthRoot() + "/auth/realms/" + REALM_CONS_NAME);
        p.put("url.realm.consumer-2", getAuthRoot() + "/auth/realms/" + REALM_CONS_NAME + "-2");
        
        testRealms.add(loadFromClasspath("kc3731-provider-realm.json", p));
        testRealms.add(loadFromClasspath("kc3731-broker-realm.json", p));
    }

    @Test
    public void testProviderIdpInitiatedLogin() throws Exception {
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
        updateAccountInformationPage.updateAccountInformation(CONSUMER_CHOSEN_USERNAME, "test@localhost", "Firstname", "Lastname");

        UsersResource consumerUsers = adminClient.realm(REALM_CONS_NAME).users();

        int userCount = consumerUsers.count();
        Assert.assertTrue("There must be at least one user", userCount > 0);

        List<UserRepresentation> users = consumerUsers.search("", 0, userCount);

        boolean isUserFound = users.stream().anyMatch(user -> user.getUsername().equals(CONSUMER_CHOSEN_USERNAME) && user.getEmail().equals("test@localhost"));
        Assert.assertTrue("There must be user " + CONSUMER_CHOSEN_USERNAME + " in realm " + REALM_CONS_NAME, isUserFound);

        Assert.assertThat(driver.findElement(By.tagName("a")).getAttribute("id"), containsString("account"));
    }

    private String getSamlIdpInitiatedUrl(String realmName, String samlIdpInitiatedSsoUrlName) {
        return getAuthRoot() + "/auth/realms/" + realmName + "/protocol/saml/clients/" + samlIdpInitiatedSsoUrlName;
    }

    private String getSamlBrokerIdpInitiatedUrl(String realmName, String samlIdpInitiatedSsoUrlName) {
        return getAuthRoot() + "/auth/realms/" + realmName + "/broker/saml-leaf/endpoint/clients/" + samlIdpInitiatedSsoUrlName;
    }

    private String getSamlBrokerUrl(String realmName) {
        return getAuthRoot() + "/auth/realms/" + realmName + "/broker/saml-leaf/endpoint";
    }

    private void waitForPage(final String title) {
        WebDriverWait wait = new WebDriverWait(driver, 5);

        ExpectedCondition<Boolean> condition = (WebDriver input) -> input.getTitle().toLowerCase().contains(title);

        wait.until(condition);
    }

    @Test
    public void testProviderIdpInitiatedLoginToApp() {
        SAMLDocumentHolder samlResponse = new SamlClientBuilder()
          .navigateTo(getSamlIdpInitiatedUrl(REALM_PROV_NAME, "samlbroker"))
          // Login in provider realm
          .login().user(PROVIDER_REALM_USER_NAME, PROVIDER_REALM_USER_PASSWORD).build()

          // Send the response to the consumer realm
          .processSamlResponse(Binding.POST)
          .transformObject(ob -> {
              assertThat(ob, Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
              ResponseType resp = (ResponseType) ob;
              assertThat(resp.getDestination(), is(getSamlBrokerIdpInitiatedUrl(REALM_CONS_NAME, "sales")));
              return ob;
          })
          .build()

          .updateProfile().username(CONSUMER_CHOSEN_USERNAME).email("test@localhost").firstName("Firstname").lastName("Lastname").build()
          .followOneRedirect()

          // Obtain the response sent to the app
          .getSamlResponse(Binding.POST);

        assertThat(samlResponse.getSamlObject(), Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        ResponseType resp = (ResponseType) samlResponse.getSamlObject();
        assertThat(resp.getDestination(), is("http://localhost:8180/auth/realms/" + REALM_CONS_NAME + "/app/auth"));
    }

    @Test
    public void testConsumerIdpInitiatedLoginToApp() {
        SAMLDocumentHolder samlResponse = new SamlClientBuilder()
          .navigateTo(getSamlIdpInitiatedUrl(REALM_CONS_NAME, "sales"))
          // Request login via saml-leaf
          .login().idp("saml-leaf").build()

          .processSamlResponse(Binding.POST)    // AuthnRequest to producer IdP
            .targetAttributeSamlRequest()
            .build()

          // Login in provider realm
          .login().user(PROVIDER_REALM_USER_NAME, PROVIDER_REALM_USER_PASSWORD).build()

          // Send the response to the consumer realm
          .processSamlResponse(Binding.POST)
          .transformObject(ob -> {
              assertThat(ob, Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
              ResponseType resp = (ResponseType) ob;
              assertThat(resp.getDestination(), is(getSamlBrokerUrl(REALM_CONS_NAME)));
              return ob;
          })
          .build()

          .updateProfile().username(CONSUMER_CHOSEN_USERNAME).email("test@localhost").firstName("Firstname").lastName("Lastname").build()
          .followOneRedirect()

          // Obtain the response sent to the app
          .getSamlResponse(Binding.POST);

        assertThat(samlResponse.getSamlObject(), Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        ResponseType resp = (ResponseType) samlResponse.getSamlObject();
        assertThat(resp.getDestination(), is("http://localhost:8180/auth/realms/" + REALM_CONS_NAME + "/app/auth"));
    }

    @Test
    public void testTwoConsequentIdpInitiatedLogins() {
        SAMLDocumentHolder samlResponse = new SamlClientBuilder()
          .navigateTo(getSamlIdpInitiatedUrl(REALM_PROV_NAME, "samlbroker"))
          // Login in provider realm
          .login().user(PROVIDER_REALM_USER_NAME, PROVIDER_REALM_USER_PASSWORD).build()

          // Send the response to the consumer realm
          .processSamlResponse(Binding.POST)
            .transformObject(ob -> {
              assertThat(ob, Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
              ResponseType resp = (ResponseType) ob;
              assertThat(resp.getDestination(), is(getSamlBrokerIdpInitiatedUrl(REALM_CONS_NAME, "sales")));
              return ob;
            })
            .build()

          .updateProfile().username(CONSUMER_CHOSEN_USERNAME).email("test@localhost").firstName("Firstname").lastName("Lastname").build()
          .followOneRedirect()

          // Obtain the response sent to the app and ignore result
          .processSamlResponse(Binding.POST)
            .transformObject(ob -> {
              assertThat(ob, Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
              ResponseType resp = (ResponseType) ob;
              assertThat(resp.getDestination(), is("http://localhost:8180/auth/realms/" + REALM_CONS_NAME + "/app/auth"));
              return null;
            })
            .build()


          // Now login to the second app
          .navigateTo(getSamlIdpInitiatedUrl(REALM_PROV_NAME, "samlbroker-2"))

          // Login in provider realm
          .login().sso(true).build()

          // Send the response to the consumer realm
          .processSamlResponse(Binding.POST)
            .transformObject(ob -> {
              assertThat(ob, Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
              ResponseType resp = (ResponseType) ob;
              assertThat(resp.getDestination(), is(getSamlBrokerIdpInitiatedUrl(REALM_CONS_NAME, "sales2")));
              return ob;
            })
            .build()

          .getSamlResponse(Binding.POST);

        assertThat(samlResponse.getSamlObject(), Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
        ResponseType resp = (ResponseType) samlResponse.getSamlObject();
        assertThat(resp.getDestination(), is("http://localhost:8180/auth/realms/" + REALM_CONS_NAME + "/app/auth/sales2/saml"));

        assertSingleUserSession(REALM_CONS_NAME, CONSUMER_CHOSEN_USERNAME,
          "http://localhost:8180/auth/realms/" + REALM_CONS_NAME + "/app/auth",
          "http://localhost:8180/auth/realms/" + REALM_CONS_NAME + "/app/auth2"
        );

        assertSingleUserSession(REALM_PROV_NAME, PROVIDER_REALM_USER_NAME,
          getAuthRoot() + "/auth/realms/" + REALM_CONS_NAME,
          getAuthRoot() + "/auth/realms/" + REALM_CONS_NAME + "-2"
        );
    }

    private void assertSingleUserSession(String realmName, String userName, String... expectedClientIds) {
        final UsersResource users = adminClient.realm(realmName).users();
        final ClientsResource clients = adminClient.realm(realmName).clients();

        UserRepresentation userRepresentation = users
          .search(userName).stream()
          .findFirst().get();

        List<UserSessionRepresentation> userSessions = users.get(userRepresentation.getId()).getUserSessions();
        assertThat(userSessions, hasSize(1));
        Map<String, String> clientSessions = userSessions.get(0).getClients();

        Set<String> clientIds = clientSessions.values().stream()
          .flatMap(c -> clients.findByClientId(c).stream())
          .map(ClientRepresentation::getClientId)
          .collect(Collectors.toSet());

        assertThat(clientIds, containsInAnyOrder(expectedClientIds));
    }
}
