package org.keycloak.testsuite.console.page;

import java.net.URL;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContext;

/**
 *
 * @author tkyjovsk
 */
public class AuthServerContextRoot extends AbstractPageWithProvidedUrl {

    @ArquillianResource
    @AuthServerContext
    private URL authServerContextRoot;

    @Override
    public URL getProvidedUrl() {
        return authServerContextRoot;
    }

}
