package org.keycloak.testsuite.adapter.page;

import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.net.URL;

/**
 * @author mhajas
 */
public class SAMLPostEncExample extends AbstractPageWithInjectedUrl {
    public static final String DEPLOYMENT_NAME = "saml-post-encryption";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @FindBy(tagName = "a")
    WebElement logoutButton;

    @Override
    public URL getInjectedUrl() {
        //EAP6 URL fix
        URL fixedUrl = createInjectedURL("sales-post-enc");
        return fixedUrl != null ? fixedUrl : url;
    }

    public void logout() {
        logoutButton.click();
    }
}
