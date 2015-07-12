package org.keycloak.testsuite.page.adapter;

import java.net.MalformedURLException;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import java.net.URL;
import org.keycloak.testsuite.arquillian.URLProvider;

/**
 *
 * @author tkyjovsk
 */
public class AppServerContextRoot extends AbstractPageWithInjectedUrl {

    private URL appServerContextRoot;

    public AppServerContextRoot() {
        try {
            // get directly instead of injection
            appServerContextRoot = new URL(URLProvider.getAppServerContextRoot());
        } catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public URL getInjectedUrl() {
        return appServerContextRoot;
    }

}
