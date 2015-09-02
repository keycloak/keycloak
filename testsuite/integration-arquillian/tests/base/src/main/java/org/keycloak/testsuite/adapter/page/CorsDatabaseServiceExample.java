package org.keycloak.testsuite.adapter.page;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;

import java.net.URL;

/**
 *
 * @author tkyjovsk
 */
public class CorsDatabaseServiceExample extends AbstractPageWithInjectedUrl {

    public static final String DEPLOYMENT_NAME = "cors-database-service";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getInjectedUrl() {
        return url;
    }

}