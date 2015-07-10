package org.keycloak.testsuite.util;

import org.keycloak.testsuite.page.console.Realm;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrlStartsWith;
import org.openqa.selenium.WebDriver;

/**
 *
 * @author tkyjovsk
 */
public class RealmAssert {

    public static void assertCurrentUrlStartsWithLoginUrlOf(Realm realm) {
        assertCurrentUrlStartsWithLoginUrlOf(realm.getDriver(), realm);
    }
    
    public static void assertCurrentUrlStartsWithLoginUrlOf(WebDriver driver, Realm realm) {
        assertCurrentUrlStartsWith(driver, realm.getOIDCLoginUrl().toString());
    }
    
}
