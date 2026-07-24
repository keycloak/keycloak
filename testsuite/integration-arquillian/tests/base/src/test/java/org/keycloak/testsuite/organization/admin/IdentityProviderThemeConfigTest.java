package org.keycloak.testsuite.organization.admin;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testsuite.AbstractAdminTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class IdentityProviderThemeConfigTest extends AbstractAdminTest {

    @Before
    public void onBefore() {
        RealmResource realm = managedRealm.admin();
        RealmRepresentation rep = realm.toRepresentation();
        rep.setLoginTheme("themeconfig");
        realm.update(rep);
    }

    @Test
    public void testIdentityProviderThemeConfigs() {
        managedRealm.admin().identityProviders().create(
                IdentityProviderBuilder.create()
                        .alias("broker")
                        .providerId("oidc")
                        .attribute("unsupported-themeConfig", "This value is not shown in the Keycloak theme")
                        .attribute("kcTheme-idpConfigValue", "This value is shown in the Keycloak theme")
                        .build()).close();

        oauth.realm(TEST_REALM_NAME);
        oauth.openLoginForm();
        String pageSource = driver.getPageSource();
        Assertions.assertTrue(pageSource.contains("This value is shown in the Keycloak theme"));
        Assertions.assertFalse(pageSource.contains("This value is not shown in the Keycloak theme"));
    }
}
