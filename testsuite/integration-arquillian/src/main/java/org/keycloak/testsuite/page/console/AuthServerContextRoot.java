package org.keycloak.testsuite.page.console;

import org.keycloak.testsuite.page.AbstractPageWithProvidedUrl;
import java.net.URL;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainer;

/**
 *
 * @author tkyjovsk
 */
public class AuthServerContextRoot extends AbstractPageWithProvidedUrl {

    @ArquillianResource
    @AuthServerContainer
    private URL authServerContextRoot;

    @Override
    public URL getProvidedUrl() {
        return authServerContextRoot;
    }

}
