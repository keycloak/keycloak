package org.keycloak.testsuite.page.adapter.fuse;

import java.net.MalformedURLException;
import java.net.URL;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.arquillian.annotation.AppServerContainer;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;

/**
 *
 * @author tkyjovsk
 */
public class CustomerPortalFuseExample extends AbstractPageWithInjectedUrl {

    public static final String DEPLOYMENT_NAME = "customer-portal-fuse-example";
    public static final String DEPLOYMENT_CONTEXT = "customer-portal";

    @ArquillianResource
//    @OperateOnDeployment(DEPLOYMENT_NAME)
    @AppServerContainer
    private URL appServerContextRoot;

    private URL url;

    @Override
    public URL getInjectedUrl() {
        if (url == null) {
            try {
                url = new URL(appServerContextRoot.toExternalForm() + "/" + DEPLOYMENT_CONTEXT);
            } catch (MalformedURLException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return url;
    }

}
