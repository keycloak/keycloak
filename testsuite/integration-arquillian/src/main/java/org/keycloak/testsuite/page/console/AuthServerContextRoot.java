package org.keycloak.testsuite.page.console;

import java.net.MalformedURLException;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import java.net.URL;
import org.keycloak.testsuite.arquillian.URLProvider;

/**
 *
 * @author tkyjovsk
 */
public class AuthServerContextRoot extends AbstractPageWithInjectedUrl {

    private URL authServerContextRoot;

    public AuthServerContextRoot() {
        try {
            // get directly instead of injection
            authServerContextRoot = new URL(URLProvider.getAuthServerContextRoot());
        } catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public URL getInjectedUrl() {
        return authServerContextRoot;
    }

}
