package org.keycloak.testsuite.page.adapter;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.UriBuilder;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.AbstractPageWithProvidedUrl;

/**
 *
 * @author tkyjovsk
 */
public class BasicAuthExample extends AbstractPageWithProvidedUrl {

    public static final String DEPLOYMENT_NAME = "basic-auth-example";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getProvidedUrl() {
        return url;
    }

    @Override
    public UriBuilder createUriBuilder() {
        return super.createUriBuilder()
                .userInfo("{user}:{password}")
                .path("service/echo")
                .queryParam("value", "{value}");
    }

    public String getUrlString(String user, String password, String value) {
        Map<String, Object> templateValues = new HashMap<>();
        templateValues.put("user", user);
        templateValues.put("password", password);
        templateValues.put("value", value);
        return getUrlString(templateValues);
    }

}
