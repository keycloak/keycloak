package org.keycloak.testsuite.auth.page;

import java.net.URL;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContext;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;

/**
 * Context root of the tested Keycloak server.
 * 
 * URL: http://localhost:${auth.server.http.port}
 * 
 * @author tkyjovsk
 */
public class AuthServerContextRoot extends AbstractPageWithInjectedUrl {

    @ArquillianResource
    @AuthServerContext
    private URL authServerContextRoot;

    @Override
    public URL getInjectedUrl() {
        return authServerContextRoot;
    }

}
