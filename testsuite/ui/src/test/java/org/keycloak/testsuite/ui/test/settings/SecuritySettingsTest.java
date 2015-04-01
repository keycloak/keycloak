package org.keycloak.testsuite.ui.test.settings;

import org.junit.Test;
import org.keycloak.testsuite.ui.AbstractKeyCloakTest;
import org.keycloak.testsuite.ui.page.settings.SecurityPage;

/**
 * Created by fkiss.
 */
public class SecuritySettingsTest extends AbstractKeyCloakTest<SecurityPage>{

    @Test
    public void securitySettingsTest() {
        navigation.security();
        page.goToAndEnableBruteForceProtectionTab();
        //TODO:

    }
}
