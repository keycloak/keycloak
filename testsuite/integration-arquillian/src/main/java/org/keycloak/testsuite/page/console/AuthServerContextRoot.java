package org.keycloak.testsuite.page.console;

import java.net.MalformedURLException;
import org.keycloak.testsuite.page.AbstractPageWithProvidedUrl;
import java.net.URL;
import org.keycloak.testsuite.arquillian.URLProvider;

/**
 *
 * @author tkyjovsk
 */
public class AuthServerContextRoot extends AbstractPageWithProvidedUrl {

    private URL authServerContextRoot;

    public AuthServerContextRoot() {
        try {
            authServerContextRoot = new URL(URLProvider.getAuthServerContextRoot());
        } catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public URL getProvidedUrl() {
        return authServerContextRoot;
    }

}
