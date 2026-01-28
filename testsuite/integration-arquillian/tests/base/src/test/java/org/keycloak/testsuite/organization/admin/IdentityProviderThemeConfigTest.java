package org.keycloak.testsuite.organization.admin;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractAdminTest;
import org.keycloak.testsuite.util.IdentityProviderBuilder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class IdentityProviderThemeConfigTest extends AbstractAdminTest {

    @Before
    public void onBefore() {
        RealmResource realm = testRealm();
        RealmRepresentation rep = realm.toRepresentation();
        rep.setLoginTheme("themeconfig");
        realm.update(rep);
    }

    @Test
    public void testIdentityProviderThemeConfigs() {
        testRealm().identityProviders().create(
                IdentityProviderBuilder.create()
                        .alias("broker")
                        .providerId("oidc")
                        .setAttribute("unsupported-themeConfig", "This value is not shown in the Keycloak theme")
                        .setAttribute("kcTheme-idpConfigValue", "This value is shown in the Keycloak theme")
                        .build()).close();

        oauth.realm(TEST_REALM_NAME);
        oauth.openLoginForm();
        String pageSource = driver.getPageSource();
        Assert.assertTrue(pageSource.contains("This value is shown in the Keycloak theme"));
        Assert.assertFalse(pageSource.contains("This value is not shown in the Keycloak theme"));
    }
}
