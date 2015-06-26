package org.keycloak.testsuite.adapter.page;

import java.net.URL;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.console.page.AbstractPageWithProvidedUrl;

/**
 *
 * @author tkyjovsk
 */
public class CustomerDbErrorPage extends AbstractPageWithProvidedUrl {

    public static final String DEPLOYMENT_NAME = "customer-db-error-page";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getProvidedUrl() {
        return url;
    }

}
