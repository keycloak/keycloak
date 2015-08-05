package org.keycloak.testsuite.util;

import org.keycloak.testsuite.auth.page.login.PageWithLoginUrl;
import static org.keycloak.testsuite.util.PageAssert.assertCurrentUrlStartsWith;
import org.openqa.selenium.WebDriver;

/**
 *
 * @author tkyjovsk
 */
public class LoginAssert {

    public static void assertCurrentUrlStartsWithLoginUrlOf(PageWithLoginUrl page) {
        assertCurrentUrlStartsWithLoginUrlOf(page.getDriver(), page);
    }
    
    public static void assertCurrentUrlStartsWithLoginUrlOf(WebDriver driver, PageWithLoginUrl page) {
        assertCurrentUrlStartsWith(driver, page.getOIDCLoginUrl().toString());
    }
    
}
