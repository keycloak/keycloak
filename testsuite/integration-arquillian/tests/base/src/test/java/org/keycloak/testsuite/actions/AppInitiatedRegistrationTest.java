package org.keycloak.testsuite.actions;

import jakarta.ws.rs.core.UriBuilder;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.locale.LocaleSelectorProvider;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.RegisterPage;

public class AppInitiatedRegistrationTest extends AbstractTestRealmKeycloakTest {

    @Page
    protected AppPage appPage;

    @Page
    protected RegisterPage registerPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void before() {
        ApiUtil.removeUserByUsername(testRealm(), "test-user@localhost");
    }

    @Test
    public void ensureLocaleParameterIsPropagatedDuringAppInitiatedRegistration() {

        var appInitiatedRegisterUrlBuilder = UriBuilder.fromUri(oauth.getRegisterationsUrl());
        appInitiatedRegisterUrlBuilder.queryParam(LocaleSelectorProvider.KC_LOCALE_PARAM, "en");
        var appInitiatedRegisterUrl = appInitiatedRegisterUrlBuilder.build().toString();

        driver.navigate().to(appInitiatedRegisterUrl);

        registerPage.assertCurrent();
        registerPage.register("first", "last", "test-user@localhost", "test-user", "test","test");

        appPage.assertCurrent();

        UserRepresentation user = testRealm().users().searchByEmail("test-user@localhost", true).get(0);
        // ensure that the locale was set on the user
        Assert.assertEquals("en", user.getAttributes().get("locale").get(0));
    }
}
