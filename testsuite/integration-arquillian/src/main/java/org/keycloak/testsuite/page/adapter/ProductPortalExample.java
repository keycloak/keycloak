package org.keycloak.testsuite.page.adapter;

import java.net.URL;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.AbstractPageWithProvidedUrl;

/**
 *
 * @author tkyjovsk
 */
public class ProductPortalExample extends AbstractPageWithProvidedUrl {

    public static final String DEPLOYMENT_NAME = "product-portal-example";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getProvidedUrl() {
        return url;
    }

}
