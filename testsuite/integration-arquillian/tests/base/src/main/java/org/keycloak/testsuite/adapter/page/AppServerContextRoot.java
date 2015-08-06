package org.keycloak.testsuite.adapter.page;

import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import java.net.URL;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.arquillian.annotation.AppServerContext;

/**
 *
 * @author tkyjovsk
 */
public class AppServerContextRoot extends AbstractPageWithInjectedUrl {

    @ArquillianResource
    @AppServerContext
    private URL appServerContextRoot;

    @Override
    public URL getInjectedUrl() {
        return appServerContextRoot;
    }

}
