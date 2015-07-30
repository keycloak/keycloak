package org.keycloak.testsuite.page.auth;

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
