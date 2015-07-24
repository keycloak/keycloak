package org.keycloak.testsuite.adapter.page;

import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import java.net.URL;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;

/**
 *
 * @author tkyjovsk
 */
public class AppServerContextRoot extends AbstractPageWithInjectedUrl {

    @ArquillianResource
    @AppServerContainer // our custom URLProvider injects correct app server context root depending on test class
    private URL appServerContextRoot;

    @Override
    public URL getInjectedUrl() {
        return appServerContextRoot;
    }

}
