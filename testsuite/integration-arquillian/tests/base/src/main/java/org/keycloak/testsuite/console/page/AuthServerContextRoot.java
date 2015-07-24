package org.keycloak.testsuite.console.page;

import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import java.net.URL;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainer;

/**
 *
 * @author tkyjovsk
 */
public class AuthServerContextRoot extends AbstractPageWithInjectedUrl {

    @ArquillianResource
    @AuthServerContainer // our custom URLProvider injects auth server context root depending on test class
    private URL authServerContextRoot;

    @Override
    public URL getInjectedUrl() {
        return authServerContextRoot;
    }

}
