package org.keycloak.testsuite.adapter.page;

import java.net.MalformedURLException;
import java.net.URL;
import javax.ws.rs.core.UriBuilder;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;

/**
 *
 * @author tkyjovsk
 */
public class MultiTenant extends AbstractPageWithInjectedUrl {

    public static final String DEPLOYMENT_NAME = "multi-tenant";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getInjectedUrl() {
        return url;
    }

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder().path("/").queryParam("realm", "{tenantRealm}");
    }

    public URL getTenantRealmUrl(String realm) {
        try {
            return getUriBuilder().build(realm).toURL();
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("Page URL is malformed.");
        }
    }

    public void navigateToRealm(String realm) {
        URL u = getTenantRealmUrl(realm);
        log.info("navigate to "+u.toExternalForm());
        driver.navigate().to(u.toExternalForm());
    }

}
