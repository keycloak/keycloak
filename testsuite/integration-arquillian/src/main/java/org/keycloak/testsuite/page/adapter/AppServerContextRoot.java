package org.keycloak.testsuite.page.adapter;

import org.keycloak.testsuite.page.AbstractPageWithProvidedUrl;
import java.net.URL;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.arquillian.annotation.AppServerContext;

/**
 *
 * @author tkyjovsk
 */
public class AppServerContextRoot extends AbstractPageWithProvidedUrl {

    @ArquillianResource
    @AppServerContext
    private URL appServerContextRoot;

    @Override
    public URL getProvidedUrl() {
        return appServerContextRoot;
    }

}
