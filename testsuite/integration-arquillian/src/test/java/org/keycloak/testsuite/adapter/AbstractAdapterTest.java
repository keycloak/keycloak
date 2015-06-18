package org.keycloak.testsuite.adapter;

import javax.ws.rs.core.UriBuilder;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.testsuite.AbstractKeycloakTest;
import static org.keycloak.testsuite.AbstractKeycloakTest.AUTH_SERVER_URL;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractAdapterTest extends AbstractKeycloakTest {

    protected String APP_SERVER_BASE_URL;

    protected String LOGIN_URL;
    
    public AbstractAdapterTest(String appServerBaseURL) {
        this.APP_SERVER_BASE_URL = appServerBaseURL;
        this.LOGIN_URL = OIDCLoginProtocolService.authUrl(
            UriBuilder.fromUri(AUTH_SERVER_URL)).build("demo").toString();
    }
    
}
