package org.keycloak.testsuite.auth.page;

import java.net.URL;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;

/**
 *
 * @author tkyjovsk
 */
public class AuthServerContextRoot extends AbstractPageWithInjectedUrl {

    @ArquillianResource
    private URL authServerContextRoot;

    @Override
    public URL getInjectedUrl() {
        return authServerContextRoot;
    }

}
