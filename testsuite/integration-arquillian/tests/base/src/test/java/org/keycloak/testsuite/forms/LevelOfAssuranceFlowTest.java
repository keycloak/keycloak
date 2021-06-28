package org.keycloak.testsuite.forms;

import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.authentication.authenticators.browser.PasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.SetLoaAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernameFormFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticatorFactory;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.authentication.PushButtonAuthenticatorFactory;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginUsernameOnlyPage;
import org.keycloak.testsuite.pages.PasswordPage;
import org.keycloak.testsuite.util.FlowUtil;
import org.openqa.selenium.By;

/**
 * Tests for Level Of Assurance conditions in authentication flow.
 * 
 * @author <a href="mailto:sebastian.zoescher@prime-sign.com">Sebastian Zoescher</a>
 */
@EnableFeature(value = Profile.Feature.WEB_AUTHN, skipRestart = true, onlyForProduct = true)
public class LevelOfAssuranceFlowTest extends AbstractTestRealmKeycloakTest {

    static final String CONFIG_CONDITION_LEVEL = "loa-condition-level";

    static final String CONFIG_SET_LEVEL = "loa-level";
    static final String CONFIG_STORE_IN_USER_SESSION = "loa-store-in-user-session";
    
    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected LoginUsernameOnlyPage loginUsernameOnlyPage;

    @Page
    protected PasswordPage passwordPage;

    @Page
    protected ErrorPage errorPage;
    
    

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }

    private RealmRepresentation loadTestRealm() {
        RealmRepresentation res = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        res.setBrowserFlow("browser");
        return res;
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
  
        log.debug("Adding test realm for import from testrealm.json");
        testRealms.add(loadTestRealm());
    }

    @Before
    public void setupFlow() {
        
        final String newFlowAlias = "browser -  Level of Authebtication FLow";
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server("test")
            .run(session -> FlowUtil.inCurrentRealm(session).selectFlow(newFlowAlias).inForms(forms -> forms.clear()
                // level 1 authentication
                .addSubFlowExecution(Requirement.CONDITIONAL, subFlow -> {
                    subFlow.addAuthenticatorExecution(Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                        config -> {
                            config.getConfig().put(CONFIG_CONDITION_LEVEL, "1");
                        });

                    // username input for level 2
                    subFlow.addAuthenticatorExecution(Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID);

                    // set current LOA value to 1
                    subFlow.addAuthenticatorExecution(Requirement.REQUIRED, SetLoaAuthenticatorFactory.PROVIDER_ID, config -> {
                        config.getConfig().put(CONFIG_SET_LEVEL, "1");
                        config.getConfig().put(CONFIG_STORE_IN_USER_SESSION, "true");
                    });

                })

                // level 2 authentication
                .addSubFlowExecution(Requirement.CONDITIONAL, subFlow -> {
                    subFlow.addAuthenticatorExecution(Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                        config -> {
                            config.getConfig().put(CONFIG_CONDITION_LEVEL, "2");
                        });

                    // password required for level 2
                    subFlow.addAuthenticatorExecution(Requirement.REQUIRED, PasswordFormFactory.PROVIDER_ID);

                    // set current LOA value to 2
                    subFlow.addAuthenticatorExecution(Requirement.REQUIRED, SetLoaAuthenticatorFactory.PROVIDER_ID, config -> {
                        config.getConfig().put(CONFIG_SET_LEVEL, "2");
                        config.getConfig().put(CONFIG_STORE_IN_USER_SESSION, "true");
                    });

                })

                // level 3 authentication
                .addSubFlowExecution(Requirement.CONDITIONAL, subFlow -> {
                    subFlow.addAuthenticatorExecution(Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                        config -> {
                            config.getConfig().put(CONFIG_CONDITION_LEVEL, "3");
                        });

                    // simply push button for level 3
                    subFlow.addAuthenticatorExecution(Requirement.REQUIRED, PushButtonAuthenticatorFactory.PROVIDER_ID);

                    // set current LOA value to 3
                    subFlow.addAuthenticatorExecution(Requirement.REQUIRED, SetLoaAuthenticatorFactory.PROVIDER_ID, config -> {
                        config.getConfig().put(CONFIG_SET_LEVEL, "3");
                        config.getConfig().put(CONFIG_STORE_IN_USER_SESSION, "true");
                    });

                })

            ).defineAsBrowserFlow());
    }

    @Test
    public void testNoLevelOfAuthentication() throws Exception {
        driver.navigate().to(oauth.getLoginFormUrl());
        // Authentication without specific LOA results in level 1 authentication
        assertLevel1();
        events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
    }

    @Test
    public void testLevelOfAuthentication1() throws Exception {

        // username input for level 1
        driver.navigate().to(loginFormWithClaim(Collections.singletonList("1")));
        assertLevel1();
        events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();

    }

    @Test
    public void testLevelOfAuthentication2() throws Exception {

        // username and password input for level 2
        driver.navigate().to(loginFormWithClaim(Collections.singletonList("2")));
        assertLevel1();
        assertLevel2();
        events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
    }

    @Test
    public void testLevelOfAuthentication3() throws Exception {

        // username, password input and finally push button for level 3
        driver.navigate().to(loginFormWithClaim(Collections.singletonList("3")));
        assertLevel1();
        assertLevel2();
        assertLevel3();
        events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
    }

    private String loginFormWithClaim(List<String> values) throws Exception {

        String loginFormUrl = oauth.getLoginFormUrl();
        return UriBuilder.fromUri(loginFormUrl).queryParam("claims", urlEncodedClaim(values)).build().toString();
    }

    private String urlEncodedClaim(List<String> values) throws Exception {

        StringBuilder builder = new StringBuilder();
        builder.append("{\"id_token\":{\"acr\":{\"essential\":true,\"values\":[");

        Iterator<String> valueIterator = values.iterator();
        while (valueIterator.hasNext()) {
            builder.append("\"");
            builder.append(valueIterator.next());
            builder.append("\"");
            if (valueIterator.hasNext()) {
                builder.append(".");
            }
        }
        builder.append("]}}}");

        return URLEncoder.encode(builder.toString(), StandardCharsets.UTF_8.toString());
    }


    private void assertLevel1() {
        loginUsernameOnlyPage.assertCurrent();
        loginUsernameOnlyPage.login("test-user@localhost");
    }

    private void assertLevel2() {
        passwordPage.assertCurrent();
        passwordPage.login("password");
    }

    private void assertLevel3() {
        Assert.assertEquals("PushTheButton", driver.getTitle());
        driver.findElement(By.name("submit1")).click();
    }
    
    // TODO: Add tests for custom mapping values (f.i: bronze, silver, gold) and invalid (not-mapped) values;

}
