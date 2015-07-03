package org.keycloak.testsuite.page.adapter;

import java.net.MalformedURLException;
import java.net.URL;
import javax.ws.rs.core.UriBuilder;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.AbstractPageWithProvidedUrl;

/**
 *
 * @author tkyjovsk
 */
public class MultiTenant extends AbstractPageWithProvidedUrl {

    public static final String DEPLOYMENT_NAME = "multi-tenant";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getProvidedUrl() {
        return url;
    }

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder().path("?realm={tenantRealm}");
    }

    public URL getTenantRealmUrl(String realm) {
        try {
            return getUriBuilder().build(realm).toURL();
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("Page URL is malformed.");
        }
    }

    public void navigateToRealm(String realm) {
        driver.navigate().to(getTenantRealmUrl(realm));
    }

}
