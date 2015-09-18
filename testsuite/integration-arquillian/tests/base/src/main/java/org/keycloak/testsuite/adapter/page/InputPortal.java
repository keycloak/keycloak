package org.keycloak.testsuite.adapter.page;

import java.net.URL;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.keycloak.testsuite.page.AbstractPageWithInjectedUrl;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 *
 * @author tkyjovsk
 */
public class InputPortal extends AbstractPageWithInjectedUrl {

    public static final String DEPLOYMENT_NAME = "input-portal";

    @ArquillianResource
    @OperateOnDeployment(DEPLOYMENT_NAME)
    private URL url;

    @Override
    public URL getInjectedUrl() {
        return url;
    }

    @FindBy(id = "parameter")
    private WebElement parameter;

    @FindBy(name = "submit")
    private WebElement submit;

    public void execute(String param) {
        parameter.clear();
        parameter.sendKeys(param);
        submit.click();
    }

}
