package org.keycloak.testsuite.adapter.page;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;

import javax.ws.rs.core.UriBuilder;
import java.net.URL;

/**
 *
 * @author tkyjovsk
 */
public class BasicAuthExample extends AbstractPageWithInjectedUrl {

    public static final String DEPLOYMENT_NAME = "basic-auth-example";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getInjectedUrl() {
        //EAP6 URL fix
        URL fixedUrl = createInjectedURL("basicauth");
        return fixedUrl != null ? fixedUrl : url;
    }

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder()
                .userInfo("{user}:{password}")
                .path("service/echo")
                .queryParam("value", "{value}");
    }

    public BasicAuthExample setTemplateValues(String user, String password, String value) {
        setUriParameter("user", user);
        setUriParameter("password", password);
        setUriParameter("value", value);
        return this;
    }

}
