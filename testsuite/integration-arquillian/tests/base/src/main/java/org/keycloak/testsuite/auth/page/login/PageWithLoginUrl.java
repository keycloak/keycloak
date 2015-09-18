package org.keycloak.testsuite.auth.page.login;

import java.net.URI;
import org.openqa.selenium.WebDriver;

/**
 * Used by util class LoginAssert. Implementing classes: AuthRealm, AdminConsole.
 * @author tkyjovsk
 */
public interface PageWithLoginUrl {

    WebDriver getDriver();
    URI getOIDCLoginUrl();
    
}
