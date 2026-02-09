package org.keycloak.testsuite.actions;

import org.keycloak.locale.LocaleSelectorProvider;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.RegisterPage;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;

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

        oauth.registrationForm()
                .param(LocaleSelectorProvider.KC_LOCALE_PARAM, "en")
                .open();

        registerPage.assertCurrent();
        registerPage.register("first", "last", "test-user@localhost", "test-user", "test","test");

        appPage.assertCurrent();

        UserRepresentation user = testRealm().users().searchByEmail("test-user@localhost", true).get(0);
        // ensure that the locale was set on the user
        Assert.assertEquals("en", user.getAttributes().get("locale").get(0));
    }
}
