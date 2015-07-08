package org.keycloak.testsuite.page.adapter;

import java.net.MalformedURLException;
import org.keycloak.testsuite.page.AbstractPageWithProvidedUrl;
import java.net.URL;
import org.keycloak.testsuite.arquillian.URLProvider;

/**
 *
 * @author tkyjovsk
 */
public class AppServerContextRoot extends AbstractPageWithProvidedUrl {

    private URL appServerContextRoot;

    public AppServerContextRoot() {
        try {
            appServerContextRoot = new URL(URLProvider.getAppServerContextRoot());
        } catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public URL getProvidedUrl() {
        return appServerContextRoot;
    }

}
