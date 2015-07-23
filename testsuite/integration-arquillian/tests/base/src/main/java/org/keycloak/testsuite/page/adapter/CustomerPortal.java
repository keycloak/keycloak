package org.keycloak.testsuite.page.adapter;

import java.net.URL;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;

/**
 *
 * @author tkyjovsk
 */
public class CustomerPortal extends AbstractPageWithInjectedUrl {

    public static final String DEPLOYMENT_NAME = "customer-portal";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getInjectedUrl() {
        return url;
    }

}
