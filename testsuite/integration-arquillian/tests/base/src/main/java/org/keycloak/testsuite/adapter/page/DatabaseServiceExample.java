package org.keycloak.testsuite.adapter.page;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;

import java.net.URL;

/**
 *
 * @author tkyjovsk
 */
public class DatabaseServiceExample extends AbstractPageWithInjectedUrl {

    public static final String DEPLOYMENT_NAME = "database-service-example";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getInjectedUrl() {
        //EAP6 URL fix
        URL fixedUrl = createInjectedURL("database");
        return fixedUrl != null ? fixedUrl : url;
    }

}
