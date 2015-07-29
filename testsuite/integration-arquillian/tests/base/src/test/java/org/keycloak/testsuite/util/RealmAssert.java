package org.keycloak.testsuite.util;

import org.keycloak.testsuite.page.auth.AuthRealm;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrlStartsWith;
import org.openqa.selenium.WebDriver;

/**
 *
 * @author tkyjovsk
 */
public class RealmAssert {

    public static void assertCurrentUrlStartsWithLoginUrlOf(AuthRealm realm) {
        assertCurrentUrlStartsWithLoginUrlOf(realm.getDriver(), realm);
    }
    
    public static void assertCurrentUrlStartsWithLoginUrlOf(WebDriver driver, AuthRealm realm) {
        assertCurrentUrlStartsWith(driver, realm.getOIDCLoginUrl().toString());
    }
    
}
