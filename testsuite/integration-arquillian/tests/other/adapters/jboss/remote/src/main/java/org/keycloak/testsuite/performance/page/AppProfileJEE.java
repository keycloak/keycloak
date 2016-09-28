package org.keycloak.testsuite.performance.page;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;

import java.net.URL;

/**
 *
 * @author tkyjovsk
 */
public class AppProfileJEE extends AbstractPageWithInjectedUrl {
    
    public static final String DEPLOYMENT_NAME = "app-profile-jee";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getInjectedUrl() {
        //EAP6 URL fix
        URL fixedUrl = createInjectedURL(DEPLOYMENT_NAME);
        return fixedUrl != null ? fixedUrl : url;
    }
    
    
}
